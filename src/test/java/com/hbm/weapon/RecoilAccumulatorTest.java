package com.hbm.weapon;

import com.hbm.weapon.state.RecoilAccumulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecoilAccumulatorTest {
    @Test
    void recoveryIsBoundedAndReturnsToZero() {
        RecoilAccumulator recoil = new RecoilAccumulator();
        recoil.add(1.0F, -0.5F);

        RecoilAccumulator.Recovery first = recoil.recover(0.4F);
        assertEquals(0.4F, first.pitch(), 1.0E-6F);
        assertEquals(-0.26F, first.yaw(), 1.0E-6F);
        assertEquals(0.6F, recoil.pitchDebt(), 1.0E-6F);
        assertEquals(-0.24F, recoil.yawDebt(), 1.0E-6F);

        recoil.recover(1.0F);
        assertEquals(0.0F, recoil.pitchDebt(), 1.0E-6F);
        assertEquals(0.0F, recoil.yawDebt(), 1.0E-6F);
    }
}
