package game;

import assets.Fonts;
import assets.Palette;
import assets.SpritePainter;
import characters.Direction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Draws the top status bar: score, high score, level, lives, and the fright timer. */
final class Hud {
    private Hud() {}

    static void draw(Graphics2D g2, GameModel model, GamePanel panel) {
        int w = panel.getWidth();
        float big = 20f, small = 12f;

        SpritePainter.neonText(g2, "SCORE", Fonts.hud(small), 18, 18,
                Palette.TEXT_DIM, Palette.TEXT_DIM, false);
        SpritePainter.neonText(g2, pad(model.score()), Fonts.arcade(big), 18, 40,
                Palette.TEXT_PRIMARY, Palette.TEXT_GLOW, false);

        SpritePainter.neonText(g2, "HIGH", Fonts.hud(small), w / 2.0, 18,
                Palette.GOLD, Palette.GOLD, true);
        SpritePainter.neonText(g2, pad(model.highScore()), Fonts.arcade(big), w / 2.0, 40,
                Palette.GOLD, Palette.glow(Palette.GOLD, 0.6f), true);

        String lvl = "LEVEL " + model.level();
        g2.setFont(Fonts.hud(small));
        int lvlW = g2.getFontMetrics().stringWidth(lvl);
        SpritePainter.neonText(g2, lvl, Fonts.hud(small), w - 18 - lvlW, 18,
                Palette.TEXT_DIM, Palette.TEXT_DIM, false);

        // Lives as mini Pac-Men (top-right under the level label).
        int shown = Math.min(5, Math.max(0, model.lives() - 1));
        double r = 8;
        for (int i = 0; i < shown; i++) {
            double cx = w - 22 - i * 24;
            SpritePainter.pacman(g2, cx, 40, r, Math.toRadians(180), 0.55);
        }

        // Fright timer bar (bottom-center of HUD).
        if (model.frightActive() && model.cfg().frightSeconds() > 0) {
            double frac = Math.max(0, model.frightTimer() / model.cfg().frightSeconds());
            double bw = Math.min(220, w * 0.3), bh = 6;
            double bx = w / 2.0 - bw / 2, by = GamePanel.HUD_PX - 8;
            g2.setColor(Palette.glow(Palette.TEXT_DIM, 0.4f));
            g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh));
            Color fill = model.frightFlashing() ? Palette.FRIGHT_FLASH : Palette.ACCENT_CYAN;
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(bx, by, bw * frac, bh, bh, bh));
        }
    }

    private static String pad(int v) {
        String s = Integer.toString(v);
        while (s.length() < 5) s = "0" + s;
        return s;
    }
}
