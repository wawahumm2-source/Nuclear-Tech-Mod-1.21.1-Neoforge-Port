package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class HbmGspExplosionPlannerTest {
    @Test
    void directionTablePreservesTheSourcePoles() {
        int count = 2048;
        assertEquals(-1D, HbmGspExplosionPlanner.directionAt(0, count).y(), 0.000001D);
        assertEquals(1D, HbmGspExplosionPlanner.directionAt(count - 1, count).y(), 0.000001D);
    }

    @Test
    void sourceBatchesComposeToTheSameResultAsOnePass() {
        HbmNuclearResistanceVolume snapshot = stoneSnapshot(false);
        HbmGspExplosionPlanner.Parameters parameters =
                new HbmGspExplosionPlanner.Parameters(14F, 7, 1F, 2048);

        HbmGspExplosionPlanner.BatchResult whole =
                HbmGspExplosionPlanner.planBatch(snapshot, parameters, 0, 2048);
        HbmGspExplosionPlanner.BatchResult first =
                HbmGspExplosionPlanner.planBatch(snapshot, parameters, 0, 1024);
        HbmGspExplosionPlanner.BatchResult second =
                HbmGspExplosionPlanner.planBatch(snapshot, parameters, 1024, 2048);

        BitSet combined = BitSet.valueOf(first.targets().get(0L).targetWords());
        combined.or(BitSet.valueOf(second.targets().get(0L).targetWords()));
        assertArrayEquals(whole.targets().get(0L).targetWords(), combined.toLongArray());
    }

    @Test
    void highResistancePlaneShadowsTerrainBehindIt() {
        HbmNuclearResistanceVolume snapshot = stoneSnapshot(true);
        HbmGspExplosionPlanner.Parameters parameters =
                new HbmGspExplosionPlanner.Parameters(14F, 7, 1F, 8192);

        HbmGspExplosionPlanner.BatchResult result =
                HbmGspExplosionPlanner.planBatch(snapshot, parameters, 0, 8192);
        HbmHybridChunkMask targets = result.targets().get(0L);

        assertTrue(targets.contains(9, 8, 8));
        assertFalse(targets.contains(14, 8, 8));
    }

    @Test
    void sourceScaleRayPlanningFitsTheFourCoreEnvelope() {
        HbmNuclearResistanceVolume snapshot = largeTerrainSnapshot();
        int rayCount = HbmExplosionMath.mk5SourceRayCount(240F);
        HbmGspExplosionPlanner.Parameters parameters =
                new HbmGspExplosionPlanner.Parameters(240F, 120, 1F, rayCount);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            int split = rayCount / 2;
            HbmGspExplosionPlanner.BatchResult[] results = assertTimeout(
                    Duration.ofSeconds(10),
                    () -> {
                        CompletableFuture<HbmGspExplosionPlanner.BatchResult> first = CompletableFuture.supplyAsync(
                                () -> HbmGspExplosionPlanner.planBatch(snapshot, parameters, 0, split), workers);
                        CompletableFuture<HbmGspExplosionPlanner.BatchResult> second = CompletableFuture.supplyAsync(
                                () -> HbmGspExplosionPlanner.planBatch(snapshot, parameters, split, rayCount), workers);
                        return new HbmGspExplosionPlanner.BatchResult[] { first.join(), second.join() };
                    }
            );
            assertTrue(results[0].targetCount() + results[1].targetCount() > 100_000);
        } finally {
            workers.shutdownNow();
        }
    }

    private static HbmNuclearResistanceVolume stoneSnapshot(boolean resistancePlane) {
        int origin = 8;
        int radius = 7;
        char[] section = new char[4096];
        char stone = HbmNuclearResistanceVolume.encodeResistance(2F);
        char barrier = HbmNuclearResistanceVolume.encodeResistance(4095F);
        Arrays.fill(section, HbmNuclearResistanceVolume.AIR);
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int dx = x - origin;
                    int dy = y - origin;
                    int dz = z - origin;
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        section[HbmNuclearResistanceVolume.localIndex(x, y, z)] =
                                resistancePlane && x == 10 ? barrier : stone;
                    }
                }
            }
        }
        return new HbmNuclearResistanceVolume(
                origin, origin, origin, radius,
                0, 0, 1, 1, 0, 1,
                0, 16,
                new char[][] { section }
        );
    }

    private static HbmNuclearResistanceVolume largeTerrainSnapshot() {
        int chunkWidth = 16;
        int chunkDepth = 16;
        int minSectionY = -4;
        int sectionCount = 16;
        char[][] sections = new char[chunkWidth * chunkDepth * sectionCount][];
        char stone = HbmNuclearResistanceVolume.encodeResistance(2F);
        for (int chunkX = 0; chunkX < chunkWidth; chunkX++) {
            for (int chunkZ = 0; chunkZ < chunkDepth; chunkZ++) {
                for (int sectionOffset = 0; sectionOffset <= 8; sectionOffset++) {
                    int index = (chunkX * chunkDepth + chunkZ) * sectionCount + sectionOffset;
                    sections[index] = new char[4096];
                    Arrays.fill(sections[index], stone);
                }
            }
        }
        return new HbmNuclearResistanceVolume(
                0, 64, 0, 120,
                -8, -8, chunkWidth, chunkDepth,
                minSectionY, sectionCount,
                -64, 384,
                sections
        );
    }
}
