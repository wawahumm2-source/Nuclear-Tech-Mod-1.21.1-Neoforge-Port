package com.hbm.client.weapon;

import net.minecraft.client.model.HumanoidModel;

/** Third-person gun arm selection following Superb Warfare's normal/sprint pose split. */
public final class HbmGunArmPose {
    public static HumanoidModel.ArmPose select(boolean reloading, boolean sprinting, boolean onGround) {
        if (reloading) {
            return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        }
        return sprinting && onGround
                ? HumanoidModel.ArmPose.CROSSBOW_CHARGE
                : HumanoidModel.ArmPose.BOW_AND_ARROW;
    }

    private HbmGunArmPose() {
    }
}
