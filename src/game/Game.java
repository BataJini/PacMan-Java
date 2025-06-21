package game;

import assets.Assets;
import characters.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class Game extends JFrame {
    private int[][] map;
    private Pacman pacman;
    private GameState gameState;
    private GameUI ui;
    private GameLogic gameLogic;

    public Game(int boardSize) {
        this.map = Map.getMap(boardSize);
        this.pacman = new Pacman();
        this.gameState = new GameState();
        this.ui = new GameUI();
        this.gameLogic = new GameLogic(map, pacman, gameState, ui);

        initializeWindow();
        initializeUI();
        initializeControls();
        startGameLoops();
    }

    private void initializeWindow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int mapWidth = map[0].length;
        int mapHeight = map.length;
        int baseTileSize;
        if (mapWidth <= 15) {
            baseTileSize = 25;
        } else if (mapWidth <= 20) {
            baseTileSize = 28;
        } else {
            baseTileSize = 32;
        }
        int windowWidth = mapWidth * baseTileSize + 60;
        int windowHeight = mapHeight * baseTileSize + 120;
        int minWidth = Math.max(300, mapWidth * 12);
        int minHeight = Math.max(350, mapHeight * 12 + 100);

        windowWidth = Math.max(windowWidth, minWidth);
        windowHeight = Math.max(windowHeight, minHeight);
        windowWidth = Math.min(windowWidth, screenSize.width - 100);
        windowHeight = Math.min(windowHeight, screenSize.height - 100);

        setSize(windowWidth, windowHeight);
        setMinimumSize(new Dimension(minWidth, minHeight));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(Color.BLACK);

        setLayout(new BorderLayout());
        setFocusable(true);
        setTitle("PacMan");
    }

    private void initializeUI() {
        ui.initializeUI(this, map, this::returnToMenu);
        setVisible(true);
        ui.getTiles()[pacman.getY()][pacman.getX()].setIcon(Assets.PACMAN_RIGHT);
        gameLogic.initializeEnemies();
    }

    private void initializeControls() {
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();

                int newDx = 0, newDy = 0;
                if (key == KeyEvent.VK_LEFT) {
                    newDx = -1; newDy = 0;
                } else if (key == KeyEvent.VK_RIGHT) {
                    newDx = 1; newDy = 0;
                } else if (key == KeyEvent.VK_UP) {
                    newDx = 0; newDy = -1;
                } else if (key == KeyEvent.VK_DOWN) {
                    newDx = 0; newDy = 1;
                }
                gameLogic.setPacmanDirection(newDx, newDy);
            }
        });
    }

    private void startGameLoops() {
        new Thread(() -> {
            while (gameState.isRunning()) {
                try {
                    int moveDelay = gameState.isSpeedBoostActive() ? 100 : 200;
                    Thread.sleep(moveDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (gameState.isRunning()) {
                    gameLogic.movePacman();
                    if (gameState.isGameOver()) {
                        gameLogic.handleGameOver(this);
                        break;
                    }
                }
            }
        }).start();
        new Thread(() -> {
            while (gameState.isRunning()) {
                SwingUtilities.invokeLater(() -> ui.updateTime(gameState.getElapsedTime()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private void returnToMenu() {
        gameState.setRunning(false);
        gameLogic.stopEnemies();
        dispose();
        SwingUtilities.invokeLater(() -> new menu.MainMenu());
    }
}
