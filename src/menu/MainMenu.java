package menu;

import javax.swing.*;
import java.awt.*;
import game.*;
import highscores.*;

public class MainMenu extends JFrame {
    private static final int SMALL_SIZE = Map.SMALL_MAP;
    private static final int MEDIUM_SIZE = Map.MEDIUM_MAP;
    private static final int LARGE_SIZE = Map.LARGE_MAP;

    public MainMenu() {
        this.setTitle("PacMan");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width / 2;
        int height = screenSize.height / 2;
        this.setSize(width, height);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(Color.BLACK);
        this.setLayout(new BorderLayout());

        JLabel label = new JLabel("PAC-MAN");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Monospaced", Font.BOLD, 45));
        label.setForeground(Color.YELLOW);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        this.add(label, BorderLayout.NORTH);

        JButton newGameBtn = new JButton("New Game");
        newGameBtn.setFont(new Font("Monospaced", Font.PLAIN, 18));
        newGameBtn.addActionListener(e -> {
            String[] options = {"Small", "Medium", "Large"};
            int[] boardSizes = {SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE};
            int choice = JOptionPane.showOptionDialog(
                    this, "Select board size:", "Board Size",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]
            );
            if (choice >= 0) {
                this.dispose();
                int boardSize = boardSizes[choice];
                SwingUtilities.invokeLater(() -> new Game(boardSize));
            }
        });

        JButton highScoresBtn = new JButton("High Scores");
        highScoresBtn.setFont(new Font("Monospaced", Font.PLAIN, 18));
        highScoresBtn.addActionListener(e -> {
            this.dispose();
            SwingUtilities.invokeLater(HighScore::new);
        });

        JButton exitBtn = new JButton("Exit");
        exitBtn.setFont(new Font("Monospaced", Font.PLAIN, 18));
        exitBtn.addActionListener(e -> System.exit(0));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        newGameBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        highScoresBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonPanel.add(newGameBtn);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(highScoresBtn);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(exitBtn);

        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setOpaque(false);
        wrapperPanel.add(buttonPanel);
        this.add(wrapperPanel, BorderLayout.CENTER);

        this.setVisible(true);
    }
}