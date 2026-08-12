package com.hbm.weapon;

import com.hbm.HbmNuclearTech;
import com.hbm.client.weapon.render.SuperbGunRig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetPistolPresentationTest {
    @Test
    void adsPoseUsesCenteredPhysicalSightPresentation() {
        SuperbGunRig rig = targetRig();
        SuperbGunRig.FirstPersonPose ads = rig.ads();

        assertEquals(-0.484375D, ads.x(), 1.0E-9D);
        assertEquals(0.2340625D, ads.y(), 1.0E-9D);
        assertEquals(0.18D, ads.z(), 1.0E-9D);
        assertEquals(0.0F, ads.yRotation());
        assertEquals(0.0F, ads.xRotation());
        assertEquals(0.0F, ads.zRotation());
        assertEquals(1.0F, ads.scale());
    }

    @Test
    void targetPistolPreservesApprovedHipMeshAndHandsDuringAds() {
        SuperbGunRig rig = targetRig();

        assertEquals(-0.484375D, rig.hip().x(), 1.0E-9D);
        assertEquals(0.21875D, rig.hip().y(), 1.0E-9D);
        assertEquals(-0.09375D, rig.hip().z(), 1.0E-9D);
        assertEquals(0.0F, rig.hip().yRotation());
        assertEquals(1.0F, rig.hip().scale());
        assertEquals(1.0F, rig.ads().scale());
        assertEquals(new Vec3(0.40, -0.70, -2.20), rig.modelPose().translation());
        assertEquals(new Vec3(-1.0, 180.0, 0.0), rig.modelPose().rotationDegrees());
        assertEquals(1.01F, rig.modelPose().scale());
        assertEquals(new Vec3(-1.20, 0.55, -2.65), rig.adsModelPose().translation());
        assertEquals(new Vec3(1.0, 180.0, 0.0), rig.adsModelPose().rotationDegrees());
        assertEquals(0.99F, rig.adsModelPose().scale());
        assertEquals(1.0F, rig.armScale());
        assertEquals(1.0F, rig.adsArmScale());
        assertEquals(new Vec3(-0.65, -1.55, 0.50),
                hand(rig, SuperbGunRig.BoneRole.RIGHT_HAND).pivot());
        assertEquals(new Vec3(0.75, -1.60, 0.45),
                hand(rig, SuperbGunRig.BoneRole.LEFT_HAND).pivot());
        assertEquals(new Vec3(-81.0, -163.0, 167.0),
                hand(rig, SuperbGunRig.BoneRole.RIGHT_HAND).rotationDegrees());
        assertEquals(new Vec3(-88.0, 161.0, 170.0),
                hand(rig, SuperbGunRig.BoneRole.LEFT_HAND).rotationDegrees());
        assertEquals(new Vec3(-1.75, -0.50, 0.50),
                adsHand(rig, SuperbGunRig.BoneRole.RIGHT_HAND).pivot());
        assertEquals(new Vec3(-0.50, -0.55, 0.45),
                adsHand(rig, SuperbGunRig.BoneRole.LEFT_HAND).pivot());
        assertEquals(new Vec3(-88.0, 161.0, 176.0),
                adsHand(rig, SuperbGunRig.BoneRole.LEFT_HAND).rotationDegrees());
    }

    @Test
    void hipAndAdsRemainDistinctEndpointsWithAContinuousMidpoint() {
        SuperbGunRig rig = targetRig();
        SuperbGunRig.FirstPersonPose midpoint = rig.hip().lerp(rig.ads(), 0.5F);

        assertEquals(-0.484375D, midpoint.x(), 1.0E-9D);
        assertEquals((0.21875D + 0.2340625D) * 0.5D, midpoint.y(), 1.0E-9D);
        assertEquals((-0.09375D + 0.18D) * 0.5D, midpoint.z(), 1.0E-9D);
        assertEquals(1.0F, midpoint.scale());
        assertEquals(new Vec3(0.40, -0.70, -2.20), rig.modelPose().translation());
        assertEquals(new Vec3(-0.65, -1.55, 0.50),
                hand(rig, SuperbGunRig.BoneRole.RIGHT_HAND).pivot());
    }

    private static SuperbGunRig.VirtualBone hand(SuperbGunRig rig, SuperbGunRig.BoneRole role) {
        return rig.virtualBones().stream().filter(bone -> bone.role() == role).findFirst()
                .orElseThrow();
    }

    private static SuperbGunRig.VirtualBone adsHand(SuperbGunRig rig,
                                                     SuperbGunRig.BoneRole role) {
        return rig.adsVirtualBones().stream().filter(bone -> bone.role() == role).findFirst()
                .orElseThrow();
    }

    private static SuperbGunRig targetRig() {
        ResourceLocation model = ResourceLocation.fromNamespaceAndPath(
                HbmNuclearTech.MOD_ID, "models/weapons/star_f.obj");
        return SuperbGunRig.find(model).orElseThrow();
    }
}
