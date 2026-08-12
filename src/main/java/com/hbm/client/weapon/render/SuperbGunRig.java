package com.hbm.client.weapon.render;

import com.hbm.HbmNuclearTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * HBM-authored presentation rigs consumed by the Superb Warfare-derived renderer path.
 *
 * <p>The coordinates in this file were authored against HBM's OBJ geometry. They are not
 * Superb Warfare model data. Only the renderer architecture and procedural-animation approach
 * are adapted from Superb Warfare commit 9b5284f42ef79532e6fb7f03ab07425c693b0b43.</p>
 */
public record SuperbGunRig(
        ResourceLocation model,
        FirstPersonPose hip,
        FirstPersonPose ads,
        ModelPose modelPose,
        ModelPose adsModelPose,
        float armScale,
        float adsArmScale,
        List<VirtualBone> virtualBones,
        List<VirtualBone> adsVirtualBones
) {
    private static final List<SuperbGunRig> RIGS = List.of(
            rig("star_f",
                    // Superb Warfare's MP-443 first-person item transform is
                    // translation [-7.75, 3.5, -1.5]. Express it in PoseStack units. Weapon
                    // mesh calibration lives on model_space so these exact reference arms are
                    // never scaled or rotated to compensate for HBM OBJ conventions.
                    pose(-0.484375, 0.21875, -0.09375, 0.0F, 0.0F, 0.0F, 1.0F),
                    // Approved ADS endpoint: the whole assembled rig moves to the camera axis,
                    // preserving the calibrated weapon-to-hand relationship from hip fire.
                    pose(-0.484375, 0.2340625, 0.18, 0.0F, 0.0F, 0.0F, 1.0F),
                    // Approved hip-fire mesh endpoint, captured from the live HBM calibration.
                    // HBM's Star-F points down positive Z, opposite the viewmodel convention.
                    new ModelPose(new Vec3(0.40, -0.70, -2.20),
                            new Vec3(-1.0, 180.0, 0.0), 1.01F),
                    // ADS has a distinct geometry endpoint. Keeping it separate prevents the
                    // centered iron-sight pose from replacing the approved offset hip pose.
                    new ModelPose(new Vec3(-1.20, 0.55, -2.65),
                            new Vec3(1.0, 180.0, 0.0), 0.99F),
                    1.0F, 1.0F,
                    List.of(
                            new VirtualBone("Righthand", new Vec3(-0.65, -1.55, 0.50),
                                    new Vec3(-81.0, -163.0, 167.0),
                                    BoneRole.RIGHT_HAND, 0.0F),
                            new VirtualBone("Lefthand", new Vec3(0.75, -1.60, 0.45),
                                    new Vec3(-88.0, 161.0, 170.0),
                                    BoneRole.LEFT_HAND, 0.0F),
                            new VirtualBone("flare", new Vec3(0.04, 2.45, 6.18),
                                    Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.035F)),
                    List.of(
                            new VirtualBone("Righthand", new Vec3(-1.75, -0.50, 0.50),
                                    new Vec3(-81.0, -163.0, 167.0),
                                    BoneRole.RIGHT_HAND, 0.0F),
                            new VirtualBone("Lefthand", new Vec3(-0.50, -0.55, 0.45),
                                    new Vec3(-88.0, 161.0, 176.0),
                                    BoneRole.LEFT_HAND, 0.0F),
                            new VirtualBone("flare", new Vec3(0.04, 2.45, 6.18),
                                    Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.035F))),
            rig("stg77",
                    pose(0.30, -0.25, -0.42, -2.0F, 166.0F, -2.0F, 0.82F),
                    pose(0.00, -0.16, -0.50, 0.0F, 180.0F, 0.0F, 0.88F),
                    1.0F,
                    new VirtualBone("Righthand", new Vec3(-0.08, -1.55, -3.20),
                            new Vec3(-70.0, 2.0, 180.0), BoneRole.RIGHT_HAND, 0.0F),
                    new VirtualBone("Lefthand", new Vec3(0.12, -0.70, 1.10),
                            new Vec3(-72.0, -8.0, 180.0), BoneRole.LEFT_HAND, 0.0F),
                    new VirtualBone("flare", new Vec3(0.02, 0.55, 8.05),
                            Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.045F)),
            rig("spas-12",
                    pose(0.31, -0.24, -0.42, -2.0F, -14.0F, -2.0F, 0.90F),
                    pose(0.00, -0.17, -0.52, 0.0F, 0.0F, 0.0F, 0.96F),
                    1.0F,
                    new VirtualBone("Righthand", new Vec3(-0.05, 0.10, 0.20),
                            new Vec3(-70.0, 0.0, 180.0), BoneRole.RIGHT_HAND, 0.0F),
                    new VirtualBone("Lefthand", new Vec3(0.05, 0.20, -4.80),
                            new Vec3(-72.0, 8.0, 180.0), BoneRole.LEFT_HAND, 0.0F),
                    new VirtualBone("flare", new Vec3(0.00, 1.20, -10.28),
                            Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.050F)),
            rig("congolake",
                    pose(0.29, -0.24, -0.36, -2.0F, 166.0F, -2.0F, 1.02F),
                    pose(0.00, -0.18, -0.46, 0.0F, 180.0F, 0.0F, 1.08F),
                    1.0F,
                    new VirtualBone("Righthand", new Vec3(-0.04, 0.75, -2.85),
                            new Vec3(-70.0, 2.0, 180.0), BoneRole.RIGHT_HAND, 0.0F),
                    new VirtualBone("Lefthand", new Vec3(0.08, 0.85, 0.40),
                            new Vec3(-72.0, -8.0, 180.0), BoneRole.LEFT_HAND, 0.0F),
                    new VirtualBone("flare", new Vec3(0.02, 1.55, 4.28),
                            Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.055F))
    );

    public static Optional<SuperbGunRig> find(ResourceLocation model) {
        return RIGS.stream().filter(rig -> rig.model.equals(model)).findFirst();
    }

    public Optional<VirtualBone> virtualBone(String name) {
        return virtualBones.stream().filter(bone -> bone.name.equals(name)).findFirst();
    }

    public Optional<VirtualBone> adsVirtualBone(String name) {
        return adsVirtualBones.stream().filter(bone -> bone.name.equals(name)).findFirst();
    }

    private static SuperbGunRig rig(String modelName, FirstPersonPose hip, FirstPersonPose ads,
                                    float armScale,
                                    VirtualBone... bones) {
        return rig(modelName, hip, ads, ModelPose.IDENTITY, armScale, bones);
    }

    private static SuperbGunRig rig(String modelName, FirstPersonPose hip, FirstPersonPose ads,
                                    ModelPose modelPose, float armScale,
                                    VirtualBone... bones) {
        List<VirtualBone> boneList = List.of(bones);
        return rig(modelName, hip, ads, modelPose, modelPose, armScale, armScale,
                boneList, boneList);
    }

    private static SuperbGunRig rig(String modelName, FirstPersonPose hip, FirstPersonPose ads,
                                    ModelPose modelPose, ModelPose adsModelPose,
                                    float armScale, float adsArmScale,
                                    List<VirtualBone> bones, List<VirtualBone> adsBones) {
        return new SuperbGunRig(
                ResourceLocation.fromNamespaceAndPath(
                        HbmNuclearTech.MOD_ID, "models/weapons/" + modelName + ".obj"),
                hip,
                ads,
                modelPose,
                adsModelPose,
                armScale,
                adsArmScale,
                bones,
                adsBones
        );
    }

    private static FirstPersonPose pose(double x, double y, double z, float xRot, float yRot,
                                        float zRot, float scale) {
        return new FirstPersonPose(x, y, z, xRot, yRot, zRot, scale);
    }

    public record FirstPersonPose(double x, double y, double z, float xRotation,
                                  float yRotation, float zRotation, float scale) {
        public FirstPersonPose lerp(FirstPersonPose target, float amount) {
            float blend = Mth.clamp(amount, 0.0F, 1.0F);
            return new FirstPersonPose(
                    Mth.lerp(blend, x, target.x),
                    Mth.lerp(blend, y, target.y),
                    Mth.lerp(blend, z, target.z),
                    Mth.lerp(blend, xRotation, target.xRotation),
                    Mth.lerp(blend, yRotation, target.yRotation),
                    Mth.lerp(blend, zRotation, target.zRotation),
                    Mth.lerp(blend, scale, target.scale)
            );
        }
    }

    public record ModelPose(Vec3 translation, Vec3 rotationDegrees, float scale) {
        public static final ModelPose IDENTITY = new ModelPose(Vec3.ZERO, Vec3.ZERO, 1.0F);

        public ModelPose lerp(ModelPose target, float amount) {
            double blend = Mth.clamp(amount, 0.0F, 1.0F);
            if (blend <= 0.0D) {
                return this;
            }
            if (blend >= 1.0D) {
                return target;
            }
            return new ModelPose(
                    translation.lerp(target.translation, blend),
                    rotationDegrees.lerp(target.rotationDegrees, blend),
                    Mth.lerp((float) blend, scale, target.scale));
        }

        public boolean isIdentity() {
            return Vec3.ZERO.equals(translation)
                    && Vec3.ZERO.equals(rotationDegrees)
                    && scale == 1.0F;
        }
    }

    public record VirtualBone(String name, Vec3 pivot, Vec3 rotationDegrees,
                              BoneRole role, float effectSize) {
    }

    public enum BoneRole {
        LEFT_HAND,
        RIGHT_HAND,
        MUZZLE_FLASH
    }
}
