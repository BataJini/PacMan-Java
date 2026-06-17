package game;

import java.awt.Color;

/** Lightweight death-burst particle in tile coordinates. */
public final class Particle {
    public double col;
    public double row;
    public double vc;   // velocity (tiles/sec)
    public double vr;
    public final Color color;
    public final double ttl;
    public double age;

    public Particle(double col, double row, double vc, double vr, Color color, double ttl) {
        this.col = col;
        this.row = row;
        this.vc = vc;
        this.vr = vr;
        this.color = color;
        this.ttl = ttl;
    }

    public boolean expired() { return age >= ttl; }

    public double life() { return Math.min(1.0, age / ttl); }
}
