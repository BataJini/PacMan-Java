package game;

public class GameState {
    private int score = 0;
    private int lives = 3;
    private long startTime;
    private boolean running = true;
    
    private boolean speedBoostActive = false;
    private boolean invincibilityActive = false;
    private long speedBoostEndTime = 0;
    private long invincibilityEndTime = 0;
    
    private static final long SPEED_BOOST_DURATION = 10000;
    private static final long INVINCIBILITY_DURATION = 8000;

    public GameState() {
        this.startTime = System.currentTimeMillis();
    }

    public int getScore() {
        return score;
    }

    public void incrementScore() {
        score++;
    }

    public int getLives() {
        return lives;
    }

    public void addLife() {
        lives++;
    }

    public void loseLife() {
        lives--;
    }

    public long getElapsedTime() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isGameOver() {
        return lives <= 0;
    }
    
    public boolean isSpeedBoostActive() {
        return speedBoostActive && System.currentTimeMillis() < speedBoostEndTime;
    }
    
    public void activateSpeedBoost() {
        speedBoostActive = true;
        speedBoostEndTime = System.currentTimeMillis() + SPEED_BOOST_DURATION;
    }
    
    public long getSpeedBoostTimeRemaining() {
        if (!speedBoostActive) return 0;
        long remaining = speedBoostEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    public boolean isInvincibilityActive() {
        return invincibilityActive && System.currentTimeMillis() < invincibilityEndTime;
    }
    
    public void activateInvincibility() {
        invincibilityActive = true;
        invincibilityEndTime = System.currentTimeMillis() + INVINCIBILITY_DURATION;
    }
    
    public long getInvincibilityTimeRemaining() {
        if (!invincibilityActive) return 0;
        long remaining = invincibilityEndTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}
