package com.hbm.world.explosion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HbmHybridExplosionPlannerTest {
    @Test
    void identicalInputProducesIdenticalChunkMasks() {
        HbmNuclearResistanceVolume snapshot = stoneSphereSnapshot(false);
        HbmHybridExplosionPlanner.Parameters parameters =
                new HbmHybridExplosionPlanner.Parameters(14F, 7, 2.5F, 16, 42L);

        HbmHybridExplosionPlanner.Result first = HbmHybridExplosionPlanner.plan(snapshot, parameters);
        HbmHybridExplosionPlanner.Result second = HbmHybridExplosionPlanner.plan(snapshot, parameters);

        assertEquals(first.targetCount(), second.targetCount());
        assertTrue(first.targetCount() > 0);
        HbmHybridChunkMask firstTargets = first.targets().get(0L);
        HbmHybridChunkMask secondTargets = second.targets().get(0L);
        assertArrayEquals(firstTargets.targetWords(), secondTargets.targetWords());
    }

    @Test
    void highResistancePlaneProtectsTerrainBehindIt() {
        HbmNuclearResistanceVolume snapshot = stoneSphereSnapshot(true);
        HbmHybridExplosionPlanner.Parameters parameters =
                new HbmHybridExplosionPlanner.Parameters(14F, 7, 1F, 24, 7L);

        HbmHybridExplosionPlanner.Result result = HbmHybridExplosionPlanner.plan(snapshot, parameters);
        HbmHybridChunkMask targets = result.targets().get(0L);

        assertTrue(targets.contains(9, 8, 8));
        assertFalse(targets.contains(14, 8, 8));
    }

    @Test
    void resistanceEncodingIsBoundedAndStable() {
        char encoded = HbmNuclearResistanceVolume.encodeResistance(6.25F);
        assertEquals(6.25F, HbmNuclearResistanceVolume.decodeResistance(encoded), 0.0001F);
        assertEquals(0F, HbmNuclearResistanceVolume.decodeResistance(HbmNuclearResistanceVolume.FLUID));
    }

    @Test
    void parallelPlanningIsSchedulingIndependent() throws Exception {
        HbmNuclearResistanceVolume snapshot = stoneSphereSnapshot(false);
        HbmHybridExplosionPlanner.Parameters parameters =
                new HbmHybridExplosionPlanner.Parameters(14F, 7, 2.5F, 24, 99L);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<HbmHybridExplosionPlanner.Result> first = CompletableFuture.supplyAsync(
                    () -> HbmHybridExplosionPlanner.plan(snapshot, parameters), workers);
            CompletableFuture<HbmHybridExplosionPlanner.Result> second = CompletableFuture.supplyAsync(
                    () -> HbmHybridExplosionPlanner.plan(snapshot, parameters), workers);
            HbmHybridChunkMask firstMask = first.get(5, TimeUnit.SECONDS).targets().get(0L);
            HbmHybridChunkMask secondMask = second.get(5, TimeUnit.SECONDS).targets().get(0L);
            assertArrayEquals(firstMask.targetWords(), secondMask.targetWords());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void sparseVolumeAccountsOnlyAllocatedSections() {
        HbmNuclearResistanceVolume snapshot = stoneSphereSnapshot(false);
        assertEquals(1, snapshot.allocatedSectionCount());
        assertTrue(snapshot.estimatedBytes() < 16_384L);
    }

    @Test
    void littleBoyScalePlanFitsTheTargetHardwareEnvelope() {
        HbmNuclearResistanceVolume snapshot = largeTerrainSnapshot();
        HbmHybridExplosionPlanner.Parameters parameters =
                new HbmHybridExplosionPlanner.Parameters(240F, 120, 2.5F, 64, 123456789L);

        HbmHybridExplosionPlanner.Result result = assertTimeout(
                Duration.ofSeconds(10),
                () -> HbmHybridExplosionPlanner.plan(snapshot, parameters)
        );

        assertTrue(result.targetCount() > 100_000);
        assertTrue(snapshot.estimatedBytes() < 24L * 1024L * 1024L);
    }

    private static HbmNuclearResistanceVolume stoneSphereSnapshot(boolean resistancePlane) {
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
