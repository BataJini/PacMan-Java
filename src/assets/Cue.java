package assets;

import assets.Synth.Wave;

/** One-shot SFX catalog. Each cue renders its own PCM buffer (cached by SoundEngine). */
public enum Cue {
    CHOMP_A {
        public byte[] render() { return Synth.sweep(230, 150, 55, Wave.SQUARE, 0.32); }
    },
    CHOMP_B {
        public byte[] render() { return Synth.sweep(150, 95, 55, Wave.SQUARE, 0.32); }
    },
    POWER {
        public byte[] render() {
            return Synth.arpeggio(new double[] {300, 380, 300, 380}, 70, Wave.SQUARE, 0.30);
        }
    },
    EAT_GHOST {
        public byte[] render() {
            return Synth.arpeggio(
                    new double[] {200, 300, 420, 560, 720, 900, 1080, 1260}, 36, Wave.SQUARE, 0.34);
        }
    },
    GHOST_RETREAT {
        public byte[] render() { return Synth.sweep(900, 300, 260, Wave.SINE, 0.28); }
    },
    DEATH {
        public byte[] render() {
            return Synth.concat(
                    Synth.sweep(620, 90, 900, Wave.SQUARE, 0.34),
                    Synth.noiseBurst(260, 0.22, 9001));
        }
    },
    EXTRA_LIFE {
        public byte[] render() {
            return Synth.arpeggio(new double[] {784, 1046, 1318, 1568}, 90, Wave.TRIANGLE, 0.32);
        }
    },
    INTRO {
        public byte[] render() {
            return Synth.arpeggio(
                    new double[] {523, 659, 784, 1046, 784, 659, 523, 392}, 110, Wave.SQUARE, 0.30);
        }
    },
    FRUIT {
        public byte[] render() {
            return Synth.concat(
                    Synth.tone(880, 90, Wave.SINE, 0.34),
                    Synth.tone(1175, 140, Wave.SINE, 0.34));
        }
    },
    MENU_MOVE {
        public byte[] render() { return Synth.tone(440, 40, Wave.SQUARE, 0.25); }
    },
    MENU_SELECT {
        public byte[] render() {
            return Synth.arpeggio(new double[] {520, 780}, 55, Wave.SQUARE, 0.30);
        }
    };

    /** Builds the PCM buffer for this cue. */
    public abstract byte[] render();
}
