package highscores;

import java.io.Serializable;

/** Immutable high-score row. Field shape frozen for highscores.ser compatibility. */
public class HighScoreEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int score;

    public HighScoreEntry(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return name + " - " + score;
    }
}
