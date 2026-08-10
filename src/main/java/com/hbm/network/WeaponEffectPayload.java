package com.hbm.network;

import com.hbm.HbmNuclearTech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WeaponEffectPayload(
        WeaponEffectType effect,
        ResourceLocation gunId,
        ResourceLocation resource,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        int sourceEntityId,
        int variant
) implements CustomPacketPayload {
    public static final Type<WeaponEffectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "weapon_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponEffectPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeEnum(payload.effect());
                buffer.writeResourceLocation(payload.gunId());
                buffer.writeResourceLocation(payload.resource());
                buffer.writeDouble(payload.x());
                buffer.writeDouble(payload.y());
                buffer.writeDouble(payload.z());
                buffer.writeFloat(payload.yaw());
                buffer.writeFloat(payload.pitch());
                buffer.writeVarInt(payload.sourceEntityId());
                buffer.writeVarInt(payload.variant());
            },
            buffer -> new WeaponEffectPayload(
                    buffer.readEnum(WeaponEffectType.class),
                    buffer.readResourceLocation(),
                    buffer.readResourceLocation(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readVarInt(),
                    buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
