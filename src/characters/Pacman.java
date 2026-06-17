package characters;

/** Player actor: smooth grid movement plus a buffered desired turn and chomp phase. */
public final class Pacman extends Entity {
    private Direction desiredDir = Direction.NONE;
    private double mouthPhase;

    public Direction desiredDir() { return desiredDir; }

    public void setDesiredDir(Direction d) {
        if (d != Direction.NONE) this.desiredDir = d;
    }

    public void clearDesired() { this.desiredDir = Direction.NONE; }

    public double mouthPhase() { return mouthPhase; }

    public void advanceMouth(double amount) { this.mouthPhase += amount; }

    public void resetMouth() { this.mouthPhase = 0; }
}
