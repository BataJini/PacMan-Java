package game;

/** Immutable per-level tuning, built from the spec tables by {@link #forLevel}. */
public record LevelConfig(
        double pacSpeedMul,
        double ghostSpeedMul,
        double ghostTunnelMul,
        double frightPacMul,
        double frightGhostMul,
        double frightSeconds,
        int frightFlashes,
        int elroy1,
        int elroy2,
        FruitType fruit) {

    public static LevelConfig forLevel(int lvl) {
        double pac, gho, tun, fpac, fgho;
        if (lvl <= 1) {
            pac = 0.80; gho = 0.75; tun = 0.40; fpac = 0.90; fgho = 0.50;
        } else if (lvl <= 4) {
            pac = 0.90; gho = 0.85; tun = 0.45; fpac = 0.95; fgho = 0.55;
        } else if (lvl <= 20) {
            pac = 1.00; gho = 0.95; tun = 0.50; fpac = 1.00; fgho = 0.60;
        } else {
            pac = 0.90; gho = 0.95; tun = 0.50; fpac = 1.00; fgho = 0.60;
        }

        double frSecs;
        int flashes;
        switch (clamp(lvl)) {
            case 1: frSecs = 6; flashes = 5; break;
            case 2: frSecs = 5; flashes = 5; break;
            case 3: frSecs = 4; flashes = 5; break;
            case 4: frSecs = 3; flashes = 5; break;
            case 5: frSecs = 2; flashes = 5; break;
            case 6: frSecs = 5; flashes = 5; break;
            case 7: case 8: frSecs = 2; flashes = 5; break;
            default:
                if (lvl <= 17) { frSecs = 1; flashes = 3; }
                else { frSecs = 0; flashes = 0; }
        }

        int e1, e2;
        if (lvl <= 1) { e1 = 20; e2 = 10; }
        else if (lvl == 2) { e1 = 30; e2 = 15; }
        else if (lvl <= 5) { e1 = 40; e2 = 20; }
        else if (lvl <= 8) { e1 = 50; e2 = 25; }
        else if (lvl <= 11) { e1 = 60; e2 = 30; }
        else { e1 = 80; e2 = 40; }

        return new LevelConfig(pac, gho, tun, fpac, fgho, frSecs, flashes, e1, e2,
                FruitType.forLevel(lvl));
    }

    private static int clamp(int lvl) {
        return lvl < 1 ? 1 : lvl;
    }
}
