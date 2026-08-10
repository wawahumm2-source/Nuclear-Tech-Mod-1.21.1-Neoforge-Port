package com.hbm.client.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NuclearImpactTimingTest {
    @Test
    void flashIsImmediateThenDecays() {
        assertEquals(1F, NuclearImpactTiming.flashEnvelope(0F));
        assertEquals(1F, NuclearImpactTiming.flashEnvelope(NuclearImpactTiming.FLASH_HARD_WHITE_TICKS));
        assertTrue(NuclearImpactTiming.flashEnvelope(NuclearImpactTiming.FLASH_HOLD_TICKS) < 1F);
        assertEquals(0F, NuclearImpactTiming.flashEnvelope(NuclearImpactTiming.FLASH_DURATION_TICKS));
    }

    @Test
    void configuredBlindnessKeepsTheImpactBrightThenFades() {
        int holdTicks = 40;
        int fadeTicks = 60;
        int fadeStart = NuclearImpactTiming.FLASH_RAMP_TICKS + holdTicks;

        assertEquals(101, NuclearImpactTiming.flashDurationTicks(holdTicks, fadeTicks));
        assertTrue(NuclearImpactTiming.flashEnvelope(fadeStart - 1F, holdTicks, fadeTicks) < 0.5F);
        assertEquals(0.19F, NuclearImpactTiming.flashEnvelope(fadeStart + 30F, holdTicks, fadeTicks), 0.0001F);
        assertEquals(0F, NuclearImpactTiming.flashEnvelope(101F, holdTicks, fadeTicks));
    }

    @Test
    void shockHasASharpImpactAndShortRumble() {
        assertTrue(NuclearImpactTiming.shakeEnvelope(0F) > NuclearImpactTiming.shakeEnvelope(8F));
        assertTrue(NuclearImpactTiming.shakeEnvelope(8F) > 0F);
        assertEquals(0F, NuclearImpactTiming.shakeEnvelope(NuclearImpactTiming.SHAKE_DURATION_TICKS));
    }

    @Test
    void shakeOscillatesAroundZeroInsteadOfDriftingOneDirection() {
        boolean positiveLateral = false;
        boolean negativeLateral = false;
        boolean positiveYaw = false;
        boolean negativeYaw = false;
        for (int age = 0; age < NuclearImpactTiming.SHAKE_DURATION_TICKS; age++) {
            NuclearImpactTiming.ShakeSample sample = NuclearImpactTiming.shakeSample(age);
            positiveLateral |= sample.lateral() > 0F;
            negativeLateral |= sample.lateral() < 0F;
            positiveYaw |= sample.yaw() > 0F;
            negativeYaw |= sample.yaw() < 0F;
        }
        assertTrue(positiveLateral && negativeLateral);
        assertTrue(positiveYaw && negativeYaw);
    }
}
