package com.hbm.network;

import com.hbm.world.radiation.RadiationSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class HbmPayloads {
    private static final String NETWORK_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(PlayerRadiationPayload.TYPE, PlayerRadiationPayload.STREAM_CODEC, HbmPayloads::handlePlayerRadiation);
        registrar.playToServer(MachineGuiActionPayload.TYPE, MachineGuiActionPayload.STREAM_CODEC, HbmPayloads::handleMachineGuiAction);
        registrar.playToClient(ClientEffectPayload.TYPE, ClientEffectPayload.STREAM_CODEC, HbmPayloads::handleClientEffect);
    }

    public static void syncRadiation(ServerPlayer player) {
        RadiationSavedData data = RadiationSavedData.get(player.serverLevel());
        PacketDistributor.sendToPlayer(player, new PlayerRadiationPayload(
                data.getExposure(player.getUUID()),
                data.getChunkFallout(new ChunkPos(player.blockPosition()))
        ));
    }

    private static void handlePlayerRadiation(PlayerRadiationPayload payload, IPayloadContext context) {
        // Client HUD integration will read this payload after the HUD parity pass.
    }

    private static void handleMachineGuiAction(MachineGuiActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer)) {
            context.disconnect(net.minecraft.network.chat.Component.literal("Invalid HBM machine GUI action context."));
        }
    }

    private static void handleClientEffect(ClientEffectPayload payload, IPayloadContext context) {
        // Client effect dispatch is intentionally inert until source-parity visual events are ported.
    }

    private HbmPayloads() {
    }
}
