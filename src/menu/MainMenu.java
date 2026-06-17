package menu;

import assets.Cue;
import assets.Fonts;
import assets.Palette;
import assets.SpritePainter;
import game.BoardSizeOption;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/** Animated neon main menu with an attract "chase" motif and custom buttons. */
public final class MainMenu extends JPanel implements Screen {
    private static final int PLAY = 0, BOARD = 1, SCORES = 2, MUTE = 3, QUIT = 4;

    private final App app;
    private final Timer anim;
    private final MenuButton[] buttons = new MenuButton[5];
    private int selected = PLAY;
    private BoardSizeOption board = BoardSizeOption.MEDIUM;
    private double timeMs;

    public MainMenu(App app) {
        this.app = app;
        setOpaque(true);
        setBackground(Palette.BG_DEEP);
        setPreferredSize(new Dimension(720, 780));
        setFocusable(true);
        for (int i = 0; i < buttons.length; i++) buttons[i] = new MenuButton();
        anim = new Timer(16, e -> { timeMs += 16; repaint(); });
        addKeyListener(new Keys());
        addMouseListener(new Mouse());
        addMouseMotionListener(new Mouse());
    }

    @Override public void onShow() { anim.start(); requestFocusInWindow(); }
    @Override public void onHide() { anim.stop(); }

    // ------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // background
        g2.setColor(Palette.BG_DEEP);
        g2.fillRect(0, 0, w, h);
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.4f),
                Math.max(w, h) * 0.7f, new float[] {0f, 1f},
                new Color[] {Palette.BG_VIGNETTE, Palette.BG_DEEP}));
        g2.fillRect(0, 0, w, h);

        // title
        double bob = Math.sin(timeMs / 1000.0 * 1.6) * 5;
        float titleSize = Math.min(96, w * 0.16f);
        SpritePainter.neonText(g2, "PAC", Fonts.arcade(titleSize), w / 2.0 - titleSize * 1.55,
                h * 0.2 + bob, Palette.PAC, Palette.PAC_GLOW, true);
        SpritePainter.neonText(g2, "MAN", Fonts.arcade(titleSize), w / 2.0 + titleSize * 1.55,
                h * 0.2 + bob, Palette.ACCENT_CYAN, Palette.ACCENT_CYAN, true);
        SpritePainter.neonText(g2, "N E O N   A R C A D E", Fonts.hud(16f),
                w / 2.0, h * 0.2 + titleSize * 0.7 + bob, Palette.TEXT_DIM, Palette.TEXT_DIM, true);

        drawAttract(g2, w, h);
        layoutButtons(w, h);
        for (MenuButton b : buttons) b.draw(g2, timeMs);

        SpritePainter.neonText(g2, "↑↓ select    ←→ board size    ENTER play    M mute",
                Fonts.hud(13f), w / 2.0, h - 22, Palette.TEXT_DIM, Palette.TEXT_DIM, true);
        g2.dispose();
    }

    private void drawAttract(Graphics2D g2, int w, int h) {
        double laneY = h * 0.40;
        double t = (timeMs / 1000.0 % 7.0) / 7.0;
        double pacX = -60 + t * (w + 120);
        double r = 15;
        // pellets along the lane, eaten as Pac passes
        g2.setColor(Palette.PELLET);
        for (int x = 40; x < w - 20; x += 34) {
            if (x > pacX + 10) SpritePainter.pellet(g2, x, laneY, 28);
        }
        double mouth = (Math.sin(timeMs / 1000.0 * 12) + 1) / 2;
        SpritePainter.pacman(g2, pacX, laneY, r, 0, mouth);
        Color[] cols = {Palette.GHOST_RED, Palette.GHOST_PINK, Palette.GHOST_CYAN, Palette.GHOST_ORANGE};
        Color[] glows = {Palette.GHOST_RED_GLOW, Palette.GHOST_PINK_GLOW,
                Palette.GHOST_CYAN_GLOW, Palette.GHOST_ORANGE_GLOW};
        for (int i = 0; i < 4; i++) {
            double gx = pacX - (i + 1) * 42;
            SpritePainter.ghost(g2, gx, laneY, r, cols[i], glows[i], 1, 0, timeMs / 1000.0 * 8);
        }
    }

    private void layoutButtons(int w, int h) {
        String[] labels = {
                "PLAY",
                "BOARD:  " + board.label(),
                "HIGH SCORES",
                "SOUND:  " + (app.sound().isMuted() ? "OFF" : "ON"),
                "QUIT"};
        double bw = Math.min(360, w * 0.6), bh = 50, gap = 16;
        double total = labels.length * bh + (labels.length - 1) * gap;
        double startY = h * 0.52;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].label = labels[i];
            buttons[i].selected = (i == selected);
            buttons[i].setBounds(w / 2.0 - bw / 2, startY + i * (bh + gap), bw, bh);
        }
    }

    // ------------------------------------------------------------ actions

    private void move(int delta) {
        selected = (selected + delta + buttons.length) % buttons.length;
        app.sound().play(Cue.MENU_MOVE);
        repaint();
    }

    private void cycleBoard(int dir) {
        BoardSizeOption[] all = BoardSizeOption.values();
        board = all[(board.ordinal() + dir + all.length) % all.length];
        app.sound().play(Cue.MENU_MOVE);
        repaint();
    }

    private void activate(int index) {
        app.sound().play(Cue.MENU_SELECT);
        switch (index) {
            case PLAY: app.startGame(board); break;
            case BOARD: cycleBoard(1); break;
            case SCORES: app.showScores(); break;
            case MUTE: app.sound().setMuted(!app.sound().isMuted()); repaint(); break;
            case QUIT: app.quit(); break;
            default: break;
        }
    }

    private final class Keys extends KeyAdapter {
        @Override public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP: case KeyEvent.VK_W: move(-1); break;
                case KeyEvent.VK_DOWN: case KeyEvent.VK_S: move(1); break;
                case KeyEvent.VK_LEFT: case KeyEvent.VK_A:
                    if (selected == BOARD) cycleBoard(-1); break;
                case KeyEvent.VK_RIGHT: case KeyEvent.VK_D:
                    if (selected == BOARD) cycleBoard(1); break;
                case KeyEvent.VK_ENTER: case KeyEvent.VK_SPACE: activate(selected); break;
                case KeyEvent.VK_M: app.sound().setMuted(!app.sound().isMuted()); repaint(); break;
                case KeyEvent.VK_ESCAPE: case KeyEvent.VK_Q: app.quit(); break;
                default: break;
            }
        }
    }

    private final class Mouse extends MouseAdapter {
        @Override public void mouseMoved(MouseEvent e) {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].contains(e.getX(), e.getY()) && selected != i) {
                    selected = i;
                    repaint();
                    return;
                }
            }
        }
        @Override public void mouseClicked(MouseEvent e) {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].contains(e.getX(), e.getY())) { activate(i); return; }
            }
        }
    }
}
