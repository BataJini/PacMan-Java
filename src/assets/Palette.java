package assets;

import java.awt.Color;

/**
 * Central neon palette. All colors defined once; helpers for glow/lerp/hue-shift
 * support the per-level maze hue rotation and procedural glow layering.
 */
public final class Palette {
    private Palette() {}

    // Background / playfield
    public static final Color BG_DEEP        = new Color(5, 6, 14);
    public static final Color BG_VIGNETTE    = new Color(10, 11, 26);
    public static final Color BG_GRID_LINE   = new Color(18, 22, 52);
    public static final Color PLAYFIELD_INK  = new Color(3, 4, 10);

    // Maze (Level-1 blue; rotated per level via hueShift)
    public static final Color WALL_CORE      = new Color(61, 107, 255);
    public static final Color WALL_EDGE      = new Color(27, 46, 140);
    public static final Color WALL_GLOW      = new Color(77, 123, 255);
    public static final Color WALL_GLOW_SOFT = new Color(110, 146, 255);
    public static final Color GATE           = new Color(255, 184, 255);

    // Pellets
    public static final Color PELLET         = new Color(255, 233, 176);
    public static final Color PELLET_GLOW    = new Color(255, 210, 122);
    public static final Color POWER_PELLET   = new Color(255, 255, 255);
    public static final Color POWER_GLOW     = new Color(255, 224, 138);

    // Pac-Man
    public static final Color PAC            = new Color(255, 226, 31);
    public static final Color PAC_HI         = new Color(255, 247, 168);
    public static final Color PAC_GLOW       = new Color(255, 212, 0);

    // Ghosts
    public static final Color GHOST_RED          = new Color(255, 43, 78);
    public static final Color GHOST_RED_GLOW     = new Color(255, 107, 133);
    public static final Color GHOST_PINK         = new Color(255, 138, 208);
    public static final Color GHOST_PINK_GLOW    = new Color(255, 182, 230);
    public static final Color GHOST_CYAN         = new Color(34, 224, 255);
    public static final Color GHOST_CYAN_GLOW    = new Color(138, 242, 255);
    public static final Color GHOST_ORANGE       = new Color(255, 166, 43);
    public static final Color GHOST_ORANGE_GLOW  = new Color(255, 200, 120);

    public static final Color EYE_WHITE      = new Color(244, 248, 255);
    public static final Color PUPIL          = new Color(26, 31, 102);

    // Frightened
    public static final Color FRIGHT_BODY        = new Color(35, 48, 255);
    public static final Color FRIGHT_GLOW        = new Color(106, 120, 255);
    public static final Color FRIGHT_FACE        = new Color(255, 214, 224);
    public static final Color FRIGHT_FLASH       = new Color(255, 255, 255);
    public static final Color FRIGHT_FLASH_FACE  = new Color(255, 43, 78);

    // Text / accents
    public static final Color TEXT_PRIMARY   = new Color(234, 240, 255);
    public static final Color TEXT_DIM       = new Color(124, 134, 184);
    public static final Color TEXT_GLOW      = new Color(92, 123, 255);
    public static final Color ACCENT_CYAN    = new Color(25, 224, 200);
    public static final Color ACCENT_MAGENTA = new Color(255, 61, 208);
    public static final Color READY_YELLOW   = new Color(255, 226, 31);
    public static final Color GAMEOVER_RED   = new Color(255, 43, 78);

    public static final Color GOLD           = new Color(255, 210, 74);
    public static final Color SILVER         = new Color(207, 224, 255);
    public static final Color BRONZE         = new Color(224, 138, 74);
    public static final Color SCORE_POP      = new Color(156, 251, 255);
    public static final Color FRUIT_POP      = new Color(255, 107, 168);

    /** Per-level maze hue degrees (added to the L1 blue hue). */
    private static final float[] LEVEL_HUE_DEG = {0f, 78f, 168f, 222f};

    /** Returns the same color with the given alpha [0..255]. */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp255(a));
    }

    /** Returns the same color with the given alpha fraction [0..1]. */
    public static Color glow(Color c, float aFrac) {
        return alpha(c, Math.round(aFrac * 255f));
    }

    /** Linear blend between two colors (also blends alpha). */
    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    /** Lighten toward white by fraction t. */
    public static Color lighten(Color c, float t) {
        return lerp(c, Color.WHITE, t);
    }

    /** Rotate hue by the given degrees, preserving saturation/brightness/alpha. */
    public static Color hueShift(Color c, float deg) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float h = (hsb[0] + deg / 360f) % 1f;
        if (h < 0) h += 1f;
        Color shifted = Color.getHSBColor(h, hsb[1], hsb[2]);
        return new Color(shifted.getRed(), shifted.getGreen(), shifted.getBlue(), c.getAlpha());
    }

    /** Maze color rotated for the given (1-based) level. */
    public static Color mazeColor(Color base, int level) {
        float deg = LEVEL_HUE_DEG[(Math.max(1, level) - 1) % LEVEL_HUE_DEG.length];
        return deg == 0f ? base : hueShift(base, deg);
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
