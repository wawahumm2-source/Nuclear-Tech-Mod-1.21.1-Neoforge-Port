package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HbmNuclearLaneAllocatorTest {
    @Test
    void allActiveTicketLanesShareTheGlobalCap() {
        HbmNuclearLaneAllocator.Allocation allocation = HbmNuclearLaneAllocator.allocateTickets(
                16, 4, 0, true, true, true, true
        );

        assertEquals(4, allocation.planning());
        assertEquals(4, allocation.mutation());
        assertEquals(4, allocation.water());
        assertEquals(4, allocation.fallout());
        assertEquals(16, allocation.total());
    }

    @Test
    void waterUsesTheWholeTicketBudgetWhenItIsTheOnlyActiveLane() {
        HbmNuclearLaneAllocator.Allocation allocation = HbmNuclearLaneAllocator.allocateTickets(
                16, 4, 0, false, false, true, false
        );

        assertEquals(16, allocation.water());
        assertEquals(16, allocation.total());
    }

    @Test
    void contendedWaterCapReturnsUnusedTicketsToOtherWork() {
        HbmNuclearLaneAllocator.Allocation allocation = HbmNuclearLaneAllocator.allocateTickets(
                16, 4, 0, true, false, true, false
        );

        assertEquals(12, allocation.planning());
        assertEquals(4, allocation.water());
        assertEquals(16, allocation.total());
    }

    @Test
    void rotatingRemainderPreventsStarvationBelowTheLaneCount() {
        int[] totals = new int[4];
        for (int cursor = 0; cursor < 4; cursor++) {
            HbmNuclearLaneAllocator.Allocation allocation = HbmNuclearLaneAllocator.allocateFair(
                    2, cursor, true, true, true, true
            );
            totals[0] += allocation.planning();
            totals[1] += allocation.mutation();
            totals[2] += allocation.water();
            totals[3] += allocation.fallout();
            assertEquals(2, allocation.total());
        }

        for (int total : totals) {
            assertTrue(total > 0);
        }
    }

    @Test
    void rotatingTimeRemainderBalancesFourBusyLanes() {
        int[] totals = new int[4];
        for (int cursor = 0; cursor < 4; cursor++) {
            HbmNuclearLaneAllocator.Allocation allocation = HbmNuclearLaneAllocator.allocateFair(
                    35, cursor, true, true, true, true
            );
            totals[0] += allocation.planning();
            totals[1] += allocation.mutation();
            totals[2] += allocation.water();
            totals[3] += allocation.fallout();
            assertEquals(35, allocation.total());
        }

        assertEquals(35, totals[0]);
        assertEquals(35, totals[1]);
        assertEquals(35, totals[2]);
        assertEquals(35, totals[3]);
    }

    @Test
    void inactiveLanesNeverReceiveWork() {
        HbmNuclearLaneAllocator.Allocation allocation = HbmNuclearLaneAllocator.allocateFair(
                9, 3, false, true, false, true
        );

        assertEquals(0, allocation.planning());
        assertEquals(0, allocation.water());
        assertEquals(9, allocation.mutation() + allocation.fallout());
        assertEquals(9, allocation.total());
    }
}
