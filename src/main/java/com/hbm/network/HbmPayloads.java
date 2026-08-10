package com.hbm.network;

import com.hbm.world.radiation.RadiationDiagnostics;
import com.hbm.world.radiation.RadiationManager;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class HbmPayloads {
    private static final String NETWORK_VERSION = "4";
    private static Consumer<PlayerRadiationPayload> clientRadiationHandler = ignored -> {
    };
    private static Consumer<ClientEffectPayload> clientEffectHandler = ignored -> {
    };
    private static Consumer<NuclearProgressPayload> clientNuclearProgressHandler = ignored -> {
    };

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(PlayerRadiationPayload.TYPE, PlayerRadiationPayload.STREAM_CODEC, HbmPayloads::handlePlayerRadiation);
        registrar.playToServer(MachineGuiActionPayload.TYPE, MachineGuiActionPayload.STREAM_CODEC, HbmPayloads::handleMachineGuiAction);
        registrar.playToClient(ClientEffectPayload.TYPE, ClientEffectPayload.STREAM_CODEC, HbmPayloads::handleClientEffect);
        registrar.playToClient(NuclearProgressPayload.TYPE, NuclearProgressPayload.STREAM_CODEC, HbmPayloads::handleNuclearProgress);
    }

    public static void syncRadiation(ServerPlayer player) {
        RadiationDiagnostics diagnostics = RadiationManager.getDiagnostics(player);
        PacketDistributor.sendToPlayer(player, new PlayerRadiationPayload(
                diagnostics.accumulatedRadiation(),
                diagnostics.totalRate(),
                diagnostics.inventoryRate(),
                diagnostics.blockRate(),
                diagnostics.falloutRate(),
                diagnostics.explosionRate(),
                diagnostics.lastExplosionDose(),
                diagnostics.resistance()
        ));
    }

    public static void setClientRadiationHandler(Consumer<PlayerRadiationPayload> handler) {
        clientRadiationHandler = handler;
    }

    public static void setClientEffectHandler(Consumer<ClientEffectPayload> handler) {
        clientEffectHandler = handler;
    }

    public static void setClientNuclearProgressHandler(Consumer<NuclearProgressPayload> handler) {
        clientNuclearProgressHandler = handler;
    }

    private static void handlePlayerRadiation(PlayerRadiationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientRadiationHandler.accept(payload));
    }

    private static void handleMachineGuiAction(MachineGuiActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer)) {
            context.disconnect(net.minecraft.network.chat.Component.literal("Invalid HBM machine GUI action context."));
        }
    }

    private static void handleClientEffect(ClientEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientEffectHandler.accept(payload));
    }

    private static void handleNuclearProgress(NuclearProgressPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientNuclearProgressHandler.accept(payload));
    }

    private HbmPayloads() {
    }
}
