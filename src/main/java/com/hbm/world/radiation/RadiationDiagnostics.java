package com.hbm.world.radiation;

public record RadiationDiagnostics(
        double accumulatedRadiation,
        double totalRate,
        double inventoryRate,
        double blockRate,
        double falloutRate,
        double explosionRate,
        double lastExplosionDose,
        double dimensionRate,
        double scriptedRate,
        double resistance,
        double protectionMultiplier
) {
    public static final RadiationDiagnostics EMPTY = new RadiationDiagnostics(0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 1D);
}
