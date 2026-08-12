package com.hbm.client.weapon.render;

import com.hbm.HbmNuclearTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetPistolCalibrationStateTest {
    @Test
    void translationRotationAndScaleUseIndependentStepSizes() {
        TargetPistolCalibrationState.Values base = new TargetPistolCalibrationState.Values(
                0.0D, 0.0D, 0.0D, 0.0D, 180.0D, 0.0D, 0.82F,
                arm(), arm(), 1.0F, view());

        TargetPistolCalibrationState.Values moved = base.adjust(
                TargetPistolCalibrationState.Field.X, 1,
                TargetPistolCalibrationState.Step.NORMAL);
        TargetPistolCalibrationState.Values rotated = moved.adjust(
                TargetPistolCalibrationState.Field.ROT_Y, -1,
                TargetPistolCalibrationState.Step.COARSE);
        TargetPistolCalibrationState.Values scaled = rotated.adjust(
                TargetPistolCalibrationState.Field.SCALE, 1,
                TargetPistolCalibrationState.Step.FINE);

        assertEquals(0.05D, scaled.x(), 1.0E-9D);
        assertEquals(175.0D, scaled.rotationY(), 1.0E-9D);
        assertEquals(0.822F, scaled.scale(), 1.0E-6F);
    }

    @Test
    void unsafeScaleAndTranslationAreClamped() {
        TargetPistolCalibrationState.Values base = new TargetPistolCalibrationState.Values(
                32.0D, 0.0D, 0.0D, 0.0D, 180.0D, 0.0D, 0.10F,
                arm(), arm(), 1.0F, view());

        TargetPistolCalibrationState.Values result = base
                .adjust(TargetPistolCalibrationState.Field.X, 1,
                        TargetPistolCalibrationState.Step.COARSE)
                .adjust(TargetPistolCalibrationState.Field.SCALE, -1,
                        TargetPistolCalibrationState.Step.COARSE);

        assertEquals(32.0D, result.x(), 1.0E-9D);
        assertEquals(0.10F, result.scale(), 1.0E-6F);
    }

    @Test
    void armCalibrationDoesNotMoveWeaponMeshOrOppositeArm() {
        TargetPistolCalibrationState.Values base = new TargetPistolCalibrationState.Values(
                0.0D, 0.0D, 0.0D, 0.0D, 180.0D, 0.0D, 0.82F,
                arm(), arm(), 1.0F, view());

        TargetPistolCalibrationState.Values result = base.adjust(
                TargetPistolCalibrationState.Field.RIGHT_ROT_Z, 1,
                TargetPistolCalibrationState.Step.COARSE);

        assertEquals(0.0D, result.x(), 1.0E-9D);
        assertEquals(0.0D, result.leftArm().rotationZ(), 1.0E-9D);
        assertEquals(5.0D, result.rightArm().rotationZ(), 1.0E-9D);
    }

    @Test
    void adsPoseMovesWholeRigWithoutChangingHipModelOrHands() {
        TargetPistolCalibrationState.Values base = new TargetPistolCalibrationState.Values(
                0.4D, -0.7D, -2.2D, -1.0D, 180.0D, 0.0D, 1.01F,
                arm(), arm(), 1.0F, view());

        TargetPistolCalibrationState.Values result = base.adjust(
                TargetPistolCalibrationState.Field.ADS_X, -1,
                TargetPistolCalibrationState.Step.FINE);

        assertEquals(0.4D, result.x(), 1.0E-9D);
        assertEquals(0.0D, result.rightArm().pivotX(), 1.0E-9D);
        assertEquals(-0.01D, result.adsPose().x(), 1.0E-9D);
    }

    @Test
    void hipAndAdsGeometryUseIndependentCoordinatedEndpoints() {
        SuperbGunRig rig = targetRig();
        TargetPistolCalibrationState.Values values =
                TargetPistolCalibrationState.Values.from(rig);

        assertEquals(new Vec3(0.40, -0.70, -2.20),
                values.modelPoseAt(rig, 0.0F).translation());
        assertEquals(new Vec3(-1.20, 0.55, -2.65),
                values.modelPoseAt(rig, 1.0F).translation());
        assertEquals(-0.65D, values.armPoseAt(
                rig, SuperbGunRig.BoneRole.RIGHT_HAND, 0.0F).pivotX(), 1.0E-9D);
        assertEquals(-1.75D, values.armPoseAt(
                rig, SuperbGunRig.BoneRole.RIGHT_HAND, 1.0F).pivotX(), 1.0E-9D);
        assertEquals(0.75D, values.armPoseAt(
                rig, SuperbGunRig.BoneRole.LEFT_HAND, 0.0F).pivotX(), 1.0E-9D);
        assertEquals(-0.50D, values.armPoseAt(
                rig, SuperbGunRig.BoneRole.LEFT_HAND, 1.0F).pivotX(), 1.0E-9D);
    }

    @Test
    void guiAndThirdPersonNeverInheritFirstPersonCalibration() {
        SuperbGunRig rig = targetRig();
        TargetPistolCalibrationState.Values values =
                TargetPistolCalibrationState.Values.from(rig);

        SuperbGunRig.ModelPose gui = TargetPistolCalibrationState.modelPoseForContext(
                rig, values, ItemDisplayContext.GUI, 1.0F);
        SuperbGunRig.ModelPose thirdPerson = TargetPistolCalibrationState.modelPoseForContext(
                rig, values, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 0.0F);
        SuperbGunRig.ModelPose firstPerson = TargetPistolCalibrationState.modelPoseForContext(
                rig, values, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0.0F);

        assertEquals(Vec3.ZERO, gui.translation());
        assertEquals(new Vec3(0.0D, 180.0D, 0.0D), gui.rotationDegrees());
        assertEquals(1.0F, gui.scale());
        assertEquals(gui, thirdPerson);
        assertEquals(rig.modelPose(), firstPerson);
    }

    private static TargetPistolCalibrationState.ArmPose arm() {
        return new TargetPistolCalibrationState.ArmPose(
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static TargetPistolCalibrationState.ViewPose view() {
        return new TargetPistolCalibrationState.ViewPose(
                0.0D, 0.23D, 0.18D, 0.0D, 0.0D, 0.0D, 1.0F);
    }

    private static SuperbGunRig targetRig() {
        ResourceLocation model = ResourceLocation.fromNamespaceAndPath(
                HbmNuclearTech.MOD_ID, "models/weapons/star_f.obj");
        return SuperbGunRig.find(model).orElseThrow();
    }
}
