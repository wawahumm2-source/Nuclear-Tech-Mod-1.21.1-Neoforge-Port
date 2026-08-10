package com.hbm.network;

import com.hbm.HbmNuclearTech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WeaponCommandPayload(WeaponCommand command, int sequence) implements CustomPacketPayload {
    public static final Type<WeaponCommandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "weapon_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponCommandPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeEnum(payload.command());
                buffer.writeVarInt(payload.sequence());
            },
            buffer -> new WeaponCommandPayload(buffer.readEnum(WeaponCommand.class), buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
