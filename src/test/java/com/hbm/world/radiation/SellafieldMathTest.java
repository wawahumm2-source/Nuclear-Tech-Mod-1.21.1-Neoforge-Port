package com.hbm.world.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SellafieldMathTest {
    @Test
    void slakedStagesUseTheSourceMetadataBrightnessCurve() {
        assertEquals(0xFFFFFF, SellafieldMath.slakedColor(0));
        assertEquals(0xEEEEEE, SellafieldMath.slakedColor(1));
        assertEquals(0x999999, SellafieldMath.slakedColor(6));
        assertEquals(0x666666, SellafieldMath.slakedColor(9));
    }

    @Test
    void slakedStageInputIsClampedToTheModernStateRange() {
        assertEquals(0xFFFFFF, SellafieldMath.slakedColor(-1));
        assertEquals(0x666666, SellafieldMath.slakedColor(10));
    }
}
