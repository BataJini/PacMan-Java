package game;

import java.awt.Point;

/**
 * Mutable runtime grid for one game. Owns the load-time transform of the raw
 * {@link Map} array (0/1) into extended tile codes, plus all spawn / tunnel /
 * corner metadata. Sole owner of board-cell mutation (dot eating).
 */
public final class Board {
    private final int cols;
    private final int rows;
    private final int[][] grid;

    private int totalDots;
    private int dotsRemaining;
    private int[][] homeDist;

    private final Point pacSpawn;
    private final Point home;          // ghost revive point (house center)
    private final Point[] ghostSpawns; // per-ghost start slots
    private final Point exit;          // tile just outside the house
    private final Point fruitSpot;
    private final int tunnelRow;

    private final Point cornerTopRight;
    private final Point cornerTopLeft;
    private final Point cornerBottomRight;
    private final Point cornerBottomLeft;

    public Board(BoardSizeOption size) {
        int[][] raw = Map.getMap(size.mapId());
        this.rows = raw.length;
        this.cols = raw[0].length;
        this.grid = new int[rows][cols];

        // 1. Base copy: wall -> WALL, path -> DOT.
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = (raw[r][c] == 1) ? 1 : 2;
            }
        }

        // Spawns / house centers (verified-open per map; snapped for safety).
        this.pacSpawn = snapOpen(pacSpawnFor(size));
        this.home = snapOpen(homeFor(size));
        this.exit = snapOpen(new Point(home.x, home.y - 2));
        this.ghostSpawns = ghostSpawnsFor(size);

        // 2. Pac spawn -> PAC_SPAWN (clears any dot).
        set(pacSpawn, 8);

        // 3. Ghost home + slots -> EMPTY (no dots under the pen).
        set(home, 0);
        for (Point p : ghostSpawns) set(p, 0);

        // 4. Power pellets near the four quadrant corners.
        for (Point p : powerPelletsFor(size)) {
            Point open = snapOpen(p);
            set(open, 3);
        }

        // 5. Tunnel row: carve both edge tiles, enable wrap.
        this.tunnelRow = tunnelRowFor(size);
        if (tunnelRow >= 0) {
            grid[tunnelRow][0] = 6;
            grid[tunnelRow][cols - 1] = 6;
        }

        // 6. Fruit spot: a walkable tile a few rows below the house.
        this.fruitSpot = snapOpen(new Point(home.x, home.y + 3));

        // Scatter corners (generic in-bounds quadrant corners).
        this.cornerTopRight = new Point(cols - 2, 1);
        this.cornerTopLeft = new Point(1, 1);
        this.cornerBottomRight = new Point(cols - 2, rows - 2);
        this.cornerBottomLeft = new Point(1, rows - 2);

        // Count live pellets.
        int dots = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 2 || grid[r][c] == 3) dots++;
            }
        }
        this.totalDots = dots;
        this.dotsRemaining = dots;
    }

    // ----- per-map placement tables (col,row) -----

    private static Point pacSpawnFor(BoardSizeOption s) {
        switch (s) {
            case SMALL: return new Point(7, 13);
            case MEDIUM: return new Point(9, 15);
            default: return new Point(13, 23);
        }
    }

    private static Point homeFor(BoardSizeOption s) {
        switch (s) {
            case SMALL: return new Point(7, 7);
            case MEDIUM: return new Point(9, 8);
            default: return new Point(13, 11);
        }
    }

    private Point[] ghostSpawnsFor(BoardSizeOption s) {
        switch (s) {
            case SMALL:
                return new Point[] {
                        snapOpen(new Point(7, 7)),
                        snapOpen(new Point(8, 7)),
                        snapOpen(new Point(9, 7)) };
            case MEDIUM:
                return new Point[] {
                        snapOpen(new Point(8, 8)),
                        snapOpen(new Point(9, 8)),
                        snapOpen(new Point(10, 8)),
                        snapOpen(new Point(9, 9)) };
            default:
                return new Point[] {
                        snapOpen(new Point(11, 11)),
                        snapOpen(new Point(13, 11)),
                        snapOpen(new Point(15, 11)),
                        snapOpen(new Point(13, 10)) };
        }
    }

    private static Point[] powerPelletsFor(BoardSizeOption s) {
        switch (s) {
            case SMALL:
                return new Point[] {
                        new Point(1, 2), new Point(13, 2), new Point(1, 12), new Point(13, 12) };
            case MEDIUM:
                return new Point[] {
                        new Point(1, 2), new Point(18, 2), new Point(1, 14), new Point(18, 14) };
            default:
                return new Point[] {
                        new Point(1, 3), new Point(25, 3), new Point(1, 23), new Point(25, 23) };
        }
    }

    private static int tunnelRowFor(BoardSizeOption s) {
        switch (s) {
            case SMALL: return 9;
            case MEDIUM: return 9;
            default: return 14;
        }
    }

    // ----- queries -----

    public int cols() { return cols; }
    public int rows() { return rows; }
    public int totalDots() { return totalDots; }
    public int dotsRemaining() { return dotsRemaining; }
    public boolean isCleared() { return dotsRemaining == 0; }

    public Point pacSpawn() { return new Point(pacSpawn); }
    public Point home() { return new Point(home); }
    public Point exit() { return new Point(exit); }
    public Point fruitSpot() { return new Point(fruitSpot); }
    public int ghostSpawnCount() { return ghostSpawns.length; }
    public Point ghostSpawn(int i) { return new Point(ghostSpawns[i % ghostSpawns.length]); }

    public Point cornerTopRight() { return new Point(cornerTopRight); }
    public Point cornerTopLeft() { return new Point(cornerTopLeft); }
    public Point cornerBottomRight() { return new Point(cornerBottomRight); }
    public Point cornerBottomLeft() { return new Point(cornerBottomLeft); }

    public boolean inBounds(int c, int r) {
        return c >= 0 && c < cols && r >= 0 && r < rows;
    }

    public TileType typeAt(int c, int r) {
        if (!inBounds(c, r)) return TileType.WALL;
        return TileType.of(grid[r][c]);
    }

    public boolean isTunnelRow(int r) {
        return r == tunnelRow;
    }

    public boolean isTunnel(int c, int r) {
        return typeAt(c, r) == TileType.TUNNEL;
    }

    public boolean blocksPacman(int c, int r) {
        return typeAt(c, r).blocksPacman();
    }

    public boolean blocksGhost(int c, int r, boolean leavingOrEaten) {
        return typeAt(c, r).blocksGhost(leavingOrEaten);
    }

    /** Eats a pellet at the tile; returns NONE / DOT / POWER. */
    public EatResult eat(int c, int r) {
        if (!inBounds(c, r)) return EatResult.NONE;
        int code = grid[r][c];
        if (code == 2) {
            grid[r][c] = 0;
            dotsRemaining--;
            return EatResult.DOT;
        }
        if (code == 3) {
            grid[r][c] = 0;
            dotsRemaining--;
            return EatResult.POWER;
        }
        return EatResult.NONE;
    }

    public enum EatResult { NONE, DOT, POWER }

    /**
     * BFS distance (in tiles) from every ghost-passable cell to the house center,
     * honouring tunnel wrap. Used to steer eaten "eyes" home on any connected
     * maze without the greedy pather's cycle traps. Unreachable / wall = -1.
     */
    public int[][] homeDistance() {
        if (homeDist != null) return homeDist;
        int[][] d = new int[rows][cols];
        for (int[] row : d) java.util.Arrays.fill(row, -1);
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        d[home.y][home.x] = 0;
        q.add(new int[] {home.x, home.y});
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int c = cur[0], r = cur[1], base = d[r][c];
            for (int[] dir : dirs) {
                int nr = r + dir[1];
                int nc = c + dir[0];
                if (isTunnelRow(nr)) {
                    if (nc < 0) nc = cols - 1;
                    else if (nc >= cols) nc = 0;
                }
                if (!inBounds(nc, nr) || d[nr][nc] != -1) continue;
                if (typeAt(nc, nr) == TileType.WALL) continue;
                d[nr][nc] = base + 1;
                q.add(new int[] {nc, nr});
            }
        }
        homeDist = d;
        return d;
    }

    // ----- helpers -----

    private void set(Point p, int code) {
        if (inBounds(p.x, p.y)) grid[p.y][p.x] = code;
    }

    /** Nearest passable (non-wall) tile to the target via expanding search. */
    private Point snapOpen(Point target) {
        if (inBounds(target.x, target.y) && grid[target.y][target.x] != 1) {
            return new Point(target);
        }
        for (int radius = 1; radius < Math.max(cols, rows); radius++) {
            for (int dr = -radius; dr <= radius; dr++) {
                for (int dc = -radius; dc <= radius; dc++) {
                    if (Math.abs(dr) != radius && Math.abs(dc) != radius) continue;
                    int c = target.x + dc, r = target.y + dr;
                    if (inBounds(c, r) && grid[r][c] != 1) return new Point(c, r);
                }
            }
        }
        return new Point(1, 1);
    }
}
