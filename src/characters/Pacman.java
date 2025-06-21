package characters;

import assets.Assets;
import javax.swing.*;

public class Pacman {
    private int x = 1, y = 1;
    private int dx = 0, dy = 0;
    private boolean mouthOpen = true;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setDirection(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public boolean canMove(int[][] map) {
        if (dx == 0 && dy == 0) return false;
        int newX = x + dx;
        int newY = y + dy;
        return !(newY < 0 || newY >= map.length || newX < 0 || newX >= map[0].length || map[newY][newX] == 1);
    }

    public void move() {
        x += dx;
        y += dy;
    }

    public ImageIcon getCurrentIcon() {
        ImageIcon iconToUse;
        if (mouthOpen) {
            if (dx == -1) iconToUse = Assets.PACMAN_LEFT;
            else if (dx == 1) iconToUse = Assets.PACMAN_RIGHT;
            else if (dy == -1) iconToUse = Assets.PACMAN_UP;
            else if (dy == 1) iconToUse = Assets.PACMAN_DOWN;
            else iconToUse = Assets.PACMAN_RIGHT;
        } else {
            iconToUse = Assets.PACMAN_CLOSED;
        }
        mouthOpen = !mouthOpen;
        return iconToUse;
    }
}