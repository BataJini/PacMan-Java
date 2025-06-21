package game;

import assets.Assets;

import characters.*;
import highscores.*;
import javax.swing.*;

import menu.MainMenu;

import java.util.ArrayList;
import java.util.List;
import java.awt.Point;

public class GameLogic {
    private int[][] map;
    private Pacman pacman;
    private GameState gameState;
    private GameUI ui;
    private UpgradeGhost[][] upgrades;
    private List<Ghost> enemies = new ArrayList<>();
    private Object gameStateLock = new Object();

    public GameLogic(int[][] map, Pacman pacman, GameState gameState, GameUI ui) {
        this.map = map;
        this.pacman = pacman;
        this.gameState = gameState;
        this.ui = ui;
        this.upgrades = new UpgradeGhost[map.length][map[0].length];
    }

    public void initializeEnemies() {
        stopEnemies();
        enemies.clear();
        ImageIcon[] ghostIcons = {
                Assets.ORANGE_GHOST,
                Assets.RED_GHOST,
                Assets.PINK_GHOST,
                Assets.Blue_GHOST
        };

        for (int i = 0; i < 4; i++) {
            Point startPos = Ghost.StartingPostion(map, enemies, pacman);
            Ghost ghost = new Ghost(
                    startPos.x, startPos.y,
                    ghostIcons[i % ghostIcons.length],
                    map, ui.getTiles(), upgrades, enemies, pacman, this
            );
            enemies.add(ghost);
            new Thread(ghost, "Ghost-" + (i + 1)).start();
        }
    }

    public void movePacman() {
        synchronized (gameStateLock) {
            if (!gameState.isRunning()) {
                return;
            }
            JLabel[][] tiles = ui.getTiles();
            if (!pacman.canMove(map)) {
                updatePacmanDisplay();
                return;
            }
            tiles[pacman.getY()][pacman.getX()].setIcon(null);
            pacman.move();
            if (map[pacman.getY()][pacman.getX()] == 0) {
                gameState.incrementScore();
                ui.updateScore(gameState.getScore());
                map[pacman.getY()][pacman.getX()] = -1;
            }
            updatePacmanDisplay();
            handleUpgrades();
            if (checkWinCondition()) {
                handleGameWin();
            }
        }
    }

    private void updatePacmanDisplay() {
        JLabel[][] tiles = ui.getTiles();
        tiles[pacman.getY()][pacman.getX()].setIcon(pacman.getCurrentIcon());
    }

    private void handleUpgrades() {
        int pacX = pacman.getX();
        int pacY = pacman.getY();
        if (upgrades[pacY][pacX] != null) {
            UpgradeGhost upgrade = upgrades[pacY][pacX];
            upgrades[pacY][pacX] = null;
            switch (upgrade.getType()) {
                case SPEED:
                    gameState.activateSpeedBoost();
                    gameState.incrementScore();
                    showUpgradeMessage("Speed Boost activated");
                    startUpgradeEffectThread(upgrade.getType());
                    break;
                case LIFE:
                    gameState.addLife();
                    ui.updateLives(gameState.getLives());
                    showUpgradeMessage("Extra Life collected");
                    break;
                case INVINCIBILITY:
                    gameState.activateInvincibility();
                    gameState.incrementScore();
                    showUpgradeMessage("Invincibility activated");
                    startUpgradeEffectThread(upgrade.getType());
                    break;
            }
        }
    }

    private void startUpgradeEffectThread(UpgradeType type) {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> ui.showActiveUpgrade(type));
                long duration = (type == UpgradeType.SPEED) ? 10000 : 8000;
                Thread.sleep(duration);
                SwingUtilities.invokeLater(() -> ui.clearActiveUpgrade(type));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void showUpgradeMessage(String message) {
        SwingUtilities.invokeLater(() -> ui.showUpgradeMessage(message));
    }

    private boolean checkWinCondition() {
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[0].length; x++) {
                if (map[y][x] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private void handleGameWin() {
        synchronized (gameStateLock) {
            if (!gameState.isRunning()) {
                return;
            }
            gameState.setRunning(false);
            stopEnemies();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "You won!\nScore: " + gameState.getScore() +
                                "\nTime: " + gameState.getElapsedTime() + " seconds");
                String name = JOptionPane.showInputDialog(null, "Enter your name for high scores:");
                if (name != null && !name.trim().isEmpty()) {
                    HighScoreManager manager = new HighScoreManager();
                    manager.addScore(name.trim(), gameState.getScore());
                }
                ((JFrame) SwingUtilities.getWindowAncestor(ui.getTiles()[0][0])).dispose();
                new MainMenu();
            });
        }
    }

    public void handlePacmanCaught() {
        synchronized (gameStateLock) {
            if (!gameState.isRunning()) {
                return;
            }
            if (gameState.isInvincibilityActive()) {
                showUpgradeMessage("Ghost repelled by invincibility!");
                return;
            }
            gameState.loseLife();
            ui.updateLives(gameState.getLives());
            if (gameState.isGameOver()) {
                handleGameOver((JFrame) SwingUtilities.getWindowAncestor(ui.getTiles()[0][0]));
            }
        }
    }

    public void stopEnemies() {
        for (Ghost ghost : enemies) {
            ghost.stop();
        }
    }

    public void handleGameOver(JFrame parentFrame) {
        synchronized (gameStateLock) {
            if (!gameState.isRunning()) {
                return;
            }
            gameState.setRunning(false);
            stopEnemies();
            SwingUtilities.invokeLater(() -> {
                String name = JOptionPane.showInputDialog(parentFrame,
                        "Game Over! Final Score: " + gameState.getScore() +
                                "\nEnter your name:");
                if (name != null && !name.trim().isEmpty()) {
                    HighScoreManager manager = new HighScoreManager();
                    manager.addScore(name.trim(), gameState.getScore());
                }
                parentFrame.dispose();
                new MainMenu();
            });
        }
    }

    public void setPacmanDirection(int dx, int dy) {
        synchronized (pacman) {
            pacman.setDirection(dx, dy);
        }
    }

    public List<Ghost> getEnemies() {
        return enemies;
    }

    public UpgradeGhost[][] getUpgrades() {
        return upgrades;
    }
    
    public GameState getGameState() {
        return gameState;
    }
}
