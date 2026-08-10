package com.hbm.world.radiation;

import com.hbm.config.HbmConfig;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent living-entity radiation state. Rates are retained for diagnostics but do not need disk persistence. */
public final class RadiationPlayerState implements INBTSerializable<CompoundTag> {
    private final EnumMap<RadiationSourceType, Double> recentRates = new EnumMap<>(RadiationSourceType.class);
    private final EnumMap<RadiationSourceType, Double> lastDirectDoses = new EnumMap<>(RadiationSourceType.class);
    private double radiation;
    private double digamma;
    private double environmentRate;
    private final RadiationTreatment treatment = new RadiationTreatment();
    private int radXTicks;

    public RadiationPlayerState() {
        for (RadiationSourceType type : RadiationSourceType.values()) {
            this.recentRates.put(type, 0D);
            this.lastDirectDoses.put(type, 0D);
        }
    }

    public double getRadiation() {
        return radiation;
    }

    public double setRadiation(double radiation) {
        this.radiation = RadiationMath.clamp(
                radiation,
                RadiationMath.effectiveMaxExposure(
                        HbmConfig.RADIATION.maxExposure.get(),
                        HbmConfig.RADIATION.fatalEffectThreshold.get()
                )
        );
        return this.radiation;
    }

    public double addRadiation(double amount) {
        return setRadiation(this.radiation + amount);
    }

    public double removeRadiation(double amount) {
        return addRadiation(-Math.max(0D, amount));
    }

    public double getDigamma() {
        return digamma;
    }

    public void setDigamma(double digamma) {
        this.digamma = Math.max(0D, digamma);
    }

    public double getEnvironmentRate() {
        return environmentRate;
    }

    public double getRate(RadiationSourceType type) {
        return this.recentRates.getOrDefault(type, 0D);
    }

    public double getTotalRate() {
        return this.recentRates.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getLastDirectDose(RadiationSourceType type) {
        return this.lastDirectDoses.getOrDefault(type, 0D);
    }

    public void recordDirectDose(RadiationSourceType type, double amount) {
        this.lastDirectDoses.put(type, Math.max(0D, amount));
    }

    public void setRecentRates(Map<RadiationSourceType, Double> rates) {
        double environment = 0D;
        for (RadiationSourceType type : RadiationSourceType.values()) {
            double rate = Math.max(0D, rates.getOrDefault(type, 0D));
            this.recentRates.put(type, rate);
            environment += rate;
        }
        this.environmentRate = environment;
    }

    public void startTreatment(int ticks, double perTick) {
        this.treatment.start(ticks, perTick);
    }

    public double tickTreatment(int elapsedTicks) {
        return this.treatment.tick(elapsedTicks);
    }

    public void startRadX(int ticks) {
        this.radXTicks = Math.max(this.radXTicks, Math.max(0, ticks));
    }

    public boolean hasRadX() {
        return this.radXTicks > 0;
    }

    public void tickRadX(int elapsedTicks) {
        this.radXTicks = Math.max(0, this.radXTicks - Math.max(0, elapsedTicks));
    }

    public void cancelTreatmentAndRadX() {
        this.treatment.clear();
        this.radXTicks = 0;
    }

    public void clear() {
        this.radiation = 0D;
        this.digamma = 0D;
        this.environmentRate = 0D;
        this.treatment.clear();
        this.radXTicks = 0;
        for (RadiationSourceType type : RadiationSourceType.values()) {
            this.recentRates.put(type, 0D);
            this.lastDirectDoses.put(type, 0D);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Radiation", this.radiation);
        tag.putDouble("Digamma", this.digamma);
        tag.putInt("TreatmentTicks", this.treatment.remainingTicks());
        tag.putDouble("TreatmentPerTick", this.treatment.perTick());
        tag.putInt("RadXTicks", this.radXTicks);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        setRadiation(tag.getDouble("Radiation"));
        this.digamma = Math.max(0D, tag.getDouble("Digamma"));
        this.treatment.restore(tag.getInt("TreatmentTicks"), tag.getDouble("TreatmentPerTick"));
        this.radXTicks = Math.max(0, tag.getInt("RadXTicks"));
    }
}
