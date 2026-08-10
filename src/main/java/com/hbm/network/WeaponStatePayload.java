package com.hbm.network;

import com.hbm.HbmNuclearTech;
import com.hbm.weapon.state.GunState;
import com.hbm.weapon.state.ReloadPhase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WeaponStatePayload(
        int acknowledgedSequence,
        GunState state,
        boolean ads,
        float adsFovMultiplier,
        float adsSensitivityMultiplier,
        float recoilRecoveryPerTick,
        ReloadPhase reloadPhase,
        int actionTicks
) implements CustomPacketPayload {
    public static final Type<WeaponStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "weapon_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.acknowledgedSequence());
                GunState.STREAM_CODEC.encode(buffer, payload.state());
                buffer.writeBoolean(payload.ads());
                buffer.writeFloat(payload.adsFovMultiplier());
                buffer.writeFloat(payload.adsSensitivityMultiplier());
                buffer.writeFloat(payload.recoilRecoveryPerTick());
                buffer.writeEnum(payload.reloadPhase());
                buffer.writeVarInt(payload.actionTicks());
            },
            buffer -> new WeaponStatePayload(
                    buffer.readVarInt(),
                    GunState.STREAM_CODEC.decode(buffer),
                    buffer.readBoolean(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readEnum(ReloadPhase.class),
                    buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
