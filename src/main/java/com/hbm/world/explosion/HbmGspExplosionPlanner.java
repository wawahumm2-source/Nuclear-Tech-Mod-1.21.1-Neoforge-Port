package com.hbm.world.explosion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Pure worker-thread implementation of Tier 1's MK5 generalized-spiral ray calculation. */
final class HbmGspExplosionPlanner {
    private static final ConcurrentMap<Integer, DirectionTable> DIRECTION_CACHE = new ConcurrentHashMap<>();

    static BatchResult planBatch(HbmNuclearResistanceVolume snapshot, Parameters parameters,
            int startRay, int endRay) {
        long startedNanos = System.nanoTime();
        int pointCount = parameters.pointCount();
        int start = Math.clamp(startRay, 0, pointCount);
        int end = Math.clamp(endRay, start, pointCount);
        DirectionTable directions = DIRECTION_CACHE.computeIfAbsent(pointCount, DirectionTable::build);
        Map<Long, HbmHybridChunkMask> targets = new HashMap<>();

        for (int ray = start; ray < end; ray++) {
            traceRay(snapshot, parameters, directions, ray, targets);
        }

        int targetCount = 0;
        for (HbmHybridChunkMask mask : targets.values()) {
            targetCount += mask.targetCount();
        }
        return new BatchResult(start, end, targets, targetCount, System.nanoTime() - startedNanos);
    }

    static Direction directionAt(int index, int pointCount) {
        DirectionTable table = DIRECTION_CACHE.computeIfAbsent(pointCount, DirectionTable::build);
        int clamped = Math.clamp(index, 0, Math.max(0, pointCount - 1));
        return new Direction(table.x[clamped], table.y[clamped], table.z[clamped]);
    }

    private static void traceRay(HbmNuclearResistanceVolume snapshot, Parameters parameters,
            DirectionTable directions, int ray, Map<Long, HbmHybridChunkMask> targets) {
        double directionX = directions.x[ray];
        double directionY = directions.y[ray];
        double directionZ = directions.z[ray];
        double remainingPower = parameters.terrainStrength();
        int strengthLength = Math.max(1, (int) Math.ceil(parameters.terrainStrength()));
        double currentX = snapshot.originX() + 0.5D;
        double currentY = snapshot.originY() + 0.5D;
        double currentZ = snapshot.originZ() + 0.5D;

        for (int step = 0; step < parameters.maxSteps() && remainingPower > 0D; step++) {
            int blockX = (int) Math.floor(currentX);
            int blockY = (int) Math.floor(currentY);
            int blockZ = (int) Math.floor(currentZ);
            char encoded = snapshot.encodedAt(blockX, blockY, blockZ);
            if (encoded > HbmNuclearResistanceVolume.FLUID) {
                remainingPower -= HbmExplosionMath.mk5AdjustedResistanceLoss(
                        HbmNuclearResistanceVolume.decodeResistance(encoded),
                        step,
                        strengthLength,
                        directionY,
                        parameters.craterDepthMultiplier()
                );
            }
            if (remainingPower > 0D && encoded != HbmNuclearResistanceVolume.AIR) {
                long chunkKey = HbmChunkSchedulerMath.packChunk(blockX >> 4, blockZ >> 4);
                targets.computeIfAbsent(
                        chunkKey,
                        key -> new HbmHybridChunkMask(key, snapshot.minBuildHeight(), snapshot.buildHeight())
                ).add(blockX, blockY, blockZ);
            }
            currentX += directionX;
            currentY += directionY;
            currentZ += directionZ;
        }
    }

    record Parameters(float terrainStrength, int maxSteps, float craterDepthMultiplier, int pointCount) {
        Parameters {
            maxSteps = Math.max(1, maxSteps);
            craterDepthMultiplier = Math.max(1F, craterDepthMultiplier);
            pointCount = Math.max(1, pointCount);
        }
    }

    record BatchResult(int startRay, int endRay, Map<Long, HbmHybridChunkMask> targets,
            int targetCount, long planningNanos) {
    }

    record Direction(double x, double y, double z) {
    }

    private static final class DirectionTable {
        private final float[] x;
        private final float[] y;
        private final float[] z;

        private DirectionTable(float[] x, float[] y, float[] z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static DirectionTable build(int pointCount) {
            int count = Math.max(1, pointCount);
            float[] x = new float[count];
            float[] y = new float[count];
            float[] z = new float[count];
            double height = -1D;
            double azimuth = 0D;
            for (int index = 0; index < count; index++) {
                double radial = Math.sqrt(Math.max(0D, 1D - height * height));
                x[index] = (float) (radial * Math.cos(azimuth));
                y[index] = (float) height;
                z[index] = (float) (radial * Math.sin(azimuth));

                int sourcePoint = index + 1;
                if (sourcePoint >= count) {
                    height = 1D;
                    azimuth = 0D;
                } else {
                    height = HbmExplosionMath.mk5SpiralHeight(sourcePoint, count);
                    azimuth = (azimuth
                            + HbmExplosionMath.mk5SpiralAzimuthIncrement(sourcePoint, count)) % (Math.PI * 2D);
                }
            }
            return new DirectionTable(x, y, z);
        }
    }

    private HbmGspExplosionPlanner() {
    }
}
