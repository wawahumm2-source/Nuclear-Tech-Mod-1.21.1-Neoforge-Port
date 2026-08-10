package com.hbm.world.explosion;

/** Pure arithmetic shared by the persisted chunk scheduler and its unit tests. */
public final class HbmChunkSchedulerMath {
    private HbmChunkSchedulerMath() {
    }

    public static long packChunk(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    public static int unpackChunkX(long chunkKey) {
        return (int) chunkKey;
    }

    public static int unpackChunkZ(long chunkKey) {
        return (int) (chunkKey >>> 32);
    }

    public static boolean isInsideHorizontalRadius(int originX, int originZ, int x, int z, int radius) {
        long dx = x - originX;
        long dz = z - originZ;
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    public static int localTargetBitIndex(int localX, int yOffset, int localZ) {
        return (yOffset << 8) | ((localX & 15) << 4) | (localZ & 15);
    }
}
