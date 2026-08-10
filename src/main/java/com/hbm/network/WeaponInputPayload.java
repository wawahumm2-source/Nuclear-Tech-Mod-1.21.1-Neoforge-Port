package com.hbm.network;

import com.hbm.HbmNuclearTech;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WeaponInputPayload(WeaponInput input, boolean pressed, int sequence) implements CustomPacketPayload {
    public static final Type<WeaponInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "weapon_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeEnum(payload.input());
                buffer.writeBoolean(payload.pressed());
                buffer.writeVarInt(payload.sequence());
            },
            buffer -> new WeaponInputPayload(
                    buffer.readEnum(WeaponInput.class),
                    buffer.readBoolean(),
                    buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
