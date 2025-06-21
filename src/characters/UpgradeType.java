package characters;

import java.awt.*;

public enum UpgradeType {

    SPEED("Speed Boost", Color.YELLOW),
    LIFE("Extra Life", Color.GREEN),
    INVINCIBILITY("Invincibility", Color.BLUE);

    private final String displayName;
    private final Color color;
    UpgradeType(String displayName, Color color) {
        this.displayName = displayName;
        this.color = color;
    }
    public String getDisplayName() {
        return displayName;
    }
    public Color getColor() {
        return color;
    }
}
