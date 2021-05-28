package com.birthdates.watchdog;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.HashMap;
import java.util.Map;

/**
 * Main instance
 */
public class Watchdog {

    private static Watchdog instance;
    private final Map<Long, ThreadData> threads = new HashMap<>();
    private final Thread thread;
    private boolean stop;

    public Watchdog() {
        (thread = new Thread(this::check, "Watchdog Thread")).start();
    }

    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("Watchdog already initialized!");
        }
        instance = new Watchdog();
    }

    /**
     * Thread function to check the current threads and make sure they're up to the TPS
     */
    public void check() {
        while (!stop) {
            synchronized (thread) {
                try {
                    thread.wait(500L);
                } catch (InterruptedException ignored) {
                }
            }

            synchronized (threads) {
                for (Map.Entry<Long, ThreadData> entry : threads.entrySet()) {
                    ThreadData data = entry.getValue();
                    if (!data.isDone())
                        continue;
                    if (data.getTPS() <= data.getMinTPS())
                        logThread(entry.getKey());
                    data.reset();
                }
            }
        }
    }

    /**
     * Dump the threads information
     * Similar to Spigot's thread dumping
     *
     * @param id Thread ID
     */
    private void logThread(long id) {
        ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(id, Integer.MAX_VALUE);

        System.out.println("================================================");
        System.out.println("Thread is under the minimum TPS (" + threadInfo.getThreadName() + ")");
        System.out.println("\tState: " + threadInfo.getThreadState());

        if (threadInfo.getLockedMonitors().length > 0) {
            System.out.println("\tThread is locked!");
            for (MonitorInfo lockedMonitor : threadInfo.getLockedMonitors()) {
                System.out.println("\t\tLocked on frame: " + lockedMonitor.getLockedStackFrame());
            }
        }

        System.out.println("\nCurrent stacktrace:");
        for (StackTraceElement trace : threadInfo.getStackTrace()) {
            System.out.println("\t\t" + trace);
        }
        System.out.println("================================================");
    }

    /**
     * Stop the watchdog thread
     */
    public void stop() {
        stop = true;
    }

    /**
     * Start watching the current thread
     *
     * @param minTPS Minimum TPS before thread log
     */
    public void startWatching(long minTPS) {
        watch(false, minTPS);
    }

    /**
     * Stop watching the current thread
     */
    public void stopWatching() {
        watch(true, -1);
    }

    /**
     * Tick the current thread (make sure it's accurate)
     */
    public void tick() {
        long id = getThreadId();
        ThreadData data = threads.getOrDefault(id, null);
        if (data == null)
            throw new IllegalStateException("Not watching " + id);
        data.addTick();
    }

    private void watch(boolean stop, long minTPS) {
        long id = getThreadId();
        synchronized (threads) {
            if (stop && threads.putIfAbsent(id, new ThreadData(minTPS)) == null || !stop && threads.remove(id) == null)
                return;
        }
        throw new IllegalStateException((stop ? "Already" : "Not") + " watching" + id);
    }

    private long getThreadId() {
        return Thread.currentThread().getId();
    }

    public static Watchdog getInstance() {
        return instance;
    }
}
