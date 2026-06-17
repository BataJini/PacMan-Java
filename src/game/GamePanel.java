package game;

import assets.Palette;
import assets.SpritePainter;
import characters.Direction;
import characters.Ghost;
import characters.GhostMode;
import characters.Pacman;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * The single custom render surface. Owns the coordinate system, the cached neon
 * maze image, the locked draw order, and raw key input. Reads the model; the only
 * state it mutates is the player's desired direction.
 */
public final class GamePanel extends JPanel {
    static final int HUD_PX = 52;
    static final int BASE_TILE = 24;

    private final GameModel model;
    private final GameScreen screen;

    private int tileSize = BASE_TILE;
    private int offX, offY, boardW, boardH;

    private BufferedImage mazeCache;
    private int cacheVersion = -1;
    private int cacheTile = -1;
    private BufferedImage scanlines;

    public GamePanel(GameModel model, GameScreen screen) {
        this.model = model;
        this.screen = screen;
        setOpaque(true);
        setBackground(Palette.BG_DEEP);
        int cols = model.board().cols(), rows = model.board().rows();
        setPreferredSize(new Dimension(cols * BASE_TILE, rows * BASE_TILE + HUD_PX));
        setFocusable(true);
        addKeyListener(new Input());
    }

    public int tileSize() { return tileSize; }
    public int offX() { return offX; }
    public int offY() { return offY; }
    public double px(double fcol) { return offX + fcol * tileSize; }
    public double py(double frow) { return offY + frow * tileSize; }

    // ----------------------------------------------------------- rendering

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        computeMetrics();
        drawBackground(g2);
        ensureMazeCache();
        if (mazeCache != null) g2.drawImage(mazeCache, offX, offY, null);

        drawPellets(g2);
        drawFruit(g2);
        drawEffects(g2);
        drawPacman(g2);
        drawGhosts(g2);
        drawScanlines(g2);
        Hud.draw(g2, model, this);
        Overlays.draw(g2, model, screen, this);
        g2.dispose();
    }

    private void computeMetrics() {
        int cols = model.board().cols(), rows = model.board().rows();
        tileSize = Math.max(8, Math.min(getWidth() / Math.max(1, cols),
                (getHeight() - HUD_PX) / Math.max(1, rows)));
        boardW = tileSize * cols;
        boardH = tileSize * rows;
        offX = (getWidth() - boardW) / 2;
        offY = HUD_PX + (getHeight() - HUD_PX - boardH) / 2;
    }

    private void drawBackground(Graphics2D g2) {
        g2.setColor(Palette.BG_DEEP);
        g2.fillRect(0, 0, getWidth(), getHeight());
        int cx = getWidth() / 2, cy = getHeight() / 2;
        float rad = Math.max(getWidth(), getHeight()) * 0.7f;
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), rad,
                new float[] {0f, 1f}, new Color[] {Palette.BG_VIGNETTE, Palette.BG_DEEP}));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Palette.PLAYFIELD_INK);
        g2.fillRect(offX, offY, boardW, boardH);
    }

    private void ensureMazeCache() {
        if (mazeCache != null && cacheVersion == model.boardVersion() && cacheTile == tileSize) return;
        if (boardW <= 0 || boardH <= 0) return;
        cacheVersion = model.boardVersion();
        cacheTile = tileSize;
        mazeCache = new BufferedImage(boardW, boardH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = mazeCache.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Board board = model.board();
        int cols = board.cols(), rows = board.rows();
        double t = tileSize;
        double inset = t * 0.16;
        double arc = t * 0.42;
        Area walls = new Area();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board.typeAt(c, r) != TileType.WALL) continue;
                double x = c * t, y = r * t;
                double left = x + inset, right = x + t - inset, top = y + inset, bot = y + t - inset;
                if (isWall(board, c - 1, r)) left = x;
                if (isWall(board, c + 1, r)) right = x + t;
                if (isWall(board, c, r - 1)) top = y;
                if (isWall(board, c, r + 1)) bot = y + t;
                walls.add(new Area(new RoundRectangle2D.Double(
                        left, top, right - left, bot - top, arc, arc)));
            }
        }

        int level = model.level();
        Color core = Palette.mazeColor(Palette.WALL_CORE, level);
        Color glow = Palette.mazeColor(Palette.WALL_GLOW, level);
        Color glowSoft = Palette.mazeColor(Palette.WALL_GLOW_SOFT, level);
        Color edge = Palette.mazeColor(Palette.WALL_EDGE, level);

        g2.setColor(new Color(edge.getRed() / 3, edge.getGreen() / 3, edge.getBlue() / 2 + 8, 230));
        g2.fill(walls);

        strokeArea(g2, walls, Palette.glow(glowSoft, 0.12f), (float) (t * 0.5));
        strokeArea(g2, walls, Palette.glow(glow, 0.28f), (float) (t * 0.3));
        strokeArea(g2, walls, Palette.glow(glow, 0.55f), (float) (t * 0.18));
        strokeArea(g2, walls, core, (float) Math.max(1.5, t * 0.1));
        g2.dispose();
    }

    private static void strokeArea(Graphics2D g2, Area area, Color c, float w) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(area);
    }

    private static boolean isWall(Board b, int c, int r) {
        return b.inBounds(c, r) && b.typeAt(c, r) == TileType.WALL;
    }

    private void drawPellets(Graphics2D g2) {
        Board board = model.board();
        double pulse = Math.sin(model.timeMs() / 1000.0 * 5.5);
        boolean blinkOn = ((int) (model.timeMs() / 250)) % 2 == 0;
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.cols(); c++) {
                TileType tt = board.typeAt(c, r);
                double cx = px(c + 0.5), cy = py(r + 0.5);
                if (tt == TileType.DOT) {
                    SpritePainter.pellet(g2, cx, cy, tileSize);
                } else if (tt == TileType.POWER && blinkOn) {
                    SpritePainter.powerPellet(g2, cx, cy, tileSize, (pulse + 1) / 2);
                }
            }
        }
    }

    private void drawFruit(Graphics2D g2) {
        if (!model.fruitActive()) return;
        java.awt.Point fs = model.board().fruitSpot();
        SpritePainter.fruit(g2, px(fs.x + 0.5), py(fs.y + 0.5), tileSize * 0.5, model.fruitType());
    }

    private void drawEffects(Graphics2D g2) {
        for (Particle p : model.particles()) {
            double a = 1 - p.life();
            double rr = tileSize * (0.1 + p.life() * 0.5);
            g2.setColor(Palette.glow(p.color, (float) a));
            g2.fill(new Ellipse2D.Double(px(p.col) - rr, py(p.row) - rr, rr * 2, rr * 2));
        }
        for (ScorePopup pop : model.popups()) {
            double rise = pop.life() * tileSize * 0.7;
            float a = (float) Math.max(0, 1 - Math.max(0, pop.life() - 0.6) / 0.4);
            g2.setFont(assets.Fonts.arcade(tileSize * 0.62f));
            SpritePainter.neonText(g2, pop.text, g2.getFont(),
                    px(pop.col), py(pop.row) - rise, Palette.glow(pop.color, a),
                    Palette.glow(pop.color, a * 0.6f), true);
        }
    }

    private void drawPacman(Graphics2D g2) {
        Pacman pac = model.pac();
        double cx = px(pac.fcol()), cy = py(pac.frow());
        double r = tileSize * 0.46;
        Direction d = pac.dir() == Direction.NONE ? Direction.RIGHT : pac.dir();
        double facing = Math.atan2(d.dy, d.dx);
        if (model.phase() == GamePhase.DYING) {
            SpritePainter.pacmanDeath(g2, cx, cy, r, facing, model.deathProgress());
        } else if (model.phase() != GamePhase.LEVEL_CLEAR) {
            double mouth = (Math.sin(pac.mouthPhase()) + 1) / 2;
            if (pac.dir() == Direction.NONE) mouth = 0.25;
            SpritePainter.pacman(g2, cx, cy, r, facing, mouth);
        }
    }

    private void drawGhosts(Graphics2D g2) {
        if (model.phase() == GamePhase.DYING || model.phase() == GamePhase.LEVEL_CLEAR) return;
        double r = tileSize * 0.46;
        double skirt = model.timeMs() / 1000.0 * 8;
        for (Ghost gh : model.ghosts()) {
            double bob = gh.mode() == GhostMode.IN_HOUSE
                    ? Math.sin(model.timeMs() / 1000.0 * 4) * tileSize * 0.12 : 0;
            double cx = px(gh.fcol()), cy = py(gh.frow()) + bob;
            Direction d = gh.dir() == Direction.NONE ? Direction.UP : gh.dir();
            if (gh.mode() == GhostMode.EATEN) {
                SpritePainter.ghostEyes(g2, cx, cy, r, d.dx, d.dy);
            } else if (gh.mode() == GhostMode.FRIGHTENED) {
                SpritePainter.ghostFrightened(g2, cx, cy, r, model.frightFlashing(), skirt);
            } else {
                SpritePainter.ghost(g2, cx, cy, r, gh.personality().body(),
                        gh.personality().glow(), d.dx, d.dy, skirt);
            }
        }
    }

    private void drawScanlines(Graphics2D g2) {
        if (scanlines == null || scanlines.getWidth() != getWidth()
                || scanlines.getHeight() != getHeight()) {
            if (getWidth() <= 0 || getHeight() <= 0) return;
            scanlines = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D s = scanlines.createGraphics();
            s.setColor(Palette.glow(Palette.BG_GRID_LINE, 0.07f));
            for (int y = 0; y < getHeight(); y += 3) s.drawLine(0, y, getWidth(), y);
            s.dispose();
        }
        g2.drawImage(scanlines, 0, 0, null);
    }

    // -------------------------------------------------------------- input

    private final class Input extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (screen.isAwaitingName()) {
                screen.handleNameKey(e);
                repaint();
                return;
            }
            int code = e.getKeyCode();
            Direction d = Direction.fromKey(code);
            if (d != Direction.NONE) {
                model.setDesiredDir(d);
                return;
            }
            switch (code) {
                case KeyEvent.VK_P: model.togglePause(); break;
                case KeyEvent.VK_M: screen.toggleMute(); break;
                case KeyEvent.VK_R: screen.restart(); break;
                case KeyEvent.VK_Q:
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_BACK_SPACE: screen.backToMenu(); break;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE: screen.confirm(); break;
                default: break;
            }
        }
    }
}
