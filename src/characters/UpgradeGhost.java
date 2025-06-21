package characters;

import assets.Assets;
import javax.swing.*;
import java.awt.*;

public class UpgradeGhost {
    private int x, y;
    private UpgradeType type;
    private ImageIcon icon;
    private static final int ICON_SIZE = 16;
    public UpgradeGhost(int x, int y) {
        this.x = x;
        this.y = y;
        UpgradeType[] values = UpgradeType.values();
        this.type = values[new java.util.Random().nextInt(values.length)];
        createIcon();
    }
    private void createIcon() {
        switch (type) {
            case SPEED:
                icon = Assets.SPEED;
                break;
            case LIFE:
                icon = Assets.LIFE;
                break;
            case INVINCIBILITY:
                icon = Assets.INVINCIBILITY;
                break;
        }
        Image img = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        icon = new ImageIcon(img);
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public UpgradeType getType() {
        return type;
    }
    public ImageIcon getIcon() {
        return icon;
    }
}