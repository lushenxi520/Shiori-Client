package client.utils.animation;

public class Timer {
    private long time = System.currentTimeMillis();

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public boolean hasPassed(long durationMs) {
        return System.currentTimeMillis() - this.time > durationMs;
    }
}