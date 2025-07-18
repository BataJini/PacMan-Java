package highscores;

import java.io.Serializable;

public class HighScoreEntry implements Serializable {
    private final String name;
    private final int score;

    public HighScoreEntry(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String toString() {
        return name + " - " + score;
    }

    public int getScore() {
        return score;
    }
}
