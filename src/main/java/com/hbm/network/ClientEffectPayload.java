package com.hbm.network;

import com.hbm.HbmNuclearTech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientEffectPayload(ResourceLocation effect, double x, double y, double z, int variant) implements CustomPacketPayload {
    public static final Type<ClientEffectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "client_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientEffectPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.effect().toString(), 128);
                buffer.writeDouble(payload.x());
                buffer.writeDouble(payload.y());
                buffer.writeDouble(payload.z());
                buffer.writeVarInt(payload.variant());
            },
            buffer -> new ClientEffectPayload(ResourceLocation.parse(buffer.readUtf(128)), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
