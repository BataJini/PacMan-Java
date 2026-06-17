package characters;

import java.util.function.Consumer;

/**
 * Base for grid-bound actors with smooth pixel interpolation. The grid tile
 * {@code (col,row)} is authoritative; {@code progress} in [0,1) is the fraction
 * traversed toward the tile in {@code dir}. Decisions happen exactly at tile
 * centers (progress == 0). All mutation happens on the EDT inside the loop.
 */
public abstract class Entity {
    protected int col;
    protected int row;
    protected Direction dir = Direction.NONE;
    protected double progress;

    public int col() { return col; }
    public int row() { return row; }
    public Direction dir() { return dir; }
    public double progress() { return progress; }

    public void setDir(Direction d) { this.dir = d; }

    /** Snaps the actor to a tile center, facing the given direction. */
    public void place(int col, int row, Direction dir) {
        this.col = col;
        this.row = row;
        this.dir = dir;
        this.progress = 0;
    }

    /** Fractional column of the actor's center (tile units). */
    public double fcol() { return col + 0.5 + dir.dx * progress; }

    /** Fractional row of the actor's center (tile units). */
    public double frow() { return row + 0.5 + dir.dy * progress; }

    /** True when sitting exactly on a tile center. */
    public boolean atCenter() { return progress < 1e-9; }

    /**
     * Reverses direction while preserving physical position (rebases onto the
     * tile being entered, which is guaranteed passable since we were moving in).
     */
    public void reverse() {
        if (dir == Direction.NONE) return;
        col += dir.dx;
        row += dir.dy;
        progress = 1.0 - progress;
        dir = dir.opposite();
    }

    /**
     * Advances by {@code tiles} tile-lengths, invoking {@code atCenter} whenever
     * the actor reaches a tile center (and once up-front if stopped). The
     * callback performs wrap + eat + the direction decision, and may set
     * {@code dir = NONE} to stop the actor.
     */
    public void advance(double tiles, Consumer<Entity> atCenter) {
        if (dir == Direction.NONE) {
            atCenter.accept(this);
        }
        int guard = 0;
        while (tiles > 1e-9 && guard++ < 16) {
            if (dir == Direction.NONE) break;
            double remain = 1.0 - progress;
            if (tiles < remain - 1e-9) {
                progress += tiles;
                break;
            }
            tiles -= remain;
            col += dir.dx;
            row += dir.dy;
            progress = 0;
            atCenter.accept(this);
        }
    }

    /** Directly sets the grid tile without changing facing (used for tunnel wrap). */
    public void wrapColumn(int newCol) { this.col = newCol; }
}
