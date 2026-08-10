package com.hbm.network;

import com.hbm.HbmNuclearTech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayerRadiationPayload(
        double exposure,
        double environmentRate,
        double inventoryRate,
        double blockRate,
        double falloutRate,
        double explosionRate,
        double lastExplosionDose,
        double resistance
) implements CustomPacketPayload {
    public static final Type<PlayerRadiationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "player_radiation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerRadiationPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeDouble(payload.exposure());
                buffer.writeDouble(payload.environmentRate());
                buffer.writeDouble(payload.inventoryRate());
                buffer.writeDouble(payload.blockRate());
                buffer.writeDouble(payload.falloutRate());
                buffer.writeDouble(payload.explosionRate());
                buffer.writeDouble(payload.lastExplosionDose());
                buffer.writeDouble(payload.resistance());
            },
            buffer -> new PlayerRadiationPayload(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
