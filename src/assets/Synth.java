package assets;

/**
 * Pure PCM generators producing 16-bit mono little-endian sample buffers.
 * No I/O, no state — just maths. Short attack/release envelopes avoid clicks.
 */
public final class Synth {
    private Synth() {}

    public enum Wave { SQUARE, SINE, TRIANGLE, SAW }

    private static final float SR = AudioFormatSpec.SAMPLE_RATE;

    /** A single tone of the given wave, frequency and duration. */
    public static byte[] tone(double freq, double ms, Wave wave, double vol) {
        int n = (int) (SR * ms / 1000.0);
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t = i / SR;
            double env = envelope(i, n, 0.01, 0.03);
            double s = waveform(wave, freq, t) * vol * env;
            writeSample(out, i, s);
        }
        return out;
    }

    /** A frequency sweep from f0 to f1. */
    public static byte[] sweep(double f0, double f1, double ms, Wave wave, double vol) {
        int n = (int) (SR * ms / 1000.0);
        byte[] out = new byte[n * 2];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double k = (double) i / n;
            double f = f0 + (f1 - f0) * k;
            phase += 2 * Math.PI * f / SR;
            double env = envelope(i, n, 0.01, 0.08);
            double s = oscillator(wave, phase) * vol * env;
            writeSample(out, i, s);
        }
        return out;
    }

    /** A rising/falling sequence of equal-length notes. */
    public static byte[] arpeggio(double[] freqs, double stepMs, Wave wave, double vol) {
        int per = (int) (SR * stepMs / 1000.0);
        byte[] out = new byte[per * freqs.length * 2];
        for (int j = 0; j < freqs.length; j++) {
            for (int i = 0; i < per; i++) {
                double t = i / SR;
                double env = envelope(i, per, 0.02, 0.05);
                double s = waveform(wave, freqs[j], t) * vol * env;
                writeSample(out, j * per + i, s);
            }
        }
        return out;
    }

    /** A burst of white noise (used for the death tail). */
    public static byte[] noiseBurst(double ms, double vol, long seed) {
        int n = (int) (SR * ms / 1000.0);
        byte[] out = new byte[n * 2];
        long state = seed;
        for (int i = 0; i < n; i++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            double r = ((state >>> 40) / (double) (1L << 24)) * 2 - 1;
            double env = envelope(i, n, 0.005, 0.2);
            writeSample(out, i, r * vol * env);
        }
        return out;
    }

    /** Concatenate buffers into one. */
    public static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] out = new byte[total];
        int off = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, off, p.length);
            off += p.length;
        }
        return out;
    }

    /** Mix b on top of a (sample-wise add, clamped). Result length = max. */
    public static byte[] mix(byte[] a, byte[] b) {
        int na = a.length / 2, nb = b.length / 2, n = Math.max(na, nb);
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            int sa = i < na ? readSample(a, i) : 0;
            int sb = i < nb ? readSample(b, i) : 0;
            int s = sa + sb;
            if (s > 32767) s = 32767;
            if (s < -32768) s = -32768;
            out[i * 2] = (byte) (s & 0xff);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        return out;
    }

    // ----- internals -----

    private static double waveform(Wave wave, double freq, double t) {
        return oscillator(wave, 2 * Math.PI * freq * t);
    }

    private static double oscillator(Wave wave, double phase) {
        double p = phase % (2 * Math.PI);
        if (p < 0) p += 2 * Math.PI;
        switch (wave) {
            case SQUARE: return p < Math.PI ? 1.0 : -1.0;
            case TRIANGLE: return 2.0 / Math.PI * Math.asin(Math.sin(p));
            case SAW: return (p / Math.PI) - 1.0;
            case SINE:
            default: return Math.sin(p);
        }
    }

    private static double envelope(int i, int n, double attackFrac, double releaseFrac) {
        int a = Math.max(1, (int) (n * attackFrac));
        int r = Math.max(1, (int) (n * releaseFrac));
        if (i < a) return (double) i / a;
        if (i > n - r) return Math.max(0, (double) (n - i) / r);
        return 1.0;
    }

    private static void writeSample(byte[] out, int i, double s) {
        int v = (int) Math.round(Math.max(-1.0, Math.min(1.0, s)) * 32767);
        out[i * 2] = (byte) (v & 0xff);
        out[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
    }

    private static int readSample(byte[] buf, int i) {
        int lo = buf[i * 2] & 0xff;
        int hi = buf[i * 2 + 1];
        return (hi << 8) | lo;
    }
}
