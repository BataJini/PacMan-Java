package game;

/** Selectable board sizes shown in the menu; carries the raw map id and ghost count. */
public enum BoardSizeOption {
    SMALL("SMALL", Map.SMALL_MAP, 3),
    MEDIUM("MEDIUM", Map.MEDIUM_MAP, 4),
    LARGE("LARGE", Map.LARGE_MAP, 4);

    private final String label;
    private final int mapId;
    private final int ghostCount;

    BoardSizeOption(String label, int mapId, int ghostCount) {
        this.label = label;
        this.mapId = mapId;
        this.ghostCount = ghostCount;
    }

    public String label() { return label; }
    public int mapId() { return mapId; }
    public int ghostCount() { return ghostCount; }
}
