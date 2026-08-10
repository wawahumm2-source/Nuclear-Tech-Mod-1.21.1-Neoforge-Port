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
        List<VirtualBone> virtualBones
) {
    private static final List<SuperbGunRig> RIGS = List.of(
            rig("star_f",
                    pose(0.27, -0.22, -0.30, -2.0F, 165.0F, -1.5F, 0.88F),
                    pose(0.00, -0.23, -0.42, -0.5F, 180.0F, 0.0F, 0.94F),
                    new VirtualBone("Righthand", new Vec3(-0.10, -1.80, -3.10),
                            new Vec3(-72.0, 2.0, 180.0), BoneRole.RIGHT_HAND, 0.0F),
                    new VirtualBone("Lefthand", new Vec3(0.32, -1.20, -2.35),
                            new Vec3(-76.0, -10.0, 178.0), BoneRole.LEFT_HAND, 0.0F),
                    new VirtualBone("flare", new Vec3(0.04, 2.45, 6.18),
                            Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.18F)),
            rig("stg77",
                    pose(0.30, -0.25, -0.42, -2.0F, 166.0F, -2.0F, 0.82F),
                    pose(0.00, -0.16, -0.50, 0.0F, 180.0F, 0.0F, 0.88F),
                    new VirtualBone("Righthand", new Vec3(-0.08, -1.55, -3.20),
                            new Vec3(-70.0, 2.0, 180.0), BoneRole.RIGHT_HAND, 0.0F),
                    new VirtualBone("Lefthand", new Vec3(0.12, -0.70, 1.10),
                            new Vec3(-72.0, -8.0, 180.0), BoneRole.LEFT_HAND, 0.0F),
                    new VirtualBone("flare", new Vec3(0.02, 0.55, 8.05),
                            Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.22F)),
            rig("spas-12",
                    pose(0.31, -0.24, -0.42, -2.0F, -14.0F, -2.0F, 0.90F),
                    pose(0.00, -0.17, -0.52, 0.0F, 0.0F, 0.0F, 0.96F),
                    new VirtualBone("Righthand", new Vec3(-0.05, 0.10, 0.20),
                            new Vec3(-70.0, 0.0, 180.0), BoneRole.RIGHT_HAND, 0.0F),
                    new VirtualBone("Lefthand", new Vec3(0.05, 0.20, -4.80),
                            new Vec3(-72.0, 8.0, 180.0), BoneRole.LEFT_HAND, 0.0F),
                    new VirtualBone("flare", new Vec3(0.00, 1.20, -10.28),
                            Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.24F)),
            rig("congolake",
                    pose(0.29, -0.24, -0.36, -2.0F, 166.0F, -2.0F, 1.02F),
                    pose(0.00, -0.18, -0.46, 0.0F, 180.0F, 0.0F, 1.08F),
                    new VirtualBone("Righthand", new Vec3(-0.04, 0.75, -2.85),
                            new Vec3(-70.0, 2.0, 180.0), BoneRole.RIGHT_HAND, 0.0F),
                    new VirtualBone("Lefthand", new Vec3(0.08, 0.85, 0.40),
                            new Vec3(-72.0, -8.0, 180.0), BoneRole.LEFT_HAND, 0.0F),
                    new VirtualBone("flare", new Vec3(0.02, 1.55, 4.28),
                            Vec3.ZERO, BoneRole.MUZZLE_FLASH, 0.26F))
    );

    public static Optional<SuperbGunRig> find(ResourceLocation model) {
        return RIGS.stream().filter(rig -> rig.model.equals(model)).findFirst();
    }

    public Optional<VirtualBone> virtualBone(String name) {
        return virtualBones.stream().filter(bone -> bone.name.equals(name)).findFirst();
    }

    private static SuperbGunRig rig(String modelName, FirstPersonPose hip, FirstPersonPose ads,
                                    VirtualBone... bones) {
        return new SuperbGunRig(
                ResourceLocation.fromNamespaceAndPath(
                        HbmNuclearTech.MOD_ID, "models/weapons/" + modelName + ".obj"),
                hip,
                ads,
                List.of(bones)
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

    public record VirtualBone(String name, Vec3 pivot, Vec3 rotationDegrees,
                              BoneRole role, float effectSize) {
    }

    public enum BoneRole {
        LEFT_HAND,
        RIGHT_HAND,
        MUZZLE_FLASH
    }
}
