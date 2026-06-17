package assets;

import game.FruitType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Pure static Graphics2D recipes for every game sprite. All drawing is vector,
 * antialiased (the caller sets hints), and scales with the supplied radius — no
 * image assets anywhere.
 */
public final class SpritePainter {
    private SpritePainter() {}

    // ------------------------------------------------------------- Pac-Man

    public static void pacman(Graphics2D g2, double cx, double cy, double r,
                              double facingRad, double mouthOpen) {
        // Glow halo
        radial(g2, cx, cy, r * 1.85,
                new float[] {0f, 0.55f, 1f},
                new Color[] {Palette.glow(Palette.PAC_GLOW, 0.55f),
                        Palette.glow(Palette.PAC_GLOW, 0.16f),
                        Palette.glow(Palette.PAC_GLOW, 0f)},
                cx - r * 1.85, cy - r * 1.85, r * 3.7);

        double mouthDeg = Math.max(4, mouthOpen * 60.0);
        java.awt.geom.AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(facingRad);
        Arc2D pie = new Arc2D.Double(-r, -r, 2 * r, 2 * r,
                mouthDeg / 2.0, 360.0 - mouthDeg, Arc2D.PIE);
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(-r * 0.3, -r * 0.3), (float) (r * 1.3),
                new float[] {0f, 1f}, new Color[] {Palette.PAC_HI, Palette.PAC}));
        g2.fill(pie);
        // Eye
        g2.setColor(new Color(20, 18, 4));
        double er = r * 0.16;
        g2.fill(new Ellipse2D.Double(-r * 0.05, -r * 0.5, er * 2, er * 2));
        g2.setTransform(old);
    }

    /** Dying Pac-Man: mouth opens past 180° while shrinking. progress 0..1. */
    public static void pacmanDeath(Graphics2D g2, double cx, double cy, double r,
                                   double facingRad, double progress) {
        double scale = 1.0 - 0.15 * progress;
        double mouthDeg = Math.min(360, 30 + progress * 360);
        java.awt.geom.AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(facingRad + progress * Math.PI * 0.6);
        g2.scale(scale, scale);
        if (mouthDeg < 360) {
            Arc2D pie = new Arc2D.Double(-r, -r, 2 * r, 2 * r,
                    mouthDeg / 2.0, 360.0 - mouthDeg, Arc2D.PIE);
            g2.setColor(Palette.glow(Palette.PAC, (float) (1 - progress)));
            g2.fill(pie);
        }
        g2.setTransform(old);
    }

    // -------------------------------------------------------------- Ghosts

    public static void ghost(Graphics2D g2, double cx, double cy, double r,
                             Color body, Color glow, double dirX, double dirY,
                             double skirtPhase) {
        radial(g2, cx, cy, r * 1.7,
                new float[] {0f, 0.6f, 1f},
                new Color[] {Palette.glow(glow, 0.5f), Palette.glow(glow, 0.14f), Palette.glow(glow, 0f)},
                cx - r * 1.7, cy - r * 1.7, r * 3.4);

        Path2D p = ghostBody(cx, cy, r, skirtPhase);
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy - r * 0.3), (float) (r * 1.4),
                new float[] {0f, 1f}, new Color[] {Palette.lighten(body, 0.28f), body}));
        g2.fill(p);
        eyes(g2, cx, cy, r, dirX, dirY, Palette.EYE_WHITE, Palette.PUPIL);
    }

    public static void ghostFrightened(Graphics2D g2, double cx, double cy, double r,
                                        boolean flashWhite, double skirtPhase) {
        Color body = flashWhite ? Palette.FRIGHT_FLASH : Palette.FRIGHT_BODY;
        Color face = flashWhite ? Palette.FRIGHT_FLASH_FACE : Palette.FRIGHT_FACE;
        radial(g2, cx, cy, r * 1.7,
                new float[] {0f, 0.6f, 1f},
                new Color[] {Palette.glow(Palette.FRIGHT_GLOW, 0.45f),
                        Palette.glow(Palette.FRIGHT_GLOW, 0.12f), Palette.glow(Palette.FRIGHT_GLOW, 0f)},
                cx - r * 1.7, cy - r * 1.7, r * 3.4);

        Path2D p = ghostBody(cx, cy, r, skirtPhase);
        g2.setColor(body);
        g2.fill(p);

        // two dot eyes
        g2.setColor(face);
        double er = r * 0.12;
        g2.fill(new Ellipse2D.Double(cx - r * 0.32 - er, cy - r * 0.18 - er, er * 2, er * 2));
        g2.fill(new Ellipse2D.Double(cx + r * 0.32 - er, cy - r * 0.18 - er, er * 2, er * 2));

        // zig-zag mouth
        g2.setStroke(new BasicStroke((float) (r * 0.16), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D mouth = new Path2D.Double();
        double x0 = cx - r * 0.5, x1 = cx + r * 0.5, yTop = cy + r * 0.18, yBot = cy + r * 0.36;
        int teeth = 4;
        mouth.moveTo(x0, yBot);
        for (int i = 1; i <= teeth * 2; i++) {
            double x = x0 + (x1 - x0) * i / (teeth * 2.0);
            mouth.lineTo(x, (i % 2 == 1) ? yTop : yBot);
        }
        g2.draw(mouth);
    }

    public static void ghostEyes(Graphics2D g2, double cx, double cy, double r,
                                 double dirX, double dirY) {
        radial(g2, cx, cy, r * 1.1,
                new float[] {0f, 1f},
                new Color[] {Palette.glow(Palette.EYE_WHITE, 0.2f), Palette.glow(Palette.EYE_WHITE, 0f)},
                cx - r * 1.1, cy - r * 1.1, r * 2.2);
        eyes(g2, cx, cy, r, dirX, dirY, Palette.EYE_WHITE, Palette.PUPIL);
    }

    private static Path2D ghostBody(double cx, double cy, double r, double skirtPhase) {
        double left = cx - r, right = cx + r, top = cy - r;
        double skirtBase = cy + r * 0.62;
        Path2D p = new Path2D.Double();
        p.moveTo(left, cy);
        p.append(new Arc2D.Double(left, top, 2 * r, 2 * r, 180, -180, Arc2D.OPEN), true);
        p.lineTo(right, skirtBase);
        int bumps = 4;
        double segW = (right - left) / bumps;
        for (int i = 0; i < bumps; i++) {
            double x2 = right - (i + 1) * segW;
            double ctrlX = right - (i + 0.5) * segW;
            double wob = Math.sin(skirtPhase + i) * r * 0.06;
            p.quadTo(ctrlX, skirtBase + r * 0.26 + wob, x2, skirtBase);
        }
        p.closePath();
        return p;
    }

    private static void eyes(Graphics2D g2, double cx, double cy, double r,
                             double dirX, double dirY, Color white, Color pupil) {
        double ew = r * 0.26, eh = r * 0.32;
        double ox = r * 0.30, oy = r * 0.16;
        for (int s = -1; s <= 1; s += 2) {
            double ecx = cx + s * ox, ecy = cy - oy;
            g2.setColor(white);
            g2.fill(new Ellipse2D.Double(ecx - ew, ecy - eh, ew * 2, eh * 2));
            g2.setColor(pupil);
            double pr = r * 0.13;
            g2.fill(new Ellipse2D.Double(ecx + dirX * r * 0.1 - pr,
                    ecy + dirY * r * 0.12 - pr, pr * 2, pr * 2));
        }
    }

    // ------------------------------------------------------------- Pellets

    public static void pellet(Graphics2D g2, double cx, double cy, double tile) {
        // Cheap two-circle glow (drawn ~hundreds of times per frame — no gradient).
        double halo = tile * 0.18, core = tile * 0.11;
        g2.setColor(Palette.glow(Palette.PELLET_GLOW, 0.28f));
        g2.fill(new Ellipse2D.Double(cx - halo, cy - halo, halo * 2, halo * 2));
        g2.setColor(Palette.PELLET);
        g2.fill(new Ellipse2D.Double(cx - core, cy - core, core * 2, core * 2));
    }

    public static void powerPellet(Graphics2D g2, double cx, double cy, double tile, double pulse) {
        double core = tile * 0.26 * (1 + 0.18 * pulse);
        radial(g2, cx, cy, tile * 0.6,
                new float[] {0f, 1f},
                new Color[] {Palette.glow(Palette.POWER_GLOW, 0.7f), Palette.glow(Palette.POWER_GLOW, 0f)},
                cx - tile * 0.6, cy - tile * 0.6, tile * 1.2);
        g2.setColor(Palette.POWER_PELLET);
        g2.fill(new Ellipse2D.Double(cx - core, cy - core, core * 2, core * 2));
    }

    // --------------------------------------------------------------- Fruit

    public static void fruit(Graphics2D g2, double cx, double cy, double r, FruitType type) {
        radial(g2, cx, cy, r * 1.6,
                new float[] {0f, 1f},
                new Color[] {Palette.glow(Palette.FRUIT_POP, 0.4f), Palette.glow(Palette.FRUIT_POP, 0f)},
                cx - r * 1.6, cy - r * 1.6, r * 3.2);
        Color leaf = new Color(60, 200, 90);
        switch (type) {
            case CHERRY: {
                Color red = new Color(255, 60, 70);
                disc(g2, cx - r * 0.4, cy + r * 0.4, r * 0.55, red);
                disc(g2, cx + r * 0.4, cy + r * 0.45, r * 0.5, red);
                stem(g2, cx - r * 0.4, cy + r * 0.4, cx + r * 0.2, cy - r * 0.7, leaf);
                stem(g2, cx + r * 0.4, cy + r * 0.45, cx + r * 0.2, cy - r * 0.7, leaf);
                break;
            }
            case STRAWBERRY: {
                Color red = new Color(255, 70, 90);
                Path2D berry = new Path2D.Double();
                berry.moveTo(cx, cy + r * 0.9);
                berry.curveTo(cx - r, cy + r * 0.2, cx - r * 0.7, cy - r * 0.6, cx, cy - r * 0.4);
                berry.curveTo(cx + r * 0.7, cy - r * 0.6, cx + r, cy + r * 0.2, cx, cy + r * 0.9);
                g2.setColor(red);
                g2.fill(berry);
                g2.setColor(leaf.brighter());
                g2.fill(new Ellipse2D.Double(cx - r * 0.5, cy - r * 0.7, r, r * 0.5));
                g2.setColor(new Color(255, 220, 120));
                for (int i = 0; i < 5; i++) {
                    double sx = cx + (i - 2) * r * 0.28, sy = cy + r * 0.1 + (i % 2) * r * 0.2;
                    g2.fill(new Ellipse2D.Double(sx - r * 0.05, sy - r * 0.08, r * 0.1, r * 0.16));
                }
                break;
            }
            case ORANGE:
            case APPLE: {
                Color c = type == FruitType.ORANGE ? new Color(255, 160, 40) : new Color(255, 70, 80);
                disc(g2, cx, cy + r * 0.1, r * 0.85, c);
                g2.setColor(Palette.glow(Color.WHITE, 0.35f));
                g2.fill(new Ellipse2D.Double(cx - r * 0.5, cy - r * 0.45, r * 0.4, r * 0.4));
                stem(g2, cx, cy - r * 0.6, cx + r * 0.15, cy - r, new Color(120, 80, 40));
                g2.setColor(leaf);
                g2.fill(new Ellipse2D.Double(cx + r * 0.1, cy - r, r * 0.6, r * 0.3));
                break;
            }
            case MELON: {
                disc(g2, cx, cy + r * 0.05, r * 0.85, new Color(110, 220, 110));
                g2.setColor(new Color(40, 140, 60));
                g2.setStroke(new BasicStroke((float) (r * 0.08)));
                for (int i = -2; i <= 2; i++) {
                    g2.draw(new java.awt.geom.Line2D.Double(cx + i * r * 0.3, cy - r * 0.7,
                            cx + i * r * 0.5, cy + r * 0.8));
                }
                break;
            }
            case GALAXIAN: {
                Color c = new Color(120, 200, 255);
                Path2D ship = new Path2D.Double();
                ship.moveTo(cx, cy - r * 0.9);
                ship.lineTo(cx + r * 0.8, cy + r * 0.7);
                ship.lineTo(cx, cy + r * 0.3);
                ship.lineTo(cx - r * 0.8, cy + r * 0.7);
                ship.closePath();
                g2.setColor(c);
                g2.fill(ship);
                g2.setColor(new Color(255, 230, 120));
                g2.fill(new Ellipse2D.Double(cx - r * 0.18, cy - r * 0.2, r * 0.36, r * 0.36));
                break;
            }
            case BELL: {
                Color c = new Color(255, 210, 90);
                Path2D bell = new Path2D.Double();
                bell.moveTo(cx - r * 0.8, cy + r * 0.6);
                bell.curveTo(cx - r * 0.8, cy - r * 0.7, cx + r * 0.8, cy - r * 0.7, cx + r * 0.8, cy + r * 0.6);
                bell.closePath();
                g2.setColor(c);
                g2.fill(bell);
                g2.setColor(c.darker());
                g2.fill(new Ellipse2D.Double(cx - r * 0.18, cy + r * 0.55, r * 0.36, r * 0.36));
                break;
            }
            case KEY:
            default: {
                Color c = new Color(180, 220, 255);
                g2.setColor(c);
                g2.setStroke(new BasicStroke((float) (r * 0.22), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new java.awt.geom.Line2D.Double(cx, cy - r * 0.5, cx, cy + r * 0.8));
                g2.draw(new Ellipse2D.Double(cx - r * 0.4, cy - r * 0.9, r * 0.8, r * 0.7));
                g2.draw(new java.awt.geom.Line2D.Double(cx, cy + r * 0.5, cx + r * 0.35, cy + r * 0.5));
                break;
            }
        }
    }

    // ------------------------------------------------------------- text

    /** Draws neon text with a soft glow. Anchor (x,y) is the baseline start unless centered. */
    public static void neonText(Graphics2D g2, String text, Font font, double x, double y,
                                Color color, Color glow, boolean centered) {
        g2.setFont(font);
        java.awt.FontMetrics fm = g2.getFontMetrics();
        double drawX = centered ? x - fm.stringWidth(text) / 2.0 : x;
        for (int i = 3; i >= 1; i--) {
            g2.setColor(Palette.glow(glow, 0.14f));
            g2.setStroke(new BasicStroke(i * 2f));
            g2.drawString(text, (int) Math.round(drawX), (int) Math.round(y));
        }
        g2.setColor(color);
        g2.drawString(text, (int) Math.round(drawX), (int) Math.round(y));
    }

    // ------------------------------------------------------------- helpers

    private static void disc(Graphics2D g2, double cx, double cy, double r, Color c) {
        g2.setColor(c);
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void stem(Graphics2D g2, double x1, double y1, double x2, double y2, Color c) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
    }

    private static void radial(Graphics2D g2, double cx, double cy, double radius,
                               float[] fracs, Color[] colors,
                               double boxX, double boxY, double boxSize) {
        if (radius <= 0) return;
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) radius, fracs, colors));
        g2.fill(new Ellipse2D.Double(boxX, boxY, boxSize, boxSize));
    }
}
