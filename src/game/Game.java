package game;

import assets.SoundEngine;

/** Small factory replacing the old JFrame-based Game; builds a fresh model. */
public final class Game {
    private Game() {}

    public static GameModel newModel(BoardSizeOption size, SoundEngine sound, int highScore) {
        return new GameModel(size, sound, highScore);
    }
}
