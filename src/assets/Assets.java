package assets;

import javax.swing.*;

public class Assets {
    public static final ImageIcon WALL = load("/assets/wall.png");
    public static final ImageIcon PACMAN_UP = load("/assets/pacmanUp.png");
    public static final ImageIcon PACMAN_DOWN = load("/assets/pacmanDown.png");
    public static final ImageIcon PACMAN_LEFT = load("/assets/pacmanLeft.png");
    public static final ImageIcon PACMAN_RIGHT = load("/assets/pacmanRight.png");
    public static final ImageIcon PACMAN_CLOSED = load("/assets/pacmanClosed.png");
    public static final ImageIcon ORANGE_GHOST = load("/assets/orangeGhost.png");
    public static final ImageIcon RED_GHOST = load("/assets/redGhost.png");
    public static final ImageIcon PINK_GHOST = load("/assets/pinkGhost.png");
    public static final ImageIcon Blue_GHOST = load("/assets/blueGhost.png");
    public static final ImageIcon SCARED_GHOST = load("/assets/scaredGhost.png");
    public static final ImageIcon FOOD = load("/assets/food.png");
    public static final ImageIcon LIFE = load("/assets/life.png");
    public static final ImageIcon SPEED = load("/assets/speed.png");
    public static final ImageIcon INVINCIBILITY = load("/assets/invincibility.png");

    private static ImageIcon load(String path) {
        return new ImageIcon(Assets.class.getResource(path));
    }
}
