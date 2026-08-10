package com.hbm.world.explosion;

import com.hbm.config.HbmConfig;
import net.minecraft.nbt.CompoundTag;

/** Immutable fallout settings captured at detonation time. */
public record HbmFalloutProfile(
        int radius,
        int processedDepth,
        int woodEffectPercent,
        int fireChanceDenominator,
        boolean terrainTransformation,
        boolean falloutDeposits,
        boolean firePlacement,
        double outerRadiationRate,
        double craterRadiationRate,
        double innerRadiationRate
) {
    public static HbmFalloutProfile none() {
        return new HbmFalloutProfile(0, 0, 0, 0, false, false, false, 0D, 0D, 0D);
    }

    public static HbmFalloutProfile mk5(int bombRadius) {
        int radius = Math.max(1, (int) Math.round(bombRadius * HbmConfig.FALLOUT.nuclearFalloutRadiusMultiplier.get()));
        return new HbmFalloutProfile(
                radius,
                HbmConfig.FALLOUT.nuclearFalloutDepth.get(),
                HbmConfig.FALLOUT.nuclearWoodEffectPercent.get(),
                HbmConfig.FALLOUT.nuclearFireChanceDenominator.get(),
                HbmConfig.FALLOUT.enableNuclearTerrainTransformation.get(),
                HbmConfig.FALLOUT.enableNuclearFalloutDeposits.get(),
                HbmConfig.FALLOUT.enableNuclearWildfire.get(),
                HbmConfig.RADIATION.enableCraterRadiation.get() ? HbmConfig.RADIATION.craterOuterRadiationRate.get() : 0D,
                HbmConfig.RADIATION.enableCraterRadiation.get() ? HbmConfig.RADIATION.craterRadiationRate.get() : 0D,
                HbmConfig.RADIATION.enableCraterRadiation.get() ? HbmConfig.RADIATION.craterInnerRadiationRate.get() : 0D
        );
    }

    public boolean isEnabled() {
        return this.radius > 0;
    }

    void save(CompoundTag tag) {
        tag.putInt("FalloutRadius", this.radius);
        tag.putInt("FalloutDepth", this.processedDepth);
        tag.putInt("FalloutWoodPercent", this.woodEffectPercent);
        tag.putInt("FalloutFireChance", this.fireChanceDenominator);
        tag.putBoolean("FalloutTerrain", this.terrainTransformation);
        tag.putBoolean("FalloutDeposits", this.falloutDeposits);
        tag.putBoolean("FalloutFire", this.firePlacement);
        tag.putDouble("FalloutOuterRadiation", this.outerRadiationRate);
        tag.putDouble("FalloutCraterRadiation", this.craterRadiationRate);
        tag.putDouble("FalloutInnerRadiation", this.innerRadiationRate);
    }

    static HbmFalloutProfile load(CompoundTag tag) {
        if (!tag.contains("FalloutRadius")) {
            return none();
        }
        return new HbmFalloutProfile(
                tag.getInt("FalloutRadius"),
                tag.getInt("FalloutDepth"),
                tag.getInt("FalloutWoodPercent"),
                tag.getInt("FalloutFireChance"),
                tag.getBoolean("FalloutTerrain"),
                tag.getBoolean("FalloutDeposits"),
                tag.getBoolean("FalloutFire"),
                tag.getDouble("FalloutOuterRadiation"),
                tag.getDouble("FalloutCraterRadiation"),
                tag.getDouble("FalloutInnerRadiation")
        );
    }
}
