package com.hbm.world.explosion;

/** Fairly divides bounded per-level work among active nuclear scheduler lanes. */
final class HbmNuclearLaneAllocator {
    private static final int PLANNING = 0;
    private static final int MUTATION = 1;
    private static final int WATER = 2;
    private static final int FALLOUT = 3;
    private static final int LANE_COUNT = 4;

    private HbmNuclearLaneAllocator() {
    }

    static Allocation allocateFair(int totalUnits, int cursor,
            boolean planningActive, boolean mutationActive, boolean waterActive, boolean falloutActive) {
        int total = Math.max(0, totalUnits);
        boolean[] active = {planningActive, mutationActive, waterActive, falloutActive};
        int activeCount = countActive(active);
        if (total == 0 || activeCount == 0) {
            return new Allocation(0, 0, 0, 0);
        }

        int[] units = new int[LANE_COUNT];
        int baseShare = total / activeCount;
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            if (active[lane]) {
                units[lane] = baseShare;
            }
        }
        distribute(units, active, total - baseShare * activeCount, cursor, false);
        return allocation(units);
    }

    static Allocation allocateTickets(int totalTickets, int contendedWaterCap, int cursor,
            boolean planningActive, boolean mutationActive, boolean waterActive, boolean falloutActive) {
        boolean[] active = {planningActive, mutationActive, waterActive, falloutActive};
        Allocation fair = allocateFair(totalTickets, cursor,
                planningActive, mutationActive, waterActive, falloutActive);
        if (!waterActive || countActive(active) <= 1) {
            return fair;
        }

        int[] tickets = {fair.planning(), fair.mutation(), fair.water(), fair.fallout()};
        int excess = Math.max(0, tickets[WATER] - Math.max(0, contendedWaterCap));
        tickets[WATER] -= excess;
        distribute(tickets, active, excess, cursor + 1, true);
        return allocation(tickets);
    }

    private static int countActive(boolean[] active) {
        int count = 0;
        for (boolean laneActive : active) {
            if (laneActive) {
                count++;
            }
        }
        return count;
    }

    private static void distribute(int[] units, boolean[] active, int amount, int cursor,
            boolean excludeWater) {
        int lane = Math.floorMod(cursor, LANE_COUNT);
        while (amount > 0) {
            if (active[lane] && (!excludeWater || lane != WATER)) {
                units[lane]++;
                amount--;
            }
            lane = (lane + 1) % LANE_COUNT;
        }
    }

    private static Allocation allocation(int[] units) {
        return new Allocation(units[PLANNING], units[MUTATION], units[WATER], units[FALLOUT]);
    }

    record Allocation(int planning, int mutation, int water, int fallout) {
        int total() {
            return this.planning + this.mutation + this.water + this.fallout;
        }
    }
}
