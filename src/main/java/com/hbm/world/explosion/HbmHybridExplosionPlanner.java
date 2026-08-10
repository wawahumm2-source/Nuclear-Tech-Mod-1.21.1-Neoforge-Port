package com.hbm.world.explosion;

import java.util.HashMap;
import java.util.Map;

/** Pure deterministic MK5 approximation. This class has no world or registry access. */
final class HbmHybridExplosionPlanner {
    private static final float BARRIER_DISCONTINUITY = 6F;

    static Result plan(HbmNuclearResistanceVolume snapshot, Parameters parameters) {
        long startedNanos = System.nanoTime();
        DirectionField field = DirectionField.trace(snapshot, parameters);
        Map<Long, HbmHybridChunkMask> targets = new HashMap<>();
        int targetCount = buildTargetMasks(snapshot, parameters, field, targets);
        return new Result(targets, targetCount, System.nanoTime() - startedNanos);
    }

    private static int buildTargetMasks(HbmNuclearResistanceVolume snapshot, Parameters parameters,
            DirectionField field, Map<Long, HbmHybridChunkMask> targets) {
        int targetCount = 0;
        int radiusSquared = snapshot.radius() * snapshot.radius();
        for (int sectionIndex = 0; sectionIndex < snapshot.sectionArrayLength(); sectionIndex++) {
            char[] section = snapshot.sectionAt(sectionIndex);
            if (section == null) {
                continue;
            }
            int chunkX = snapshot.chunkXForSectionIndex(sectionIndex);
            int chunkZ = snapshot.chunkZForSectionIndex(sectionIndex);
            int sectionY = snapshot.sectionYForIndex(sectionIndex);
            int baseX = chunkX << 4;
            int baseY = sectionY << 4;
            int baseZ = chunkZ << 4;
            HbmHybridChunkMask chunkTargets = null;

            for (int localIndex = 0; localIndex < section.length; localIndex++) {
                if (section[localIndex] == HbmNuclearResistanceVolume.AIR) {
                    continue;
                }
                int localY = localIndex >>> 8;
                int localZ = localIndex >>> 4 & 15;
                int localX = localIndex & 15;
                int blockX = baseX + localX;
                int blockY = baseY + localY;
                int blockZ = baseZ + localZ;
                double dx = blockX - snapshot.originX();
                double dy = blockY - snapshot.originY();
                double dz = blockZ - snapshot.originZ();
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                if (distanceSquared > radiusSquared || distanceSquared == 0D) {
                    continue;
                }
                double distance = Math.sqrt(distanceSquared);
                float reach = field.sampleReach(dx, dy, dz);
                if (reach < 0F || distance > reach + 0.75D) {
                    continue;
                }

                if (chunkTargets == null) {
                    long chunkKey = HbmChunkSchedulerMath.packChunk(chunkX, chunkZ);
                    chunkTargets = targets.computeIfAbsent(
                            chunkKey,
                            key -> new HbmHybridChunkMask(key, snapshot.minBuildHeight(), snapshot.buildHeight())
                    );
                }
                chunkTargets.add(blockX, blockY, blockZ);
                targetCount++;
            }
        }
        return targetCount;
    }

    record Parameters(float terrainStrength, int maxSteps, float craterDepthMultiplier,
            int resolution, long seed) {
        Parameters {
            maxSteps = Math.max(1, maxSteps);
            resolution = Math.clamp(resolution, 4, 256);
        }
    }

    record Result(Map<Long, HbmHybridChunkMask> targets, int targetCount, long planningNanos) {
        Result {
            targets = Map.copyOf(targets);
        }
    }

    private static final class DirectionField {
        private final int resolution;
        private final float[] reaches;
        private final double rotationCos;
        private final double rotationSin;

        private DirectionField(int resolution, float[] reaches, double rotationCos, double rotationSin) {
            this.resolution = resolution;
            this.reaches = reaches;
            this.rotationCos = rotationCos;
            this.rotationSin = rotationSin;
        }

        static DirectionField trace(HbmNuclearResistanceVolume snapshot, Parameters parameters) {
            int resolution = parameters.resolution();
            float[] reaches = new float[6 * resolution * resolution];
            double rotation = deterministicRotation(parameters.seed());
            double rotationCos = Math.cos(rotation);
            double rotationSin = Math.sin(rotation);
            int cursor = 0;
            for (int face = 0; face < 6; face++) {
                for (int y = 0; y < resolution; y++) {
                    double v = coordinate(y, resolution);
                    for (int x = 0; x < resolution; x++) {
                        double u = coordinate(x, resolution);
                        Direction local = faceDirection(face, u, v);
                        double worldX = local.x * rotationCos - local.z * rotationSin;
                        double worldZ = local.x * rotationSin + local.z * rotationCos;
                        reaches[cursor++] = traceReach(snapshot, parameters, worldX, local.y, worldZ);
                    }
                }
            }
            return new DirectionField(resolution, reaches, rotationCos, rotationSin);
        }

        float sampleReach(double worldX, double worldY, double worldZ) {
            double localX = worldX * this.rotationCos + worldZ * this.rotationSin;
            double localZ = -worldX * this.rotationSin + worldZ * this.rotationCos;
            FaceCoordinate coordinate = faceCoordinate(localX, worldY, localZ);
            double sampleX = (coordinate.u + 1D) * 0.5D * (this.resolution - 1);
            double sampleY = (coordinate.v + 1D) * 0.5D * (this.resolution - 1);
            int x0 = Math.clamp((int) Math.floor(sampleX), 0, this.resolution - 1);
            int y0 = Math.clamp((int) Math.floor(sampleY), 0, this.resolution - 1);
            int x1 = Math.min(this.resolution - 1, x0 + 1);
            int y1 = Math.min(this.resolution - 1, y0 + 1);
            float r00 = reach(coordinate.face, x0, y0);
            float r10 = reach(coordinate.face, x1, y0);
            float r01 = reach(coordinate.face, x0, y1);
            float r11 = reach(coordinate.face, x1, y1);
            float minimum = Math.min(Math.min(r00, r10), Math.min(r01, r11));
            float maximum = Math.max(Math.max(r00, r10), Math.max(r01, r11));
            if (minimum < 0F || maximum - minimum > BARRIER_DISCONTINUITY) {
                return minimum;
            }
            float xFraction = (float) (sampleX - x0);
            float yFraction = (float) (sampleY - y0);
            float top = r00 + (r10 - r00) * xFraction;
            float bottom = r01 + (r11 - r01) * xFraction;
            return top + (bottom - top) * yFraction;
        }

        private float reach(int face, int x, int y) {
            return this.reaches[(face * this.resolution + y) * this.resolution + x];
        }

        private static float traceReach(HbmNuclearResistanceVolume snapshot, Parameters parameters,
                double directionX, double directionY, double directionZ) {
            double remainingPower = parameters.terrainStrength();
            float reach = -1F;
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
                if (remainingPower <= 0D) {
                    break;
                }
                reach = step;
                currentX += directionX;
                currentY += directionY;
                currentZ += directionZ;
            }
            return reach;
        }

        private static double coordinate(int index, int resolution) {
            return resolution <= 1 ? 0D : index * 2D / (resolution - 1D) - 1D;
        }

        private static double deterministicRotation(long seed) {
            long mixed = seed ^ seed >>> 33;
            mixed *= 0xff51afd7ed558ccdl;
            mixed ^= mixed >>> 33;
            double unit = (mixed >>> 11) * 0x1.0p-53;
            return unit * Math.PI * 2D;
        }

        private static Direction faceDirection(int face, double u, double v) {
            double x;
            double y;
            double z;
            switch (face) {
                case 0 -> { x = 1D; y = v; z = -u; }
                case 1 -> { x = -1D; y = v; z = u; }
                case 2 -> { x = u; y = 1D; z = -v; }
                case 3 -> { x = u; y = -1D; z = v; }
                case 4 -> { x = u; y = v; z = 1D; }
                case 5 -> { x = -u; y = v; z = -1D; }
                default -> throw new IllegalArgumentException("Invalid cube-map face " + face);
            }
            double inverseLength = 1D / Math.sqrt(x * x + y * y + z * z);
            return new Direction(x * inverseLength, y * inverseLength, z * inverseLength);
        }

        private static FaceCoordinate faceCoordinate(double x, double y, double z) {
            double absoluteX = Math.abs(x);
            double absoluteY = Math.abs(y);
            double absoluteZ = Math.abs(z);
            if (absoluteX >= absoluteY && absoluteX >= absoluteZ) {
                return x >= 0D
                        ? new FaceCoordinate(0, -z / absoluteX, y / absoluteX)
                        : new FaceCoordinate(1, z / absoluteX, y / absoluteX);
            }
            if (absoluteY >= absoluteZ) {
                return y >= 0D
                        ? new FaceCoordinate(2, x / absoluteY, -z / absoluteY)
                        : new FaceCoordinate(3, x / absoluteY, z / absoluteY);
            }
            return z >= 0D
                    ? new FaceCoordinate(4, x / absoluteZ, y / absoluteZ)
                    : new FaceCoordinate(5, -x / absoluteZ, y / absoluteZ);
        }
    }

    private record Direction(double x, double y, double z) {
    }

    private record FaceCoordinate(int face, double u, double v) {
    }

    private HbmHybridExplosionPlanner() {
    }
}
