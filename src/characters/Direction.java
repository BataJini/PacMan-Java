package characters;

import java.awt.event.KeyEvent;

/** Cardinal movement directions on the grid (dx = column delta, dy = row delta). */
public enum Direction {
    NONE(0, 0),
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Direction opposite() {
        switch (this) {
            case UP: return DOWN;
            case DOWN: return UP;
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            default: return NONE;
        }
    }

    /** Facing angle in degrees, math convention: RIGHT=0, UP=90, LEFT=180, DOWN=270. */
    public double angleDeg() {
        switch (this) {
            case RIGHT: return 0;
            case UP: return 90;
            case LEFT: return 180;
            case DOWN: return 270;
            default: return 0;
        }
    }

    /** Maps arrow / WASD key codes to a direction, or NONE. */
    public static Direction fromKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W: return UP;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S: return DOWN;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A: return LEFT;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D: return RIGHT;
            default: return NONE;
        }
    }
}
