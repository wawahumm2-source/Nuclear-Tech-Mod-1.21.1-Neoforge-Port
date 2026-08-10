package com.hbm.client.radiation;

import com.hbm.network.PlayerRadiationPayload;
import com.hbm.world.radiation.RadiationDiagnostics;

public final class RadiationClientState {
    private static RadiationDiagnostics diagnostics = RadiationDiagnostics.EMPTY;

    public static void accept(PlayerRadiationPayload payload) {
        diagnostics = new RadiationDiagnostics(
                payload.exposure(),
                payload.environmentRate(),
                payload.inventoryRate(),
                payload.blockRate(),
                payload.falloutRate(),
                payload.explosionRate(),
                payload.lastExplosionDose(),
                0D,
                0D,
                payload.resistance(),
                Math.pow(10D, -payload.resistance())
        );
    }

    public static RadiationDiagnostics diagnostics() {
        return diagnostics;
    }

    private RadiationClientState() {
    }
}
