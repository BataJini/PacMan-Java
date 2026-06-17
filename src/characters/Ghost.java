package characters;

/**
 * One ghost: position (via {@link Entity}) plus mode, personality and the small
 * amount of per-ghost timing/RNG state. All AI decisions are driven from the
 * game loop; this class holds no threads and no Swing references.
 */
public final class Ghost extends Entity {
    private final GhostPersonality personality;
    private GhostMode mode = GhostMode.IN_HOUSE;

    private final int spawnCol;
    private final int spawnRow;
    private final double releaseDelay; // seconds in the house before first exit
    private double timer;              // counts down release / revive

    private long rngState;

    public Ghost(GhostPersonality personality, int spawnCol, int spawnRow,
                 double releaseDelay, long rngSeed) {
        this.personality = personality;
        this.spawnCol = spawnCol;
        this.spawnRow = spawnRow;
        this.releaseDelay = releaseDelay;
        this.timer = releaseDelay;
        this.rngState = rngSeed;
        place(spawnCol, spawnRow, Direction.UP);
    }

    public GhostPersonality personality() { return personality; }
    public GhostMode mode() { return mode; }
    public void setMode(GhostMode m) { this.mode = m; }

    public int spawnCol() { return spawnCol; }
    public int spawnRow() { return spawnRow; }

    public double timer() { return timer; }
    public void setTimer(double t) { this.timer = t; }
    public void tickTimer(double dt) { this.timer -= dt; }

    public void resetToHouse(double releaseSeconds) {
        place(spawnCol, spawnRow, Direction.UP);
        this.mode = GhostMode.IN_HOUSE;
        this.timer = releaseSeconds;
    }

    /** Deterministic per-ghost LCG used for frightened flee choices. */
    public int nextRandom(int bound) {
        rngState = rngState * 1103515245L + 12345L;
        int v = (int) ((rngState >>> 16) & 0x7fffffff);
        return bound <= 0 ? 0 : v % bound;
    }
}
