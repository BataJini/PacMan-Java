package characters;


import game.*;
import assets.Assets;
import javax.swing.*;
import java.util.Random;
import java.util.List;

public class Ghost implements Runnable {
    private int x, y;
    private int dx = 0, dy = 0;
    private ImageIcon icon;
    private int[][] map;
    private JLabel[][] tiles;
    private UpgradeGhost[][] upgrades;
    private volatile boolean running = true;
    private Random random = new Random();
    private List<Ghost> allGhosts;
    private Pacman pacman;
    private GameLogic gameLogic;

    public Ghost(int startX, int startY, ImageIcon icon, int[][] map, JLabel[][] tiles,
                 UpgradeGhost[][] upgrades, List<Ghost> allGhosts, Pacman pacman, GameLogic gameLogic) {
        this.x = startX;
        this.y = startY;
        this.icon = icon;
        this.map = map;
        this.tiles = tiles;
        this.upgrades = upgrades;
        this.allGhosts = allGhosts;
        this.pacman = pacman;
        this.gameLogic = gameLogic;
        chooseRandomDirection();
        SwingUtilities.invokeLater(() -> updateDisplay());
    }

    private void chooseRandomDirection() {
        int[][] cardinalDirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        int[] chosen = cardinalDirs[random.nextInt(4)];
        dx = chosen[0];
        dy = chosen[1];
    }

    public void run() {
        long lastUpgradeTime = System.currentTimeMillis();
        long lastDirectionChange = System.currentTimeMillis();
        long lastAppearanceCheck = System.currentTimeMillis();

        while (running) {
            try {
                Thread.sleep(400 + random.nextInt(200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (System.currentTimeMillis() - lastDirectionChange >= 3000 + random.nextInt(2000)) {
                if (random.nextInt(100) < 25) {
                    chooseRandomDirection();
                    lastDirectionChange = System.currentTimeMillis();
                }
            }

            move();
            checkPacmanCollision();
            
            if (System.currentTimeMillis() - lastAppearanceCheck >= 500) {
                updateGhostAppearance();
                lastAppearanceCheck = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - lastUpgradeTime >= 5000) {
                lastUpgradeTime = System.currentTimeMillis();
                if (random.nextInt(100) < 25) {
                    spawnUpgrade();
                }
            }
        }

        SwingUtilities.invokeLater(() -> restoreTileContent(x, y));
    }

    private void move() {
        int newX = x + dx;
        int newY = y + dy;

        if (newY >= 0 && newY < map.length && newX >= 0 && newX < map[0].length && map[newY][newX] != 1) {
            boolean collision = false;
            synchronized (allGhosts) {
                for (Ghost other : allGhosts) {
                    if (other != this && other.x == newX && other.y == newY) {
                        collision = true;
                        break;
                    }
                }
            }

            if (!collision) {
                final int oldX = x;
                final int oldY = y;
                x = newX;
                y = newY;

                SwingUtilities.invokeLater(() -> {
                    restoreTileContent(oldX, oldY);
                    updateDisplay();
                });
            } else {
                chooseRandomDirection();
            }
        } else {
            chooseRandomDirection();
        }
    }

    private void updateDisplay() {
        tiles[y][x].setIcon(icon);
    }

    private void restoreTileContent(int tileX, int tileY) {
        if (upgrades[tileY][tileX] != null) {
            tiles[tileY][tileX].setIcon(upgrades[tileY][tileX].getIcon());
        } else if (map[tileY][tileX] == 0) {
            tiles[tileY][tileX].setIcon(Assets.FOOD);
        } else {
            tiles[tileY][tileX].setIcon(null);
        }
    }

    private void checkPacmanCollision() {
        synchronized (pacman) {
            if (pacman.getX() == x && pacman.getY() == y) {
                SwingUtilities.invokeLater(() -> gameLogic.handlePacmanCaught());
            }
        }
    }
    
    private void updateGhostAppearance() {
        GameState gameState = gameLogic.getGameState();
        if (gameState != null && gameState.isInvincibilityActive()) {

            SwingUtilities.invokeLater(() -> {
                icon = Assets.SCARED_GHOST;
                updateDisplay();
            });
        } else {

            if (icon == Assets.SCARED_GHOST) {
                SwingUtilities.invokeLater(() -> {

                    int index = allGhosts.indexOf(this);
                    if (index >= 0) {
                        switch (index % 4) {
                            case 0: icon = Assets.ORANGE_GHOST; break;
                            case 1: icon = Assets.RED_GHOST; break;
                            case 2: icon = Assets.PINK_GHOST; break;
                            case 3: icon = Assets.Blue_GHOST; break;
                        }
                        updateDisplay();
                    }
                });
            }
        }
    }

    private void spawnUpgrade() {
        int[] offsets = {-1, 0, 1};
        for (int dy : offsets) {
            for (int dx : offsets) {
                if (dx == 0 && dy == 0) continue;

                int newX = x + dx;
                int newY = y + dy;

                if (newY >= 0 && newY < map.length && newX >= 0 && newX < map[0].length
                        && map[newY][newX] != 1 && upgrades[newY][newX] == null) {

                    boolean occupied = false;
                    synchronized (allGhosts) {
                        for (Ghost ghost : allGhosts) {
                            if (ghost.x == newX && ghost.y == newY) {
                                occupied = true;
                                break;
                            }
                        }
                    }

                    if (!occupied && pacman.getX() != newX && pacman.getY() != newY) {
                        UpgradeGhost upgrade = new UpgradeGhost(newX, newY);
                        upgrades[newY][newX] = upgrade;

                        SwingUtilities.invokeLater(() -> {
                            tiles[newY][newX].setIcon(upgrade.getIcon());
                        });
                        return;
                    }
                }
            }
        }
    }

    public void stop() {
        running = false;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public static java.awt.Point StartingPostion(int[][] map, List<Ghost> existingGhost, Pacman pacman) {
        Random random = new Random();
        int attempts = 0;

        while (attempts < 100) {
            int x = random.nextInt(map[0].length);
            int y = random.nextInt(map.length);

            if (map[y][x] != 1) {
                boolean occupied = false;

                if (pacman.getX() == x && pacman.getY() == y) {
                    occupied = true;
                }

                for (Ghost ghost : existingGhost) {
                    if (ghost.getX() == x && ghost.getY() == y) {
                        occupied = true;
                        break;
                    }
                }

                if (!occupied) {
                    return new java.awt.Point(x, y);
                }
            }
            attempts++;
        }

        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[0].length; x++) {
                if (map[y][x] != 1) {
                    return new java.awt.Point(x, y);
                }
            }
        }

        return new java.awt.Point(1, 1);
    }
}
