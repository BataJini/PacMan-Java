package game;

import assets.SoundEngine;
import menu.Screen;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

/**
 * CardLayout "GAME" panel. Owns the {@link GameModel}, {@link GameLoop} and
 * {@link GamePanel}, and drives the game-over / high-score-entry / restart flow.
 */
public final class GameScreen extends JPanel implements Screen {
    private final GameHost host;
    private final BoardSizeOption size;
    private final SoundEngine sound;

    private GameModel model;
    private GameLoop loop;
    private GamePanel panel;

    private boolean awaitingName;
    private boolean scoreHandled;
    private String nameBuffer = "";

    public GameScreen(GameHost host, BoardSizeOption size) {
        this.host = host;
        this.size = size;
        this.sound = host.sound();
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        model = Game.newModel(size, sound, host.highScore());
        panel = new GamePanel(model, this);
        loop = new GameLoop(model, panel);
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        loop.start();
        panel.requestFocusInWindow();
    }

    @Override
    public void onHide() {
        loop.stop();
        sound.stopSiren();
    }

    // ----- input callbacks from GamePanel -----

    public void toggleMute() { sound.setMuted(!sound.isMuted()); }
    public boolean isMuted() { return sound.isMuted(); }

    public void backToMenu() {
        loop.stop();
        sound.stopSiren();
        host.onBackToMenu();
    }

    public void restart() {
        loop.stop();
        sound.stopSiren();
        remove(panel);
        awaitingName = false;
        scoreHandled = false;
        nameBuffer = "";
        build();
        revalidate();
        repaint();
        loop.start();
        panel.requestFocusInWindow();
    }

    public void confirm() {
        if (model.phase() != GamePhase.GAME_OVER) return;
        if (awaitingName) { submitName(); return; }
        if (!scoreHandled && host.qualifies(model.score())) {
            awaitingName = true;
            nameBuffer = "";
            return;
        }
        scoreHandled = true;
        host.onGameFinishedNoScore();
    }

    public boolean isAwaitingName() { return awaitingName; }
    public String nameBuffer() { return nameBuffer; }

    public void handleNameKey(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_ENTER) { submitName(); return; }
        if (code == KeyEvent.VK_BACK_SPACE) {
            if (!nameBuffer.isEmpty()) nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
            return;
        }
        if (code == KeyEvent.VK_ESCAPE) { awaitingName = false; return; }
        char ch = e.getKeyChar();
        if ((Character.isLetterOrDigit(ch) || ch == ' ') && nameBuffer.length() < 10) {
            nameBuffer += Character.toUpperCase(ch);
        }
    }

    private void submitName() {
        String n = nameBuffer.trim();
        if (n.isEmpty()) n = "PLAYER";
        awaitingName = false;
        scoreHandled = true;
        host.onSubmitScore(n, model.score());
    }
}
