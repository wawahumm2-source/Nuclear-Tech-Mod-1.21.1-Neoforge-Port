package com.hbm.network;

import com.hbm.HbmNuclearTech;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MachineGuiActionPayload(BlockPos pos, String action, int value) implements CustomPacketPayload {
    public static final Type<MachineGuiActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "machine_gui_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineGuiActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBlockPos(payload.pos());
                buffer.writeUtf(payload.action(), 64);
                buffer.writeVarInt(payload.value());
            },
            buffer -> new MachineGuiActionPayload(buffer.readBlockPos(), buffer.readUtf(64), buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
