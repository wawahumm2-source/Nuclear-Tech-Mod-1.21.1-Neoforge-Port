package com.hbm.world.explosion;

import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/** Per-server-tick cap on new nuclear force-load requests. */
final class HbmNuclearChunkLoadBudget {
    private int remaining;
    private final long deadlineNanos;

    HbmNuclearChunkLoadBudget(int maximum) {
        this(maximum, Long.MAX_VALUE);
    }

    HbmNuclearChunkLoadBudget(int maximum, long deadlineNanos) {
        this.remaining = Math.max(0, maximum);
        this.deadlineNanos = deadlineNanos;
    }

    boolean ensureForced(ServerLevel level, UUID owner, long chunkKey, Set<Long> activeTickets) {
        if (activeTickets.contains(chunkKey)) {
            return true;
        }
        if (this.remaining <= 0 || System.nanoTime() >= this.deadlineNanos) {
            return false;
        }
        HbmNuclearChunkTickets.force(level, owner, chunkKey);
        activeTickets.add(chunkKey);
        this.remaining--;
        return true;
    }

    int remaining() {
        return this.remaining;
    }

    boolean hasTimeRemaining() {
        return System.nanoTime() < this.deadlineNanos;
    }
}
