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
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /**
     * The alpha stored player radiation in the overworld SavedData. Keep it readable until a player has moved to the
     * persistent player attachment, then remove only that legacy entry.
     */
    public static double getLegacyExposure(ServerLevel level, UUID playerId) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME).getExposure(playerId);
    }

    public static void consumeLegacyExposure(ServerLevel level, UUID playerId) {
        level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME).clearExposure(playerId);
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
        double next = Mth.clamp(fallout, 0D, 100_000D);
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

    /**
     * Source-compatible simple chunk field: radiation spreads once per configured interval, with 60% retained in the
     * origin, 7.5% sent to cardinal neighbors, and 2.5% sent to diagonal neighbors by default.
     */
    public void tickChunkRadiationField() {
        if (this.chunkFallout.isEmpty()) {
            return;
        }

        Map<Long, Double> previous = new HashMap<>(this.chunkFallout);
        Map<Long, Double> next = new HashMap<>();
        for (Map.Entry<Long, Double> entry : previous.entrySet()) {
            if (entry.getValue() <= 0D) {
                continue;
            }

            ChunkPos source = new ChunkPos(entry.getKey());
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int zOffset = -1; zOffset <= 1; zOffset++) {
                    int distance = Math.abs(xOffset) + Math.abs(zOffset);
                    double share = distance == 0
                            ? HbmConfig.RADIATION.chunkSpreadCenter.get()
                            : distance == 1
                            ? HbmConfig.RADIATION.chunkSpreadCardinal.get()
                            : HbmConfig.RADIATION.chunkSpreadDiagonal.get();
                    if (share <= 0D) {
                        continue;
                    }
                    long target = ChunkPos.asLong(source.x + xOffset, source.z + zOffset);
                    next.merge(target, entry.getValue() * share, Double::sum);
                }
            }
        }

        this.chunkFallout.clear();
        for (Map.Entry<Long, Double> entry : next.entrySet()) {
            double value = entry.getValue();
            if (previous.containsKey(entry.getKey())) {
                value = value * HbmConfig.RADIATION.chunkSpreadDecay.get() - HbmConfig.RADIATION.chunkSpreadFloor.get();
            }
            if (value > 0.00001D) {
                this.chunkFallout.put(entry.getKey(), Mth.clamp(value, 0D, 100_000D));
            }
        }
        setDirty();
    }
}
