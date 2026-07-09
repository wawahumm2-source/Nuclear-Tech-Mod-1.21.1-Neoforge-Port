package com.hbm.world.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class ChunkRadiationService {
    public static double getRadiation(ServerLevel level, BlockPos pos) {
        return getRadiation(level, new ChunkPos(pos));
    }

    public static double getRadiation(ServerLevel level, ChunkPos chunkPos) {
        return RadiationSavedData.get(level).getChunkFallout(chunkPos);
    }

    public static double setRadiation(ServerLevel level, BlockPos pos, double radiation) {
        return setRadiation(level, new ChunkPos(pos), radiation);
    }

    public static double setRadiation(ServerLevel level, ChunkPos chunkPos, double radiation) {
        return RadiationSavedData.get(level).setChunkFallout(chunkPos, radiation);
    }

    public static double incrementRad(ServerLevel level, BlockPos pos, double radiation) {
        return incrementRad(level, new ChunkPos(pos), radiation);
    }

    public static double incrementRad(ServerLevel level, ChunkPos chunkPos, double radiation) {
        return RadiationSavedData.get(level).addChunkFallout(chunkPos, Math.max(0D, radiation));
    }

    public static double decrementRad(ServerLevel level, BlockPos pos, double radiation) {
        return decrementRad(level, new ChunkPos(pos), radiation);
    }

    public static double decrementRad(ServerLevel level, ChunkPos chunkPos, double radiation) {
        return RadiationSavedData.get(level).removeChunkFallout(chunkPos, Math.max(0D, radiation));
    }

    private ChunkRadiationService() {
    }
}
