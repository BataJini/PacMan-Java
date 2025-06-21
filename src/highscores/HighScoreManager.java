package highscores;

import java.io.*;
import java.util.*;

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
            highScores = highScores.subList(0, MAX_SCORES);
        }
        saveScores();
    }

    public List<HighScoreEntry> getHighScores() {
        return new ArrayList<>(highScores);
    }

    private List<HighScoreEntry> loadScores() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new ArrayList<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<HighScoreEntry>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(highScores);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
