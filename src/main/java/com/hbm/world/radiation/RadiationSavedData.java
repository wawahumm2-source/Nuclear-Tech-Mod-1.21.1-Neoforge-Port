package com.hbm.world.radiation;

import com.hbm.config.HbmConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

public final class RadiationSavedData extends SavedData {
    private static final String DATA_NAME = "hbm_radiation";
    private final Map<UUID, Double> playerExposure = new HashMap<>();
    private final Map<Long, Double> chunkFallout = new HashMap<>();

    public static final Factory<RadiationSavedData> FACTORY = new Factory<>(
            RadiationSavedData::new,
            RadiationSavedData::load,
            DataFixTypes.LEVEL
    );

    public static RadiationSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static RadiationSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        RadiationSavedData data = new RadiationSavedData();

        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerTag = players.getCompound(i);
            data.playerExposure.put(playerTag.getUUID("UUID"), playerTag.getDouble("Exposure"));
        }

        ListTag chunks = tag.getList("Chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunks.size(); i++) {
            CompoundTag chunkTag = chunks.getCompound(i);
            data.chunkFallout.put(chunkTag.getLong("Chunk"), chunkTag.getDouble("Fallout"));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag players = new ListTag();
        this.playerExposure.forEach((uuid, exposure) -> {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("UUID", uuid);
            playerTag.putDouble("Exposure", exposure);
            players.add(playerTag);
        });
        tag.put("Players", players);

        ListTag chunks = new ListTag();
        this.chunkFallout.forEach((chunk, fallout) -> {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("Chunk", chunk);
            chunkTag.putDouble("Fallout", fallout);
            chunks.add(chunkTag);
        });
        tag.put("Chunks", chunks);
        return tag;
    }

    public double getExposure(UUID playerId) {
        return this.playerExposure.getOrDefault(playerId, 0D);
    }

    public double setExposure(UUID playerId, double exposure) {
        double next = Mth.clamp(exposure, 0D, HbmConfig.RADIATION_MAX_EXPOSURE.get());
        if (next <= 0D) {
            this.playerExposure.remove(playerId);
        } else {
            this.playerExposure.put(playerId, next);
        }
        setDirty();
        return next;
    }

    public double addExposure(UUID playerId, double amount) {
        return setExposure(playerId, getExposure(playerId) + amount);
    }

    public double removeExposure(UUID playerId, double amount) {
        return addExposure(playerId, -amount);
    }

    public void clearExposure(UUID playerId) {
        if (this.playerExposure.remove(playerId) != null) {
            setDirty();
        }
    }

    public double getChunkFallout(ChunkPos chunkPos) {
        return this.chunkFallout.getOrDefault(chunkPos.toLong(), 0D);
    }

    public double setChunkFallout(ChunkPos chunkPos, double fallout) {
        long key = chunkPos.toLong();
        double next = Math.max(0D, fallout);
        if (next <= 0D) {
            this.chunkFallout.remove(key);
        } else {
            this.chunkFallout.put(key, next);
        }
        setDirty();
        return next;
    }

    public double addChunkFallout(ChunkPos chunkPos, double amount) {
        return setChunkFallout(chunkPos, getChunkFallout(chunkPos) + amount);
    }

    public void addFallout(ChunkPos chunkPos, double amount) {
        addChunkFallout(chunkPos, amount);
    }

    public double removeChunkFallout(ChunkPos chunkPos, double amount) {
        return addChunkFallout(chunkPos, -amount);
    }

    public void clearChunkFallout(ChunkPos chunkPos) {
        if (this.chunkFallout.remove(chunkPos.toLong()) != null) {
            setDirty();
        }
    }

    public void tickFalloutDecay(double amount) {
        if (amount <= 0D || this.chunkFallout.isEmpty()) {
            return;
        }

        this.chunkFallout.replaceAll((chunk, fallout) -> Math.max(0D, fallout - amount));
        this.chunkFallout.entrySet().removeIf(entry -> entry.getValue() <= 0D);
        setDirty();
    }
}
