package com.hbm.world.radiation;

import com.hbm.config.HbmConfig;

public enum RadiationEffectBand {
    NONE,
    SICK,
    MODERATE,
    SEVERE,
    CRITICAL,
    FATAL;

    public static RadiationEffectBand fromRadiation(double radiation) {
        if (radiation >= HbmConfig.RADIATION.fatalEffectThreshold.get()) {
            return FATAL;
        }
        if (radiation >= HbmConfig.RADIATION.criticalEffectThreshold.get()) {
            return CRITICAL;
        }
        if (radiation >= HbmConfig.RADIATION.severeEffectThreshold.get()) {
            return SEVERE;
        }
        if (radiation >= HbmConfig.RADIATION.moderateEffectThreshold.get()) {
            return MODERATE;
        }
        if (radiation >= HbmConfig.RADIATION.effectThreshold.get()) {
            return SICK;
        }
        return NONE;
    }
}
