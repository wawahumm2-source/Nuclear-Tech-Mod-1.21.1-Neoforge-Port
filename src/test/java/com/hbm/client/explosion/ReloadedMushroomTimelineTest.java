package com.hbm.client.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReloadedMushroomTimelineTest {
    @Test
    void littleBoyUsesReloadedTextureThresholds() {
        assertEquals(0, ReloadedMushroomTimeline.textureStage(0F, 120F));
        assertEquals(1, ReloadedMushroomTimeline.textureStage(100F, 120F));
        assertEquals(9, ReloadedMushroomTimeline.textureStage(3000F, 120F));
        assertEquals(10, ReloadedMushroomTimeline.textureStage(5000F, 120F));
    }

    @Test
    void sourceRadiusControlsTheMeshScale() {
        assertEquals(3F, ReloadedMushroomTimeline.modelScale(120F), 0.0001F);
        assertEquals(0.375F, ReloadedMushroomTimeline.modelScale(15F), 0.0001F);
    }

    @Test
    void capWidensDuringTheFirstHundredTicks() {
        assertEquals(0.7F, ReloadedMushroomTimeline.headWidth(0F), 0.0001F);
        assertEquals(0.85F, ReloadedMushroomTimeline.headWidth(50F), 0.0001F);
        assertEquals(1F, ReloadedMushroomTimeline.headWidth(100F), 0.0001F);
        assertEquals(1F, ReloadedMushroomTimeline.headWidth(500F), 0.0001F);
    }

    @Test
    void textureKeepsRisingButSlowsWithAge() {
        float earlyPerTick = ReloadedMushroomTimeline.textureScroll(100F, 10172) / 100F;
        float latePerTick = ReloadedMushroomTimeline.textureScroll(10172F, 10172) / 10172F;

        assertTrue(earlyPerTick > latePerTick);
        assertEquals(0.00528F, latePerTick, 0.0001F);
    }

    @Test
    void modernFinalFadeRemainsBounded() {
        assertEquals(1F, ReloadedMushroomTimeline.alpha(9155F, 9155, 10172), 0F);
        assertEquals(0F, ReloadedMushroomTimeline.alpha(10172F, 9155, 10172), 0F);
        assertEquals(0.5F, ReloadedMushroomTimeline.alpha(9663.5F, 9155, 10172), 0.0001F);
    }
}
