package characters;

/** Behavioural mode of a ghost. Frightened/eaten layer over the global scatter/chase. */
public enum GhostMode {
    IN_HOUSE,    // bobbing in the pen awaiting release
    SCATTER,     // heading for its home corner
    CHASE,       // pursuing its personality target
    FRIGHTENED,  // fleeing, edible
    EATEN;       // eyes returning to the house

    public boolean isHostile() {
        return this == SCATTER || this == CHASE;
    }

    public boolean leavingOrEaten() {
        return this == EATEN || this == IN_HOUSE;
    }
}
