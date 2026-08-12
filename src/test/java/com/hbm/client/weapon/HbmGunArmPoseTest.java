package com.hbm.client.weapon;

import net.minecraft.client.model.HumanoidModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HbmGunArmPoseTest {
    @Test
    void standingAndWalkingUseAimedThirdPersonPose() {
        assertEquals(HumanoidModel.ArmPose.BOW_AND_ARROW,
                HbmGunArmPose.select(false, false, true));
    }

    @Test
    void groundedSprintUsesLoweredSprintPose() {
        assertEquals(HumanoidModel.ArmPose.CROSSBOW_CHARGE,
                HbmGunArmPose.select(false, true, true));
    }

    @Test
    void airborneSprintDoesNotForceGroundedPose() {
        assertEquals(HumanoidModel.ArmPose.BOW_AND_ARROW,
                HbmGunArmPose.select(false, true, false));
    }

    @Test
    void reloadUsesDedicatedTwoHandedPose() {
        assertEquals(HumanoidModel.ArmPose.CROSSBOW_CHARGE,
                HbmGunArmPose.select(true, false, true));
    }
}
