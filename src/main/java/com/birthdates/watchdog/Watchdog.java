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
    private final long checkDelay;
    private final Object lock = new Object();
    private boolean stop;

    private Watchdog(long checkDelay, boolean newThread) {
        this.checkDelay = checkDelay;
        if (newThread)
            new Thread(this::check, "Watchdog Thread").start();
        else
            check();
    }

    /**
     * Start the watchdog instance with a certain check delay
     *
     * @param checkDelay Delay in milliseconds
     * @param newThread  Should we do checking on a new thread?
     */
    public static void init(long checkDelay, boolean newThread) {
        if (instance != null) {
            throw new IllegalStateException("Watchdog already initialized!");
        }
        instance = new Watchdog(checkDelay, newThread);
    }

    public static Watchdog getInstance() {
        return instance;
    }

    /**
     * Thread function to check the current threads and make sure they're up to the TPS
     */
    public void check() {
        while (!stop) {
            synchronized (lock) {
                try {
                    lock.wait(checkDelay);
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
     * @throws IllegalStateException if we are watching the current thread
     */
    public void startWatching(long minTPS) {
        watch(false, minTPS);
    }

    /**
     * Stop watching the current thread
     *
     * @throws IllegalStateException if we aren't watching the current thread
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
}
