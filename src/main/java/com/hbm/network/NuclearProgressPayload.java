package com.hbm.network;

import com.hbm.HbmNuclearTech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Rate-limited server progress for the visible, bounded large-nuclear terrain job. */
public record NuclearProgressPayload(long origin, Stage stage, int percent, int radius) implements CustomPacketPayload {
    public static final Type<NuclearProgressPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "nuclear_progress")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, NuclearProgressPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.origin());
                buffer.writeByte(payload.stage().ordinal());
                buffer.writeVarInt(Math.clamp(payload.percent(), 0, 100));
                buffer.writeVarInt(Math.max(0, payload.radius()));
            },
            buffer -> new NuclearProgressPayload(
                    buffer.readLong(),
                    Stage.fromId(buffer.readByte()),
                    Math.clamp(buffer.readVarInt(), 0, 100),
                    buffer.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Stage {
        CALCULATING,
        EXCAVATING,
        FALLOUT,
        CONVERTING,
        VAPORIZING;

        private static Stage fromId(int id) {
            Stage[] values = values();
            return id >= 0 && id < values.length ? values[id] : CALCULATING;
        }
    }
}
