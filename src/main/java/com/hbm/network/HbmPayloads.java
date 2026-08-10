package com.hbm.network;

import com.hbm.client.weapon.ClientWeaponController;
import com.hbm.weapon.HbmWeaponService;
import com.hbm.world.radiation.RadiationDiagnostics;
import com.hbm.world.radiation.RadiationManager;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class HbmPayloads {
    private static final String NETWORK_VERSION = "5";
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
        registrar.playToServer(WeaponInputPayload.TYPE, WeaponInputPayload.STREAM_CODEC, HbmPayloads::handleWeaponInput);
        registrar.playToServer(WeaponCommandPayload.TYPE, WeaponCommandPayload.STREAM_CODEC, HbmPayloads::handleWeaponCommand);
        registrar.playToClient(WeaponStatePayload.TYPE, WeaponStatePayload.STREAM_CODEC, HbmPayloads::handleWeaponState);
        registrar.playToClient(WeaponEffectPayload.TYPE, WeaponEffectPayload.STREAM_CODEC, HbmPayloads::handleWeaponEffect);
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

    private static void handleWeaponInput(WeaponInputPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            context.disconnect(Component.literal("Invalid HBM weapon input context."));
            return;
        }
        HbmWeaponService.acceptInput(player, payload);
    }

    private static void handleWeaponCommand(WeaponCommandPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            context.disconnect(Component.literal("Invalid HBM weapon command context."));
            return;
        }
        HbmWeaponService.acceptCommand(player, payload);
    }

    private static void handleWeaponState(WeaponStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientWeaponController.acceptState(payload);
        }
    }

    private static void handleWeaponEffect(WeaponEffectPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientWeaponController.acceptEffect(payload);
        }
    }

    private HbmPayloads() {
    }
}
