package menu;

import assets.Fonts;
import assets.Palette;
import assets.SpritePainter;
import highscores.HighScoreEntry;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/** Neon high-scores table with medal-coloured top three. */
public final class HighScoresScreen extends JPanel implements Screen {
    private final App app;
    private final Timer anim;
    private double timeMs;
    private List<HighScoreEntry> entries;

    public HighScoresScreen(App app) {
        this.app = app;
        setOpaque(true);
        setBackground(Palette.BG_DEEP);
        setPreferredSize(new Dimension(720, 780));
        setFocusable(true);
        anim = new Timer(33, e -> { timeMs += 33; repaint(); });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                app.showMenu();
            }
        });
    }

    public void refresh() { entries = app.scoreEntries(); }

    @Override public void onShow() { refresh(); anim.start(); requestFocusInWindow(); }
    @Override public void onHide() { anim.stop(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(Palette.BG_DEEP);
        g2.fillRect(0, 0, w, h);

        SpritePainter.neonText(g2, "HIGH SCORES", Fonts.arcade(Math.min(64, w * 0.10f)),
                w / 2.0, h * 0.16, Palette.GOLD, Palette.glow(Palette.GOLD, 0.6f), true);

        List<HighScoreEntry> list = entries;
        double y = h * 0.30, rowH = Math.min(46, (h * 0.5) / 10);
        g2.setFont(Fonts.hud(16f));
        if (list == null || list.isEmpty()) {
            SpritePainter.neonText(g2, "NO SCORES YET — GO PLAY!", Fonts.hud(20f),
                    w / 2.0, h * 0.45, Palette.TEXT_DIM, Palette.TEXT_DIM, true);
        } else {
            for (int i = 0; i < list.size() && i < 10; i++) {
                HighScoreEntry e = list.get(i);
                Color c = i == 0 ? Palette.GOLD : i == 1 ? Palette.SILVER
                        : i == 2 ? Palette.BRONZE : Palette.TEXT_PRIMARY;
                double ry = y + i * rowH;
                float fs = (float) (rowH * 0.5);
                SpritePainter.neonText(g2, String.format("%2d", i + 1), Fonts.arcade(fs),
                        w * 0.28, ry, c, c, false);
                SpritePainter.neonText(g2, trim(e.getName()), Fonts.arcade(fs),
                        w * 0.38, ry, c, Palette.glow(c, 0.4f), false);
                String sc = Integer.toString(e.getScore());
                g2.setFont(Fonts.arcade(fs));
                int sw = g2.getFontMetrics().stringWidth(sc);
                SpritePainter.neonText(g2, sc, Fonts.arcade(fs), w * 0.72 - sw, ry, c,
                        Palette.glow(c, 0.4f), false);
            }
        }

        boolean blink = ((int) (timeMs / 500)) % 2 == 0;
        if (blink) {
            SpritePainter.neonText(g2, "press any key to return", Fonts.hud(14f),
                    w / 2.0, h - 30, Palette.TEXT_DIM, Palette.TEXT_DIM, true);
        }
        g2.dispose();
    }

    private static String trim(String s) {
        if (s == null) return "----";
        return s.length() > 10 ? s.substring(0, 10) : s;
    }
}
