package com.hbm.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hbm.world.radiation.SellafieldMath;
import org.junit.jupiter.api.Test;

class SellafieldBlockTest {
    @Test
    void itemRadiationMatchesTierOneHazardRegistry() {
        assertEquals(0.5D, SellafieldMath.itemRadiation(0));
        assertEquals(1D, SellafieldMath.itemRadiation(1));
        assertEquals(2.5D, SellafieldMath.itemRadiation(2));
        assertEquals(4D, SellafieldMath.itemRadiation(3));
        assertEquals(5D, SellafieldMath.itemRadiation(4));
        assertEquals(10D, SellafieldMath.itemRadiation(5));
    }

    @Test
    void invalidMetadataClampsToSourceTierBounds() {
        assertEquals(0.5D, SellafieldMath.itemRadiation(-10));
        assertEquals(10D, SellafieldMath.itemRadiation(99));
    }
}
