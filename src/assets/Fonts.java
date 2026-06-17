package assets;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lazily-derived, cached fonts. Picks a clean monospaced family available on the
 * host JVM (arcade feel) with a graceful fallback chain.
 */
public final class Fonts {
    private Fonts() {}

    private static final String FAMILY = pickMonospaced();
    private static final Map<Float, Font> ARCADE = new HashMap<>();
    private static final Map<Float, Font> HUD = new HashMap<>();

    private static String pickMonospaced() {
        Set<String> available = new HashSet<>();
        try {
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()) {
                available.add(name);
            }
        } catch (Throwable ignored) {
            return Font.MONOSPACED;
        }
        for (String candidate : new String[] {
                "Consolas", "DejaVu Sans Mono", "Courier New", "Lucida Console", Font.MONOSPACED}) {
            if (available.contains(candidate)) return candidate;
        }
        return Font.MONOSPACED;
    }

    /** Bold display font for titles/HUD numbers. */
    public static Font arcade(float size) {
        return ARCADE.computeIfAbsent(size, s -> new Font(FAMILY, Font.BOLD, Math.round(s)));
    }

    /** Plain font for body/labels. */
    public static Font hud(float size) {
        return HUD.computeIfAbsent(size, s -> new Font(FAMILY, Font.PLAIN, Math.round(s)));
    }
}
