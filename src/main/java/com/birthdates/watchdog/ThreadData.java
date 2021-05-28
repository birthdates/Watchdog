package com.birthdates.watchdog;

public class ThreadData {
    private final long minTPS;
    private long tps;
    private long toReset;

    public ThreadData(long minTPS) {
        this.minTPS = minTPS;
        reset();
    }

    public void reset() {
        toReset = System.currentTimeMillis() + 1000L;
        tps = 0;
    }

    public boolean isDone() {
        return System.currentTimeMillis() < toReset;
    }

    public void addTick() {
        tps++;
    }

    public long getTPS() {
        return tps;
    }

    public long getMinTPS() {
        return minTPS;
    }
}
