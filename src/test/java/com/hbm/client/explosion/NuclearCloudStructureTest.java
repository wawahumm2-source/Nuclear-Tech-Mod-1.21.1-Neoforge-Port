package com.hbm.client.explosion;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NuclearCloudStructureTest {
    @Test
    void connectedStemOutranksDetachedCapDebrisAtEqualAlpha() {
        double stem = NuclearCloudStructure.retentionScore(1D, 20D, 80D, 40D, 14D, 0.8D);
        double detached = NuclearCloudStructure.retentionScore(80D, 80D, 80D, 40D, 14D, 0.8D);

        assertTrue(stem > detached);
    }

    @Test
    void collarBridgeOutranksAnEquallyFreshUnstructuredCloudlet() {
        double bridge = NuclearCloudStructure.retentionScore(12D, 70D, 80D, 40D, 14D, 0.8D);
        double unstructured = NuclearCloudStructure.retentionScore(70D, 35D, 80D, 40D, 14D, 0.8D);

        assertTrue(bridge > unstructured);
    }

    @Test
    void turbulenceIsFiniteBoundedAndVariesByCloudletPhase() {
        NuclearCloudStructure.Motion first = NuclearCloudStructure.turbulence(0.7D, 0.2D, 80, 140, 0.8D);
        NuclearCloudStructure.Motion second = NuclearCloudStructure.turbulence(0.7D, 2.2D, 80, 140, 0.8D);
        double length = Math.sqrt(first.x() * first.x() + first.y() * first.y() + first.z() * first.z());

        assertTrue(Double.isFinite(length));
        assertTrue(length <= 0.10D);
        assertNotEquals(first, second);
    }
}
