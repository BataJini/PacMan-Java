package game;

/** Bonus fruit shown per level, with point value. Drawn procedurally by SpritePainter. */
public enum FruitType {
    CHERRY(100),
    STRAWBERRY(300),
    ORANGE(500),
    APPLE(700),
    MELON(1000),
    GALAXIAN(2000),
    BELL(3000),
    KEY(5000);

    private final int points;

    FruitType(int points) { this.points = points; }

    public int points() { return points; }

    public static FruitType forLevel(int level) {
        if (level <= 1) return CHERRY;
        if (level == 2) return STRAWBERRY;
        if (level <= 4) return ORANGE;
        if (level <= 6) return APPLE;
        if (level <= 8) return MELON;
        if (level <= 10) return GALAXIAN;
        if (level <= 12) return BELL;
        return KEY;
    }
}
