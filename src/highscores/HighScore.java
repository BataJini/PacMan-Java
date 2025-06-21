package highscores;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HighScore extends JFrame {
    public HighScore() {

        Dimension screenSize = this.getToolkit().getScreenSize();
        int width = (int) screenSize.width / 2;
        int height = (int) screenSize.height / 2;
        this.setSize(width, height);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(Color.BLACK);
        this.setLayout(new BorderLayout());

        JButton backButton = new JButton("<- Back");
        backButton.setFont(new Font("Monospaced", Font.BOLD, 20));
        backButton.setFocusPainted(false);
        this.add(backButton, BorderLayout.NORTH);
        backButton.addActionListener(e -> {
            SwingUtilities.getWindowAncestor(backButton).dispose();

            SwingUtilities.invokeLater(menu.MainMenu::new);


        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.BLACK);
        topPanel.add(backButton);
        add(topPanel, BorderLayout.NORTH);

        HighScoreManager manager = new HighScoreManager();
        List<HighScoreEntry> scores = manager.getHighScores();

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (HighScoreEntry entry : scores) {
            listModel.addElement(entry.toString());
        }

        JList<String> scoreList = new JList<>(listModel);
        scoreList.setFont(new Font("Monospaced", Font.BOLD, 18));
        scoreList.setBackground(Color.DARK_GRAY);
        scoreList.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(scoreList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);


        this.setVisible(true);

    }

}
