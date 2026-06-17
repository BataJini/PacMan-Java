package menu;

import assets.Fonts;
import assets.Palette;
import assets.SpritePainter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** A neon pill button drawn procedurally; layout + hit-testing owned by the menu. */
final class MenuButton {
    String label = "";
    boolean selected;
    double x, y, w, h;

    void setBounds(double x, double y, double w, double h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    boolean contains(int px, int py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    void draw(Graphics2D g2, double timeMs) {
        RoundRectangle2D pill = new RoundRectangle2D.Double(x, y, w, h, h, h);
        if (selected) {
            double pulse = 0.5 + 0.5 * Math.sin(timeMs / 1000.0 * 4);
            g2.setColor(Palette.glow(Palette.ACCENT_CYAN, (float) (0.12 + 0.10 * pulse)));
            g2.fill(pill);
            g2.setColor(Palette.ACCENT_MAGENTA);
            g2.setStroke(new BasicStroke(2.4f));
        } else {
            g2.setColor(Palette.glow(Palette.WALL_CORE, 0.5f));
            g2.setStroke(new BasicStroke(1.6f));
        }
        g2.draw(pill);

        Color text = selected ? Palette.TEXT_PRIMARY : Palette.TEXT_DIM;
        Color glow = selected ? Palette.ACCENT_CYAN : Palette.TEXT_DIM;
        g2.setFont(Fonts.arcade((float) (h * 0.42)));
        int tw = g2.getFontMetrics().stringWidth(label);
        double baseline = y + h / 2 + g2.getFontMetrics().getAscent() / 2.0 - 2;
        if (selected) {
            double tx = x + h * 0.55, ty = y + h / 2, ts = h * 0.16;
            java.awt.geom.Path2D tri = new java.awt.geom.Path2D.Double();
            tri.moveTo(tx - ts, ty - ts);
            tri.lineTo(tx + ts, ty);
            tri.lineTo(tx - ts, ty + ts);
            tri.closePath();
            g2.setColor(Palette.ACCENT_MAGENTA);
            g2.fill(tri);
        }
        SpritePainter.neonText(g2, label, g2.getFont(), x + w / 2 - tw / 2.0, baseline, text, glow, false);
    }
}
