package game;

import assets.Fonts;
import assets.Palette;
import assets.SpritePainter;

import java.awt.Color;
import java.awt.Graphics2D;

/** Draws READY!, PAUSED, GAME OVER (+ name entry), and the level-clear flash. */
final class Overlays {
    private Overlays() {}

    static void draw(Graphics2D g2, GameModel model, GameScreen screen, GamePanel panel) {
        int cols = model.board().cols(), rows = model.board().rows();
        float t = panel.tileSize();
        double cx = panel.offX() + cols * t / 2.0;
        double cy = panel.offY() + rows * t / 2.0;
        double w = panel.getWidth();

        switch (model.phase()) {
            case READY:
                pulseText(g2, "READY!", cx, cy + rows * t * 0.18, model.timeMs(),
                        Palette.READY_YELLOW, panel.tileSize() * 1.2f);
                break;
            case LEVEL_CLEAR: {
                double a = 0.25 + 0.25 * Math.sin(model.timeMs() / 1000.0 * 14);
                g2.setColor(Palette.glow(Color.WHITE, (float) a));
                g2.fillRect(panel.offX(), panel.offY(), (int) (cols * t), (int) (rows * t));
                SpritePainter.neonText(g2, "LEVEL CLEAR", Fonts.arcade(panel.tileSize() * 1.0f),
                        cx, cy, Palette.ACCENT_CYAN, Palette.ACCENT_CYAN, true);
                break;
            }
            case PAUSED:
                scrim(g2, panel);
                SpritePainter.neonText(g2, "PAUSED", Fonts.arcade(panel.tileSize() * 1.6f),
                        cx, cy, Palette.TEXT_PRIMARY, Palette.TEXT_GLOW, true);
                SpritePainter.neonText(g2, "P resume   Q menu   M mute",
                        Fonts.hud(panel.tileSize() * 0.6f), cx, cy + t * 1.6,
                        Palette.TEXT_DIM, Palette.TEXT_DIM, true);
                break;
            case GAME_OVER:
                drawGameOver(g2, model, screen, panel, cx, cy, w);
                break;
            default:
                break;
        }

        if (screen.isMuted()) {
            SpritePainter.neonText(g2, "MUTED", Fonts.hud(12f), w - 60, panel.getHeight() - 14,
                    Palette.TEXT_DIM, Palette.TEXT_DIM, false);
        }
    }

    private static void drawGameOver(Graphics2D g2, GameModel model, GameScreen screen,
                                     GamePanel panel, double cx, double cy, double w) {
        scrim(g2, panel);
        float t = panel.tileSize();
        SpritePainter.neonText(g2, "GAME OVER", Fonts.arcade(t * 1.7f), cx, cy - t * 1.5,
                Palette.GAMEOVER_RED, Palette.glow(Palette.GAMEOVER_RED, 0.6f), true);
        SpritePainter.neonText(g2, "SCORE  " + model.score(), Fonts.arcade(t * 0.8f),
                cx, cy, Palette.TEXT_PRIMARY, Palette.TEXT_GLOW, true);

        if (screen.isAwaitingName()) {
            SpritePainter.neonText(g2, "NEW HIGH SCORE!", Fonts.hud(t * 0.65f), cx, cy + t * 1.4,
                    Palette.GOLD, Palette.glow(Palette.GOLD, 0.6f), true);
            boolean caret = ((int) (model.timeMs() / 400)) % 2 == 0;
            String entry = screen.nameBuffer() + (caret ? "_" : " ");
            SpritePainter.neonText(g2, "ENTER NAME: " + entry, Fonts.arcade(t * 0.8f),
                    cx, cy + t * 2.6, Palette.ACCENT_MAGENTA, Palette.glow(Palette.ACCENT_MAGENTA, 0.5f), true);
            SpritePainter.neonText(g2, "type, then ENTER", Fonts.hud(t * 0.55f),
                    cx, cy + t * 3.5, Palette.TEXT_DIM, Palette.TEXT_DIM, true);
        } else {
            SpritePainter.neonText(g2, "ENTER  continue", Fonts.hud(t * 0.6f), cx, cy + t * 1.6,
                    Palette.TEXT_DIM, Palette.TEXT_DIM, true);
        }
    }

    private static void pulseText(Graphics2D g2, String text, double x, double y, double timeMs,
                                  Color color, float size) {
        double s = size * (1.0 + 0.05 * Math.sin(timeMs / 1000.0 * 4 * Math.PI));
        SpritePainter.neonText(g2, text, Fonts.arcade((float) s), x, y, color,
                Palette.glow(color, 0.6f), true);
    }

    private static void scrim(Graphics2D g2, GamePanel panel) {
        g2.setColor(Palette.glow(Palette.BG_DEEP, 0.62f));
        g2.fillRect(0, 0, panel.getWidth(), panel.getHeight());
    }
}
