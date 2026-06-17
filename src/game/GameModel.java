package game;

import assets.Cue;
import assets.SoundEngine;
import assets.SoundEngine.SirenLevel;
import characters.Direction;
import characters.Ghost;
import characters.GhostMode;
import characters.GhostPersonality;
import characters.Pacman;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The entire mutable world for one game. {@link #update(double)} is the only
 * place game state changes, and it runs exclusively on the EDT (driven by
 * {@link GameLoop}). No threads, no locks — the only other activity is the
 * fire-and-forget {@link SoundEngine}, which shares nothing.
 */
public final class GameModel {
    // Movement / loop constants (cells == tiles; speeds are cells/sec).
    static final double BASE_PAC = 7.5, BASE_PAC_FRIGHT = 7.9;
    static final double BASE_GHOST = 7.0, BASE_GHOST_FRIGHT = 5.0, BASE_TUNNEL = 3.5, BASE_EATEN = 14.0;
    static final int START_LIVES = 3, MAX_LIVES = 5, EXTRA_LIFE_SCORE = 10000;
    static final int DOT = 10, POWER = 50;
    static final int[] GHOST_CHAIN = {200, 400, 800, 1600};
    static final double READY_TIME = 1.6, DEATH_TIME = 1.5, CLEAR_TIME = 2.0;
    static final double EAT_PAUSE = 0.5, REVIVE_DELAY = 1.0;
    static final double FLASH_PER = 0.13;

    private final BoardSizeOption size;
    private final SoundEngine sound;
    private final Random rng = new Random();

    private Board board;
    private int boardVersion;
    private final Pacman pac = new Pacman();
    private final List<Ghost> ghosts = new ArrayList<>();
    private final List<Ghost> ghostsView = Collections.unmodifiableList(ghosts);
    private Ghost blinky;

    private int level = 1;
    private LevelConfig cfg = LevelConfig.forLevel(1);
    private GamePhase phase = GamePhase.READY;
    private GamePhase pausedFrom = GamePhase.PLAYING;

    private int score;
    private int highScore;
    private int lives = START_LIVES;
    private boolean extraLifeAwarded;

    private double timeMs;
    private double phaseTimer = READY_TIME;
    private double eatPauseTimer;

    // global scatter/chase timeline
    private boolean[] modeChase = new boolean[0];
    private double[] modeSecs = new double[0];
    private int modeIndex;
    private double modeTimer;
    private boolean globalChase;

    // frightened window
    private double frightTimer;
    private int eatChain;

    // ghost release
    private double globalDotTimer;

    // fruit
    private boolean fruitActive;
    private double fruitTimer;
    private int fruitsSpawned;
    private int fruitT1, fruitT2;
    private int dotsEaten;

    private final List<ScorePopup> popups = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private boolean chompToggle;

    public GameModel(BoardSizeOption size, SoundEngine sound, int highScore) {
        this.size = size;
        this.sound = sound;
        this.highScore = highScore;
        startLevel(true);
    }

    // ---------------------------------------------------------------- setup

    private void startLevel(boolean newGame) {
        board = new Board(size);
        boardVersion++;
        cfg = LevelConfig.forLevel(level);
        buildModeTimeline();
        frightTimer = 0;
        eatChain = 0;
        fruitActive = false;
        fruitsSpawned = 0;
        dotsEaten = 0;
        fruitT1 = (int) Math.round(board.totalDots() * 0.30);
        fruitT2 = (int) Math.round(board.totalDots() * 0.73);
        globalDotTimer = 0;
        eatPauseTimer = 0;
        popups.clear();
        particles.clear();
        spawnActors();
        phase = GamePhase.READY;
        phaseTimer = READY_TIME;
        sound.stopSiren();
        sound.play(Cue.INTRO);
    }

    private void spawnActors() {
        Point ps = board.pacSpawn();
        pac.place(ps.x, ps.y, Direction.LEFT);
        pac.clearDesired();
        pac.resetMouth();

        ghosts.clear();
        GhostPersonality[] order = size.ghostCount() == 3
                ? new GhostPersonality[] {GhostPersonality.BLINKY, GhostPersonality.PINKY, GhostPersonality.CLYDE}
                : new GhostPersonality[] {GhostPersonality.BLINKY, GhostPersonality.PINKY,
                        GhostPersonality.INKY, GhostPersonality.CLYDE};
        double[] delays = {0.0, 1.5, 4.0, 6.5};
        for (int i = 0; i < order.length; i++) {
            Point sp = board.ghostSpawn(i);
            Ghost g = new Ghost(order[i], sp.x, sp.y, delays[i], (i + 1) * 2654435761L + 12345L);
            ghosts.add(g);
            if (order[i] == GhostPersonality.BLINKY) blinky = g;
            if (delays[i] <= 0) {
                g.setMode(GhostMode.SCATTER);
                g.setDir(chooseGhostDir(g)); // validated open direction, not blind UP
            }
        }
        globalChase = false;
        modeIndex = 0;
        modeTimer = modeSecs.length > 0 ? modeSecs[0] : Double.MAX_VALUE;
    }

    private void buildModeTimeline() {
        if (level == 1) {
            modeChase = new boolean[] {false, true, false, true, false, true, false};
            modeSecs = new double[] {7, 20, 7, 20, 5, 20, 5};
        } else if (level <= 4) {
            modeChase = new boolean[] {false, true, false, true, false, true};
            modeSecs = new double[] {7, 20, 7, 20, 5, 1033};
        } else {
            modeChase = new boolean[] {false, true, false, true, false, true};
            modeSecs = new double[] {5, 20, 5, 20, 5, 1037};
        }
    }

    // --------------------------------------------------------------- update

    public void update(double dt) {
        if (phase == GamePhase.PAUSED) return;
        timeMs += dt * 1000.0;
        switch (phase) {
            case READY:
                phaseTimer -= dt;
                if (phaseTimer <= 0) phase = GamePhase.PLAYING;
                break;
            case PLAYING:
                updatePlaying(dt);
                break;
            case DYING:
                phaseTimer -= dt;
                updateEffects(dt);
                if (phaseTimer <= 0) afterDeath();
                break;
            case LEVEL_CLEAR:
                phaseTimer -= dt;
                if (phaseTimer <= 0) { level++; startLevel(false); }
                break;
            case GAME_OVER:
                updateEffects(dt);
                break;
            default:
                break;
        }
    }

    private void updatePlaying(double dt) {
        if (eatPauseTimer > 0) {
            eatPauseTimer -= dt;
            updateEffects(dt);
            return;
        }

        // mode timeline (paused while frightened)
        if (frightTimer <= 0 && modeIndex < modeSecs.length) {
            modeTimer -= dt;
            if (modeTimer <= 0) advanceMode();
        }

        // frightened window
        if (frightTimer > 0) {
            frightTimer -= dt;
            if (frightTimer <= 0) endFright();
        }

        // ghost release
        globalDotTimer += dt;
        double releaseTimeout = level < 5 ? 4.0 : 3.0;
        if (globalDotTimer >= releaseTimeout) {
            releaseNextGhost();
            globalDotTimer = 0;
        }
        for (Ghost g : ghosts) {
            if (g.mode() == GhostMode.IN_HOUSE) {
                g.tickTimer(dt);
                if (g.timer() <= 0) releaseGhost(g);
            } else if (g.mode() == GhostMode.EATEN) {
                g.tickTimer(dt); // watchdog: BFS should arrive long before this
                if (g.timer() <= 0) g.resetToHouse(REVIVE_DELAY);
            }
        }

        stepPacman(dt);
        for (Ghost g : ghosts) stepGhost(g, dt);
        handleCollisions();

        if (fruitActive) {
            fruitTimer -= dt;
            if (fruitTimer <= 0) fruitActive = false;
        }

        updateEffects(dt);
        updateSiren();

        if (board.isCleared()) {
            phase = GamePhase.LEVEL_CLEAR;
            phaseTimer = CLEAR_TIME;
            sound.stopSiren();
        }
    }

    private void updateEffects(double dt) {
        for (ScorePopup p : popups) p.age += dt;
        popups.removeIf(ScorePopup::expired);
        for (Particle p : particles) {
            p.age += dt;
            p.col += p.vc * dt;
            p.row += p.vr * dt;
        }
        particles.removeIf(Particle::expired);
    }

    // ------------------------------------------------------------- Pac-Man

    private void stepPacman(double dt) {
        if (pac.desiredDir() != Direction.NONE && pac.dir() != Direction.NONE
                && pac.desiredDir() == pac.dir().opposite()) {
            pac.reverse();
        }
        double speed = (frightTimer > 0 ? BASE_PAC_FRIGHT * cfg.frightPacMul()
                : BASE_PAC * cfg.pacSpeedMul());
        if (pac.dir() != Direction.NONE) pac.advanceMouth(speed * dt * 2.2);
        pac.advance(speed * dt, e -> pacAtCenter());
    }

    private void pacAtCenter() {
        pac.wrapColumn(wrapCol(pac.col(), pac.row()));

        Board.EatResult er = board.eat(pac.col(), pac.row());
        if (er == Board.EatResult.DOT) {
            score += DOT;
            onDotEaten();
            sound.play(chompToggle ? Cue.CHOMP_A : Cue.CHOMP_B);
            chompToggle = !chompToggle;
        } else if (er == Board.EatResult.POWER) {
            score += POWER;
            onDotEaten();
            triggerFright();
            sound.play(Cue.POWER);
        }

        if (fruitActive) {
            Point fs = board.fruitSpot();
            if (pac.col() == fs.x && pac.row() == fs.y) eatFruit();
        }
        checkExtraLife();

        // decide direction
        Direction d = pac.dir();
        Direction want = pac.desiredDir();
        if (want != Direction.NONE && !blockedPac(want)) {
            d = want;
        }
        if (d == Direction.NONE || blockedPac(d)) d = Direction.NONE;
        pac.setDir(d);
    }

    private void onDotEaten() {
        dotsEaten++;
        globalDotTimer = 0;
        if (fruitsSpawned == 0 && dotsEaten >= fruitT1) spawnFruit();
        else if (fruitsSpawned == 1 && dotsEaten >= fruitT2) spawnFruit();
    }

    private boolean blockedPac(Direction d) {
        int nc = wrapCol(pac.col() + d.dx, pac.row() + d.dy);
        int nr = pac.row() + d.dy;
        return board.blocksPacman(nc, nr);
    }

    // -------------------------------------------------------------- ghosts

    private void stepGhost(Ghost g, double dt) {
        if (g.mode() == GhostMode.IN_HOUSE) return;
        g.advance(ghostSpeed(g) * dt, e -> ghostAtCenter(g));
    }

    private void ghostAtCenter(Ghost g) {
        g.wrapColumn(wrapCol(g.col(), g.row()));

        if (g.mode() == GhostMode.EATEN) {
            Point home = board.home();
            if (g.col() == home.x && g.row() == home.y) {
                g.resetToHouse(REVIVE_DELAY);
                g.setDir(Direction.NONE);
                return;
            }
        }
        g.setDir(chooseGhostDir(g));
    }

    private Direction chooseGhostDir(Ghost g) {
        boolean pass = g.mode().leavingOrEaten();
        Direction reverse = g.dir().opposite();
        Direction[] order = {Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT};

        // Eaten "eyes" follow the BFS distance field straight home (reverse allowed),
        // which cannot get trapped in cycles the way the greedy pather can.
        if (g.mode() == GhostMode.EATEN) {
            int[][] dist = board.homeDistance();
            Direction best = Direction.NONE;
            int bestD = Integer.MAX_VALUE;
            for (Direction d : order) {
                if (ghostBlocked(g, d, true)) continue;
                int nc = wrapCol(g.col() + d.dx, g.row() + d.dy);
                int nr = g.row() + d.dy;
                if (!board.inBounds(nc, nr)) continue;
                int dd = dist[nr][nc];
                if (dd >= 0 && dd < bestD) { bestD = dd; best = d; }
            }
            return best != Direction.NONE ? best : reverse;
        }

        List<Direction> legal = new ArrayList<>(4);
        for (Direction d : order) {
            if (d == reverse) continue;
            if (!ghostBlocked(g, d, pass)) legal.add(d);
        }
        if (legal.isEmpty()) return reverse; // dead-end: allow reverse

        if (g.mode() == GhostMode.FRIGHTENED) {
            return legal.get(g.nextRandom(legal.size()));
        }

        Point target = ghostTarget(g);
        Direction best = legal.get(0);
        long bestDist = Long.MAX_VALUE;
        for (Direction d : legal) {
            int nc = wrapCol(g.col() + d.dx, g.row() + d.dy);
            int nr = g.row() + d.dy;
            long dc = nc - target.x, dr = nr - target.y;
            long dist = dc * dc + dr * dr;
            if (dist < bestDist) { bestDist = dist; best = d; }
        }
        return best;
    }

    private boolean ghostBlocked(Ghost g, Direction d, boolean pass) {
        int nc = wrapCol(g.col() + d.dx, g.row() + d.dy);
        int nr = g.row() + d.dy;
        return board.blocksGhost(nc, nr, pass);
    }

    private Point ghostTarget(Ghost g) {
        switch (g.mode()) {
            case EATEN:
                return board.home();
            case SCATTER:
                if (g.personality() == GhostPersonality.BLINKY && elroyActive()) return pacTile();
                return scatterCorner(g.personality());
            case CHASE:
            default:
                return chaseTarget(g.personality());
        }
    }

    private Point chaseTarget(GhostPersonality who) {
        Point p = pacTile();
        Direction pd = pac.dir() == Direction.NONE ? Direction.RIGHT : pac.dir();
        switch (who) {
            case BLINKY:
                return p;
            case PINKY: {
                int c = p.x + pd.dx * 4, r = p.y + pd.dy * 4;
                if (pd == Direction.UP) c -= 4; // faithful overflow quirk
                return new Point(c, r);
            }
            case INKY: {
                int qc = p.x + pd.dx * 2, qr = p.y + pd.dy * 2;
                if (pd == Direction.UP) qc -= 2;
                Point b = new Point(blinky.col(), blinky.row());
                return new Point(2 * qc - b.x, 2 * qr - b.y);
            }
            case CLYDE:
            default: {
                long dc = g(who).col() - p.x, dr = g(who).row() - p.y;
                if (dc * dc + dr * dr > 64) return p;
                return scatterCorner(GhostPersonality.CLYDE);
            }
        }
    }

    private Ghost g(GhostPersonality who) {
        for (Ghost gh : ghosts) if (gh.personality() == who) return gh;
        return blinky;
    }

    private Point scatterCorner(GhostPersonality who) {
        switch (who) {
            case BLINKY: return board.cornerTopRight();
            case PINKY: return board.cornerTopLeft();
            case INKY: return board.cornerBottomRight();
            case CLYDE:
            default: return board.cornerBottomLeft();
        }
    }

    private double ghostSpeed(Ghost g) {
        if (g.mode() == GhostMode.EATEN) return BASE_EATEN;
        if (board.isTunnel(g.col(), g.row())) return BASE_TUNNEL * cfg.ghostTunnelMul();
        if (g.mode() == GhostMode.FRIGHTENED) return BASE_GHOST_FRIGHT * cfg.frightGhostMul();
        double s = BASE_GHOST * cfg.ghostSpeedMul();
        if (g.personality() == GhostPersonality.BLINKY && elroyActive()) {
            s *= (board.dotsRemaining() <= cfg.elroy2()) ? 1.10 : 1.05;
        }
        return s;
    }

    private boolean elroyActive() {
        if (anyGhostInHouse()) return false;
        int e1 = Math.min(cfg.elroy1(), (int) Math.round(board.totalDots() * 0.5));
        return board.dotsRemaining() <= e1;
    }

    private boolean anyGhostInHouse() {
        for (Ghost g : ghosts) if (g.mode() == GhostMode.IN_HOUSE) return true;
        return false;
    }

    private void advanceMode() {
        modeIndex++;
        globalChase = modeIndex >= modeChase.length || modeChase[modeIndex];
        modeTimer = modeIndex < modeSecs.length ? modeSecs[modeIndex] : Double.MAX_VALUE;
        GhostMode target = globalChase ? GhostMode.CHASE : GhostMode.SCATTER;
        for (Ghost g : ghosts) {
            if (g.mode() == GhostMode.SCATTER || g.mode() == GhostMode.CHASE) {
                g.setMode(target);
                g.reverse();
            }
        }
    }

    private void releaseNextGhost() {
        for (Ghost g : ghosts) {
            if (g.mode() == GhostMode.IN_HOUSE) { releaseGhost(g); return; }
        }
    }

    private void releaseGhost(Ghost g) {
        g.setMode(globalChase ? GhostMode.CHASE : GhostMode.SCATTER);
        g.place(g.spawnCol(), g.spawnRow(), Direction.UP);
        g.setDir(chooseGhostDir(g)); // never walk blindly into the wall above the pen
    }

    // ---------------------------------------------------------- frightened

    private void triggerFright() {
        for (Ghost g : ghosts) {
            if (g.mode() == GhostMode.SCATTER || g.mode() == GhostMode.CHASE) {
                if (cfg.frightSeconds() > 0) g.setMode(GhostMode.FRIGHTENED);
                g.reverse();
            }
        }
        if (cfg.frightSeconds() <= 0) return;
        frightTimer = cfg.frightSeconds();
        eatChain = 0;
        sound.startSiren(SirenLevel.FRIGHT);
    }

    private void endFright() {
        frightTimer = 0;
        GhostMode target = globalChase ? GhostMode.CHASE : GhostMode.SCATTER;
        for (Ghost g : ghosts) {
            if (g.mode() == GhostMode.FRIGHTENED) g.setMode(target);
        }
    }

    // --------------------------------------------------------- collisions

    private void handleCollisions() {
        for (Ghost g : ghosts) {
            if (g.mode() == GhostMode.IN_HOUSE || g.mode() == GhostMode.EATEN) continue;
            double dc = pac.fcol() - g.fcol();
            double dr = pac.frow() - g.frow();
            if (dc * dc + dr * dr < 0.25) {
                if (g.mode() == GhostMode.FRIGHTENED) {
                    eatGhost(g);
                } else {
                    killPacman();
                    return;
                }
            }
        }
    }

    private void eatGhost(Ghost g) {
        int pts = GHOST_CHAIN[Math.min(eatChain, GHOST_CHAIN.length - 1)];
        eatChain++;
        score += pts;
        popups.add(new ScorePopup(g.fcol(), g.frow(), Integer.toString(pts), assets.Palette.SCORE_POP, 1.0));
        g.setMode(GhostMode.EATEN);
        g.setDir(g.dir() == Direction.NONE ? Direction.UP : g.dir());
        g.setTimer(8.0); // watchdog ceiling for the trip home
        eatPauseTimer = EAT_PAUSE;
        checkExtraLife();
        sound.play(Cue.EAT_GHOST);
    }

    private void killPacman() {
        phase = GamePhase.DYING;
        phaseTimer = DEATH_TIME;
        lives--;
        frightTimer = 0;
        sound.stopSiren();
        sound.play(Cue.DEATH);
    }

    private void afterDeath() {
        spawnDeathBurst();
        if (lives <= 0) {
            if (score > highScore) highScore = score;
            phase = GamePhase.GAME_OVER;
        } else {
            spawnActors();
            frightTimer = 0;
            eatChain = 0;
            phase = GamePhase.READY;
            phaseTimer = READY_TIME;
        }
    }

    private void spawnDeathBurst() {
        for (int i = 0; i < 10; i++) {
            double ang = Math.PI * 2 * i / 10.0;
            particles.add(new Particle(pac.fcol(), pac.frow(),
                    Math.cos(ang) * 2.2, Math.sin(ang) * 2.2, assets.Palette.PAC, 0.6));
        }
    }

    // -------------------------------------------------------------- fruit

    private void spawnFruit() {
        fruitsSpawned++;
        fruitActive = true;
        fruitTimer = 9.0 + rng.nextDouble();
    }

    private void eatFruit() {
        score += cfg.fruit().points();
        fruitActive = false;
        Point fs = board.fruitSpot();
        popups.add(new ScorePopup(fs.x + 0.5, fs.y + 0.5,
                Integer.toString(cfg.fruit().points()), assets.Palette.FRUIT_POP, 1.2));
        checkExtraLife();
        sound.play(Cue.FRUIT);
    }

    private void checkExtraLife() {
        if (!extraLifeAwarded && score >= EXTRA_LIFE_SCORE) {
            extraLifeAwarded = true;
            if (lives < MAX_LIVES) {
                lives++;
                sound.play(Cue.EXTRA_LIFE);
            }
        }
        if (score > highScore) highScore = score;
    }

    // -------------------------------------------------------------- siren

    private void updateSiren() {
        if (frightTimer > 0) { sound.startSiren(SirenLevel.FRIGHT); return; }
        double frac = board.totalDots() == 0 ? 0 : (double) board.dotsRemaining() / board.totalDots();
        SirenLevel l = frac > 0.66 ? SirenLevel.S1
                : frac > 0.40 ? SirenLevel.S2
                : frac > 0.15 ? SirenLevel.S3 : SirenLevel.S4;
        sound.startSiren(l);
    }

    // -------------------------------------------------------------- input

    public void setDesiredDir(Direction d) {
        if (phase == GamePhase.PLAYING || phase == GamePhase.READY) pac.setDesiredDir(d);
    }

    public void togglePause() {
        if (phase == GamePhase.PLAYING || phase == GamePhase.READY) {
            pausedFrom = phase;
            phase = GamePhase.PAUSED;
            sound.stopSiren();
        } else if (phase == GamePhase.PAUSED) {
            phase = pausedFrom;
        }
    }

    public boolean isPaused() { return phase == GamePhase.PAUSED; }

    // ------------------------------------------------------------ helpers

    private int wrapCol(int c, int r) {
        if (board.isTunnelRow(r)) {
            if (c < 0) return board.cols() - 1;
            if (c >= board.cols()) return 0;
        }
        return c;
    }

    private Point pacTile() { return new Point(pac.col(), pac.row()); }

    // ------------------------------------------------------------ getters

    public Board board() { return board; }
    public Pacman pac() { return pac; }
    public List<Ghost> ghosts() { return ghostsView; }
    public GamePhase phase() { return phase; }
    public int score() { return score; }
    public int highScore() { return highScore; }
    public int lives() { return lives; }
    public int level() { return level; }
    public double timeMs() { return timeMs; }
    public int boardVersion() { return boardVersion; }
    public double frightTimer() { return frightTimer; }
    public boolean frightActive() { return frightTimer > 0; }
    public LevelConfig cfg() { return cfg; }

    /** True when frightened ghosts should be flashing white (final seconds). */
    public boolean frightFlashing() {
        if (frightTimer <= 0 || cfg.frightFlashes() <= 0) return false;
        double window = cfg.frightFlashes() * 2 * FLASH_PER;
        if (frightTimer > window) return false;
        return ((int) (frightTimer / FLASH_PER)) % 2 == 0;
    }

    public boolean fruitActive() { return fruitActive; }
    public FruitType fruitType() { return cfg.fruit(); }

    /** Death animation progress 0..1 (only meaningful in DYING). */
    public double deathProgress() {
        return phase == GamePhase.DYING ? 1.0 - Math.max(0, phaseTimer) / DEATH_TIME : 0;
    }

    /** READY countdown progress 0..1. */
    public double readyProgress() {
        return phase == GamePhase.READY ? 1.0 - Math.max(0, phaseTimer) / READY_TIME : 1;
    }

    public List<ScorePopup> popups() { return popups; }
    public List<Particle> particles() { return particles; }
}
