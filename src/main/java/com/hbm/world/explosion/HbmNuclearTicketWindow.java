package com.hbm.world.explosion;

import java.util.List;
import java.util.Set;
import java.util.function.LongPredicate;

/** Shared cursor rules for bounded nuclear chunk-ticket windows. */
final class HbmNuclearTicketWindow {
    private HbmNuclearTicketWindow() {
    }

    static int prefetch(List<Long> chunks, Set<Long> activeTickets, int prefetchIndex, int windowSize,
            LongPredicate ensureTicket) {
        int cursor = Math.clamp(prefetchIndex, 0, chunks.size());
        int boundedWindow = Math.max(0, windowSize);
        while (activeTickets.size() < boundedWindow && cursor < chunks.size()) {
            long chunkKey = chunks.get(cursor);
            if (!ensureTicket.test(chunkKey)) {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    static boolean hasSkippedCurrentChunk(List<Long> chunks, Set<Long> activeTickets, int currentIndex,
            int prefetchIndex) {
        return currentIndex >= 0
                && currentIndex < chunks.size()
                && prefetchIndex > currentIndex
                && !activeTickets.contains(chunks.get(currentIndex));
    }
}
