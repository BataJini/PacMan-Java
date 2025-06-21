package game;

import assets.Assets;
import characters.*;
import javax.swing.*;
import java.awt.*;

public class GameUI {
    private JLabel scoreLabel;
    private JLabel livesLabel;
    private JLabel timeLabel;
    private JLabel[][] tiles;
    private JLabel upgradeMessageLabel;
    private JLabel speedBoostLabel;
    private JLabel invincibilityLabel;

    public void initializeUI(JFrame frame, int[][] map, Runnable returnToMenu) {
        JPanel topPanel = createTopPanel(returnToMenu);
        frame.add(topPanel, BorderLayout.NORTH);
        JPanel gamePanel = createGamePanel(map);
        frame.add(gamePanel, BorderLayout.CENTER);
    }

    private JPanel createTopPanel(Runnable returnToMenu) {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(Color.BLACK);
        topPanel.setPreferredSize(new Dimension(400, 80));
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.X_AXIS));
        statsPanel.setBackground(Color.BLACK);


        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.WHITE);
        livesLabel = new JLabel("Lives: 3");
        livesLabel.setForeground(Color.WHITE);
        timeLabel = new JLabel("Time: 0s");
        timeLabel.setForeground(Color.WHITE);


        speedBoostLabel = new JLabel("SPEED");
        speedBoostLabel.setForeground(UpgradeType.SPEED.getColor());
        speedBoostLabel.setVisible(false);


        invincibilityLabel = new JLabel("INVINCIBLE");
        invincibilityLabel.setForeground(UpgradeType.INVINCIBILITY.getColor());
        invincibilityLabel.setVisible(false);


        JButton returnButton = new JButton("Return to Menu");
        returnButton.setFocusable(false);
        returnButton.addActionListener(e -> returnToMenu.run());

        statsPanel.add(Box.createHorizontalStrut(10));
        statsPanel.add(scoreLabel);
        statsPanel.add(Box.createHorizontalStrut(20));
        statsPanel.add(livesLabel);
        statsPanel.add(Box.createHorizontalStrut(20));
        statsPanel.add(timeLabel);
        statsPanel.add(Box.createHorizontalStrut(20));
        statsPanel.add(speedBoostLabel);
        statsPanel.add(Box.createHorizontalStrut(10));
        statsPanel.add(invincibilityLabel);
        statsPanel.add(Box.createHorizontalGlue());
        statsPanel.add(returnButton);
        statsPanel.add(Box.createHorizontalStrut(10));

        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        messagePanel.setBackground(Color.BLACK);
        upgradeMessageLabel = new JLabel(" ");
        upgradeMessageLabel.setForeground(Color.YELLOW);
        upgradeMessageLabel.setFont(new Font("Arial", Font.BOLD, 12));
        messagePanel.add(upgradeMessageLabel);
        topPanel.add(statsPanel, BorderLayout.NORTH);
        topPanel.add(messagePanel, BorderLayout.SOUTH);
        return topPanel;
    }

    private JPanel createGamePanel(int[][] map) {
        JPanel panel = new JPanel(new GridLayout(map.length, map[0].length, 1, 1));
        panel.setBackground(Color.BLACK);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tiles = new JLabel[map.length][map[0].length];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                JLabel label = new JLabel();
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setVerticalAlignment(JLabel.CENTER);
                label.setOpaque(true);
                label.setBackground(Color.BLACK);
                if (map[i][j] == 1) {
                    label.setIcon(Assets.WALL);
                } else {
                    label.setIcon(Assets.FOOD);
                }
                tiles[i][j] = label;
                panel.add(label);
            }
        }
        return panel;
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public void updateLives(int lives) {
        livesLabel.setText("Lives: " + lives);
    }

    public void updateTime(long elapsed) {
        timeLabel.setText("Time: " + elapsed + "s");
    }

    public JLabel[][] getTiles() {
        return tiles;
    }

    public void showUpgradeMessage(String message) {
        upgradeMessageLabel.setText(message);
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                SwingUtilities.invokeLater(() -> upgradeMessageLabel.setText(" "));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void showActiveUpgrade(UpgradeType type) {
        if (type == UpgradeType.SPEED) {
            speedBoostLabel.setVisible(true);
        } else if (type == UpgradeType.INVINCIBILITY) {
            invincibilityLabel.setVisible(true);
        }
    }

    public void clearActiveUpgrade(UpgradeType type) {
        if (type == UpgradeType.SPEED) {
            speedBoostLabel.setVisible(false);
        } else if (type == UpgradeType.INVINCIBILITY) {
            invincibilityLabel.setVisible(false);
        }
    }
}
