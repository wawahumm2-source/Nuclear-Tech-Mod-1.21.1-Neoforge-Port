package com.hbm.client.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExtendedTorexTimelineTest {
    @Test
    void sourceScaleUsesRadiusAndClampsAtBothEnds() {
        assertEquals(0.25F, ExtendedTorexTimeline.scale(15F), 0F);
        assertEquals(1.2F, ExtendedTorexTimeline.scale(120F), 0.0001F);
        assertEquals(5F, ExtendedTorexTimeline.scale(1_000F), 0F);
    }

    @Test
    void littleBoyUsesExtendedSourceLifetime() {
        float scale = ExtendedTorexTimeline.scale(120F);

        assertEquals(1_079, ExtendedTorexTimeline.maxAge(scale));
        assertEquals(225, ExtendedTorexTimeline.maxAge(ExtendedTorexTimeline.scale(15F)));
    }

    @Test
    void cloudletLifetimeFollowsSourceEnvelope() {
        assertEquals(201, ExtendedTorexTimeline.cloudletLifetime(1, 1_079));
        assertEquals(279, ExtendedTorexTimeline.cloudletLifetime(1_000, 1_079));
    }

    @Test
    void simulationRunsFullSpeedForFirstQuarterThenSlowsToZero() {
        assertEquals(1D, ExtendedTorexTimeline.simulationSpeed(270, 1_080), 0D);
        assertEquals(0.5D, ExtendedTorexTimeline.simulationSpeed(675, 1_080), 0.000001D);
        assertEquals(0D, ExtendedTorexTimeline.simulationSpeed(1_080, 1_080), 0D);
    }

    @Test
    void parentCloudFadesDuringFinalQuarter() {
        assertEquals(1F, ExtendedTorexTimeline.parentAlpha(810, 1_080), 0F);
        assertEquals(0.5F, ExtendedTorexTimeline.parentAlpha(945, 1_080), 0.0001F);
        assertEquals(0F, ExtendedTorexTimeline.parentAlpha(1_080, 1_080), 0F);
    }

    @Test
    void flashAndFlareDurationsScaleWithYield() {
        assertEquals(36F, ExtendedTorexTimeline.flashDuration(1.2F), 0.0001F);
        assertEquals(120F, ExtendedTorexTimeline.flareDuration(1.2F), 0.0001F);
        assertEquals(7.5F, ExtendedTorexTimeline.flashDuration(0.25F), 0F);
    }
}
