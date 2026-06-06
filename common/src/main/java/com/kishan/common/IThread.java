package com.kishan.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Common repeating thread abstraction for scheduled loop execution.
 */
public interface IThread {

    boolean startThread(long refreshMillis);

    boolean isThreadRunning();

    boolean stopThread() throws Exception;

    boolean pauseThread() throws Exception;

    boolean resumeThread() throws Exception;

    void shutdown() throws Exception;

    static IThread create(Runnable repeat) {
        return new IThreadImpl(repeat);
    }
}

final class IThreadImpl implements IThread {

    private final Runnable repeat;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private volatile boolean paused;

    IThreadImpl(Runnable repeat) {
        this.repeat = repeat;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mw-ithread");
            thread.setDaemon(true);
            return thread;
        });
        this.paused = false;
    }

    @Override
    public synchronized boolean startThread(long refreshMillis) {
        if (isThreadRunning()) {
            return false;
        }
        paused = false;
        future = executor.scheduleAtFixedRate(() -> {
            if (!paused) {
                repeat.run();
            }
        }, 0, refreshMillis, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public synchronized boolean isThreadRunning() {
        return future != null && !future.isDone() && !future.isCancelled();
    }

    @Override
    public synchronized boolean stopThread() throws Exception {
        if (!isThreadRunning()) {
            return false;
        }

        boolean cancelled = future.cancel(false);
        if (!cancelled && !future.isDone()) {
            throw new IllegalStateException("Unable to stop IThread");
        }
        future = null;
        return true;
    }

    @Override
    public synchronized boolean pauseThread() throws Exception {
        if (!isThreadRunning()) {
            return false;
        }
        paused = true;
        return true;
    }

    @Override
    public synchronized boolean resumeThread() throws Exception {
        if (!isThreadRunning()) {
            return false;
        }
        paused = false;
        return true;
    }

    @Override
    public void shutdown() throws Exception {
        stopThread();
        executor.shutdown();
    }
}
