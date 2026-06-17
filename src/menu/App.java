package menu;

import assets.Palette;
import assets.SoundEngine;
import game.BoardSizeOption;
import game.GameHost;
import game.GameScreen;
import highscores.HighScoreEntry;
import highscores.HighScoreManager;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Single-window router. Owns the {@link JFrame} + {@link CardLayout}, the shared
 * {@link SoundEngine} and {@link HighScoreManager}, and the screen lifecycle.
 * Implements {@link GameHost} so the game package can call back without importing
 * the menu package.
 */
public final class App implements GameHost {
    private final JFrame frame;
    private final JPanel root;
    private final CardLayout cards;
    private final SoundEngine sound = new SoundEngine();
    private final HighScoreManager scores = new HighScoreManager();

    private final MainMenu menu;
    private final HighScoresScreen scoresScreen;
    private GameScreen gameScreen;
    private Screen current;

    public App() {
        frame = new JFrame("NEON PAC-MAN");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { quit(); }
        });

        cards = new CardLayout();
        root = new JPanel(cards);
        root.setBackground(Palette.BG_DEEP);

        menu = new MainMenu(this);
        scoresScreen = new HighScoresScreen(this);
        root.add(menu, "MENU");
        root.add(scoresScreen, "SCORES");

        frame.setContentPane(root);
        frame.setSize(760, 820);
        frame.setMinimumSize(new Dimension(480, 560));
        frame.setLocationRelativeTo(null);
    }

    public void start() {
        frame.setVisible(true);
        showMenu();
    }

    // ----- routing -----

    private void showScreen(String name, Screen next) {
        if (current != null) current.onHide();
        current = next;
        cards.show(root, name);
        next.onShow();
    }

    public void showMenu() { showScreen("MENU", menu); }

    public void showScores() {
        scoresScreen.refresh();
        showScreen("SCORES", scoresScreen);
    }

    public void startGame(BoardSizeOption size) {
        if (gameScreen != null) root.remove(gameScreen);
        gameScreen = new GameScreen(this, size);
        root.add(gameScreen, "GAME");
        showScreen("GAME", gameScreen);
    }

    public void quit() {
        if (current != null) current.onHide();
        sound.shutdown();
        frame.dispose();
        System.exit(0);
    }

    public SoundEngine sound() { return sound; }

    public List<HighScoreEntry> scoreEntries() { return scores.getHighScores(); }

    // ----- GameHost -----

    @Override public void onBackToMenu() { showMenu(); }

    @Override public void onSubmitScore(String name, int score) {
        scores.addScore(name, score);
        showScores();
    }

    @Override public void onGameFinishedNoScore() { showScores(); }

    @Override public boolean qualifies(int score) { return scores.qualifies(score); }

    @Override public int highScore() { return scores.topScore(); }

    public static void launch() {
        SwingUtilities.invokeLater(() -> new App().start());
    }
}
