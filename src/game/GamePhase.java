package game;

/** High-level state of the game screen. */
public enum GamePhase {
    READY,        // "READY!" countdown before play
    PLAYING,      // normal play
    DYING,        // Pac-Man death animation
    LEVEL_CLEAR,  // maze flash / level transition
    GAME_OVER,    // out of lives; awaiting dismissal / name entry
    PAUSED        // frozen by the player
}
