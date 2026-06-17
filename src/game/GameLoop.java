package game;

import javax.swing.Timer;

/**
 * Fixed-timestep driver. A single Swing {@link Timer} wakes on the EDT, advances
 * the model in fixed 1/60 s steps via an accumulator, then repaints. Replaces all
 * of the old per-entity threads — game state only ever changes here, on the EDT.
 */
public final class GameLoop {
    private static final int TIMER_MS = 15;
    private static final double DT = 1.0 / 60.0;
    private static final double MAX_FRAME = 0.25;

    private final GameModel model;
    private final GamePanel panel;
    private final Timer timer;

    private long last;
    private double acc;

    public GameLoop(GameModel model, GamePanel panel) {
        this.model = model;
        this.panel = panel;
        this.timer = new Timer(TIMER_MS, e -> tick());
        this.timer.setCoalesce(true);
    }

    public void start() {
        last = System.nanoTime();
        acc = 0;
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    private void tick() {
        long now = System.nanoTime();
        double frame = Math.min((now - last) / 1e9, MAX_FRAME);
        last = now;
        acc += frame;
        int guard = 0;
        while (acc >= DT && guard++ < 8) {
            model.update(DT);
            acc -= DT;
        }
        panel.repaint();
    }
}
