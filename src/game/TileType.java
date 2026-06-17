package game;

/** Decodes the extended map codes produced by {@link Board}'s load-time transform. */
public enum TileType {
    EMPTY,        // 0 - blank / eaten dot
    WALL,         // 1 - solid wall (cached in maze image)
    DOT,          // 2 - small pellet (10)
    POWER,        // 3 - power pellet (50, frightens ghosts)
    HOUSE,        // 4 - ghost-house interior (ghosts pass, Pac blocked)
    GHOST_DOOR,   // 5 - ghost-house door bar
    TUNNEL,       // 6 - side-edge wrap tile
    FRUIT_SPOT,   // 7 - bonus spawn marker (walkable)
    PAC_SPAWN,    // 8 - Pac-Man start (walkable, no dot)
    GHOST_SPAWN;  // 9 - ghost start slot inside house

    public static TileType of(int code) {
        switch (code) {
            case 1: return WALL;
            case 2: return DOT;
            case 3: return POWER;
            case 4: return HOUSE;
            case 5: return GHOST_DOOR;
            case 6: return TUNNEL;
            case 7: return FRUIT_SPOT;
            case 8: return PAC_SPAWN;
            case 9: return GHOST_SPAWN;
            default: return EMPTY;
        }
    }

    public boolean blocksPacman() {
        return this == WALL || this == HOUSE || this == GHOST_DOOR || this == GHOST_SPAWN;
    }

    /** Walls block everyone; the door only opens for ghosts going home or leaving. */
    public boolean blocksGhost(boolean leavingOrEaten) {
        if (this == WALL) return true;
        if (this == GHOST_DOOR) return !leavingOrEaten;
        return false;
    }
}
