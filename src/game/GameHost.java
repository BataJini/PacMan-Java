package game;

import assets.SoundEngine;

/** Bridge the GameScreen uses to talk to the app router without importing menu. */
public interface GameHost {
    void onBackToMenu();
    void onSubmitScore(String name, int score);
    void onGameFinishedNoScore();
    boolean qualifies(int score);
    SoundEngine sound();
    int highScore();
}
