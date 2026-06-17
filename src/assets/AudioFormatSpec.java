package assets;

import javax.sound.sampled.AudioFormat;

/** Shared PCM format for all synthesized audio: 44.1 kHz, 16-bit, mono, signed LE. */
public final class AudioFormatSpec {
    private AudioFormatSpec() {}

    public static final float SAMPLE_RATE = 44100f;
    public static final int BITS = 16;
    public static final int CHANNELS = 1;

    public static final AudioFormat FORMAT = new AudioFormat(
            SAMPLE_RATE, BITS, CHANNELS, true, false); // signed, little-endian
}
