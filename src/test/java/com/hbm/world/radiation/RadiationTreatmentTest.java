package com.hbm.world.radiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadiationTreatmentTest {
    @Test
    void normalRadAwayTreatmentMatchesTheSourceTotal() {
        RadiationTreatment treatment = new RadiationTreatment();
        treatment.start(14, 10D);

        double total = 0D;
        for (int tick = 0; tick < 14; tick++) {
            total += treatment.tick();
        }

        assertEquals(140D, total, 0.000001D);
        assertEquals(0D, treatment.tick(), 0.000001D);
    }

    @Test
    void treatmentNeverProducesNegativeDuration() {
        RadiationTreatment treatment = new RadiationTreatment();
        treatment.start(1, 20D);
        assertTrue(treatment.remainingTicks() > 0);
        treatment.tick();
        assertEquals(0, treatment.remainingTicks());
        assertEquals(0D, treatment.tick(), 0.000001D);
        assertFalse(treatment.remainingTicks() < 0);
    }

    @Test
    void treatmentPreservesItsTotalDoseWhenSeveralTicksAreEvaluatedTogether() {
        RadiationTreatment treatment = new RadiationTreatment();
        treatment.start(14, 10D);

        assertEquals(40D, treatment.tick(4), 0.000001D);
        assertEquals(10, treatment.remainingTicks());
        assertEquals(100D, treatment.tick(20), 0.000001D);
        assertEquals(0, treatment.remainingTicks());
    }
}
