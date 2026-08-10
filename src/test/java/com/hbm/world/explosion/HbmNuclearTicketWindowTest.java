package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HbmNuclearTicketWindowTest {
    @Test
    void failedForceLoadDoesNotAdvanceCursor() {
        List<Long> chunks = List.of(11L, 12L, 13L);
        Set<Long> activeTickets = new LinkedHashSet<>();

        int cursor = HbmNuclearTicketWindow.prefetch(
                chunks,
                activeTickets,
                0,
                2,
                ignored -> false
        );

        assertEquals(0, cursor);
        assertTrue(activeTickets.isEmpty());
    }

    @Test
    void cursorAdvancesOnlyAcrossSuccessfulRequests() {
        List<Long> chunks = List.of(11L, 12L, 13L);
        Set<Long> activeTickets = new LinkedHashSet<>();

        int cursor = HbmNuclearTicketWindow.prefetch(
                chunks,
                activeTickets,
                0,
                3,
                chunkKey -> {
                    if (chunkKey == 12L) {
                        return false;
                    }
                    activeTickets.add(chunkKey);
                    return true;
                }
        );

        assertEquals(1, cursor);
        assertEquals(Set.of(11L), activeTickets);
    }

    @Test
    void skippedCurrentChunkIsDetected() {
        List<Long> chunks = List.of(11L, 12L, 13L);

        assertTrue(HbmNuclearTicketWindow.hasSkippedCurrentChunk(chunks, Set.of(12L, 13L), 0, 3));
        assertFalse(HbmNuclearTicketWindow.hasSkippedCurrentChunk(chunks, Set.of(11L, 12L), 0, 2));
        assertFalse(HbmNuclearTicketWindow.hasSkippedCurrentChunk(chunks, Set.of(), 0, 0));
    }
}
