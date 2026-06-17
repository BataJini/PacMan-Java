package characters;

import assets.Palette;

import java.awt.Color;

/**
 * The four classic ghost identities. Holds only colours and a stable name;
 * targeting math lives in the game model (which owns the board and Blinky).
 */
public enum GhostPersonality {
    BLINKY("BLINKY", Palette.GHOST_RED, Palette.GHOST_RED_GLOW),
    PINKY("PINKY", Palette.GHOST_PINK, Palette.GHOST_PINK_GLOW),
    INKY("INKY", Palette.GHOST_CYAN, Palette.GHOST_CYAN_GLOW),
    CLYDE("CLYDE", Palette.GHOST_ORANGE, Palette.GHOST_ORANGE_GLOW);

    private final String displayName;
    private final Color body;
    private final Color glow;

    GhostPersonality(String displayName, Color body, Color glow) {
        this.displayName = displayName;
        this.body = body;
        this.glow = glow;
    }

    public String displayName() { return displayName; }
    public Color body() { return body; }
    public Color glow() { return glow; }
}
