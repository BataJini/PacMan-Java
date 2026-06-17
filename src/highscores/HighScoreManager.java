package highscores;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Persists the top-10 high scores to {@code highscores.ser} (byte-compatible). */
public class HighScoreManager {
    private static final String FILE_NAME = "highscores.ser";
    private static final int MAX_SCORES = 10;

    private List<HighScoreEntry> highScores;

    public HighScoreManager() {
        highScores = loadScores();
    }

    public void addScore(String name, int score) {
        highScores.add(new HighScoreEntry(name, score));
        highScores.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (highScores.size() > MAX_SCORES) {
            highScores = new ArrayList<>(highScores.subList(0, MAX_SCORES));
        }
        saveScores();
    }

    public List<HighScoreEntry> getHighScores() {
        return new ArrayList<>(highScores);
    }

    /** True if the score would land in the top-10 table. */
    public boolean qualifies(int score) {
        if (score <= 0) return false;
        if (highScores.size() < MAX_SCORES) return true;
        return score > highScores.get(highScores.size() - 1).getScore();
    }

    public int topScore() {
        return highScores.isEmpty() ? 0 : highScores.get(0).getScore();
    }

    @SuppressWarnings("unchecked")
    private List<HighScoreEntry> loadScores() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object data = ois.readObject();
            if (data instanceof List<?>) {
                return new ArrayList<>((List<HighScoreEntry>) data);
            }
            return new ArrayList<>();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("High scores could not be loaded: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(highScores);
        } catch (IOException e) {
            System.err.println("High scores could not be saved: " + e.getMessage());
        }
    }
}
