package game;

import java.awt.Color;

/** Floating score text (ghost-eat chain or fruit) in tile coordinates. */
public final class ScorePopup {
    public final double col;
    public final double row;
    public final String text;
    public final Color color;
    public final double ttl;
    public double age;

    public ScorePopup(double col, double row, String text, Color color, double ttl) {
        this.col = col;
        this.row = row;
        this.text = text;
        this.color = color;
        this.ttl = ttl;
    }

    public boolean expired() { return age >= ttl; }

    /** 0..1 life fraction. */
    public double life() { return Math.min(1.0, age / ttl); }
}
