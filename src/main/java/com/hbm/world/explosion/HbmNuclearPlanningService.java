package com.hbm.world.explosion;

import com.hbm.config.HbmConfig;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded CPU pool for pure immutable nuclear calculations. */
final class HbmNuclearPlanningService {
    private static final Object LOCK = new Object();
    private static ThreadPoolExecutor executor;
    private static int configuredWorkers;
    private static int configuredQueueCapacity;

    static CompletableFuture<HbmHybridExplosionPlanner.Result> submit(
            HbmNuclearResistanceVolume snapshot,
            HbmHybridExplosionPlanner.Parameters parameters) {
        ThreadPoolExecutor current = executor();
        try {
            return CompletableFuture.supplyAsync(
                    () -> HbmHybridExplosionPlanner.plan(snapshot, parameters),
                    current
            );
        } catch (RejectedExecutionException rejected) {
            return null;
        }
    }

    static CompletableFuture<HbmGspExplosionPlanner.BatchResult> submitSourceBatch(
            HbmNuclearResistanceVolume snapshot,
            HbmGspExplosionPlanner.Parameters parameters,
            int startRay,
            int endRay) {
        ThreadPoolExecutor current = executor();
        try {
            return CompletableFuture.supplyAsync(
                    () -> HbmGspExplosionPlanner.planBatch(snapshot, parameters, startRay, endRay),
                    current
            );
        } catch (RejectedExecutionException rejected) {
            return null;
        }
    }

    static int activeWorkers() {
        ThreadPoolExecutor current = executor;
        return current == null ? 0 : current.getActiveCount();
    }

    static int queuedJobs() {
        ThreadPoolExecutor current = executor;
        return current == null ? 0 : current.getQueue().size();
    }

    static int boundedJobCapacity() {
        return Math.max(1, HbmConfig.BOMBS.nuclearHybridWorkerThreads.get()
                + HbmConfig.BOMBS.nuclearHybridQueueCapacity.get());
    }

    private static ThreadPoolExecutor executor() {
        int workers = HbmConfig.BOMBS.nuclearHybridWorkerThreads.get();
        int queueCapacity = HbmConfig.BOMBS.nuclearHybridQueueCapacity.get();
        synchronized (LOCK) {
            if (executor == null || executor.isShutdown()
                    || configuredWorkers != workers || configuredQueueCapacity != queueCapacity) {
                if (executor != null) {
                    executor.shutdown();
                }
                configuredWorkers = workers;
                configuredQueueCapacity = queueCapacity;
                executor = new ThreadPoolExecutor(
                        workers,
                        workers,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(queueCapacity),
                        new NuclearThreadFactory(),
                        new ThreadPoolExecutor.AbortPolicy()
                );
            }
            return executor;
        }
    }

    private static final class NuclearThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "HBM-Nuclear-Planner-" + this.sequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            return thread;
        }
    }

    private HbmNuclearPlanningService() {
    }
}
