package assets;

import javax.sound.sampled.Clip;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Audio facade. One-shot cues and the looping siren are pre-rendered PCM played
 * through cached {@link Clip}s (each manages its own playback thread — the only
 * audio activity off the EDT, sharing no game state). Every operation is guarded
 * so a missing/limited mixer degrades to silence rather than crashing the game.
 */
public final class SoundEngine {
    private final EnumMap<Cue, Clip> cueClips = new EnumMap<>(Cue.class);
    private final EnumMap<SirenLevel, Clip> sirenClips = new EnumMap<>(SirenLevel.class);
    private final AtomicBoolean muted = new AtomicBoolean(false);

    private SirenLevel currentSiren;
    private boolean available = true;

    public enum SirenLevel {
        S1(380, 470, 5.0), S2(420, 520, 5.6), S3(470, 580, 6.2),
        S4(520, 650, 7.0), FRIGHT(120, 190, 9.0);

        final double lo, hi, lfoHz;
        SirenLevel(double lo, double hi, double lfoHz) { this.lo = lo; this.hi = hi; this.lfoHz = lfoHz; }
    }

    public void play(Cue cue) {
        if (!available || muted.get()) return;
        try {
            Clip clip = cueClips.computeIfAbsent(cue, c -> openClip(c.render()));
            if (clip == null) return;
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception ignored) {
            // Audio is non-essential; never let it break the game.
        }
    }

    public void startSiren(SirenLevel level) {
        if (!available || muted.get()) return;
        if (level == currentSiren && level != null) {
            Clip c = sirenClips.get(level);
            if (c != null && c.isRunning()) return;
        }
        stopSiren();
        try {
            Clip clip = sirenClips.computeIfAbsent(level, l -> openClip(buildSiren(l)));
            if (clip == null) return;
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            currentSiren = level;
        } catch (Exception ignored) {
        }
    }

    public void stopSiren() {
        currentSiren = null;
        for (Clip c : sirenClips.values()) {
            try { if (c != null && c.isRunning()) c.stop(); } catch (Exception ignored) {}
        }
    }

    public void setMuted(boolean m) {
        muted.set(m);
        if (m) {
            stopSiren();
            for (Clip c : cueClips.values()) {
                try { if (c != null) c.stop(); } catch (Exception ignored) {}
            }
        }
    }

    public boolean isMuted() { return muted.get(); }

    public void shutdown() {
        stopSiren();
        for (Clip c : cueClips.values()) closeQuietly(c);
        for (Clip c : sirenClips.values()) closeQuietly(c);
        cueClips.clear();
        sirenClips.clear();
        available = false;
    }

    // ----- internals -----

    private Clip openClip(byte[] pcm) {
        try {
            Clip clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(AudioFormatSpec.FORMAT, pcm, 0, pcm.length);
            return clip;
        } catch (Exception e) {
            available = false;
            return null;
        }
    }

    private static void closeQuietly(Clip c) {
        try { if (c != null) { c.stop(); c.close(); } } catch (Exception ignored) {}
    }

    /** Smooth sine siren whose pitch wobbles between lo/hi; length = whole LFO cycles. */
    private static byte[] buildSiren(SirenLevel l) {
        float sr = AudioFormatSpec.SAMPLE_RATE;
        int cycles = 2;
        int n = (int) (sr * cycles / l.lfoHz);
        byte[] out = new byte[n * 2];
        double mid = (l.lo + l.hi) / 2.0, amp = (l.hi - l.lo) / 2.0;
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / sr;
            double f = mid + amp * Math.sin(2 * Math.PI * l.lfoHz * t);
            phase += 2 * Math.PI * f / sr;
            double s = Math.sin(phase) * 0.16;
            int v = (int) Math.round(Math.max(-1, Math.min(1, s)) * 32767);
            out[i * 2] = (byte) (v & 0xff);
            out[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
        }
        return out;
    }
}
