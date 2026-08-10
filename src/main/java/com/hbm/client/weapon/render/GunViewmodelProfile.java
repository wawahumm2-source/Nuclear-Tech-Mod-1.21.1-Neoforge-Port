package com.hbm.client.weapon.render;

import com.hbm.HbmNuclearTech;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Reusable first-person presentation data. Gameplay remains definition-driven; this profile only
 * describes how an HBM model is held and where player-arm bones attach to its authored geometry.
 */
public record GunViewmodelProfile(
        ResourceLocation model,
        Pose hip,
        Pose ads,
        List<ArmAnchor> armAnchors
) {
    private static final GunViewmodelProfile TARGET_PISTOL = new GunViewmodelProfile(
            ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "models/weapons/star_f.obj"),
            new Pose(0.255D, -0.16D, 0.04D, 0.0F, -2.0F, -2.5F),
            new Pose(0.0D, 0.025D, -0.10D, -1.0F, 0.0F, 0.0F),
            List.of(
                    new ArmAnchor("Righthand", new Vec3(-0.15D, -1.9D, -3.15D), false),
                    new ArmAnchor("Lefthand", new Vec3(0.35D, -0.2D, 0.45D), true)
            )
    );

    private static final List<GunViewmodelProfile> PROFILES = List.of(TARGET_PISTOL);

    public static Optional<GunViewmodelProfile> find(ResourceLocation model) {
        return PROFILES.stream().filter(profile -> profile.model.equals(model)).findFirst();
    }

    public Optional<ArmAnchor> arm(String boneName) {
        return armAnchors.stream().filter(anchor -> anchor.boneName.equals(boneName)).findFirst();
    }

    public void apply(PoseStack poseStack, float adsBlend, float recoilPitch, float recoilYaw, float time) {
        float blend = Mth.clamp(adsBlend, 0.0F, 1.0F);
        Pose pose = hip.lerp(ads, blend);
        float idleStrength = 1.0F - blend * 0.72F;
        double breathingX = Math.sin(time * 0.067F) * 0.006D * idleStrength;
        double breathingY = Math.cos(time * 0.052F) * 0.005D * idleStrength;

        poseStack.translate(pose.x + breathingX, pose.y + breathingY, pose.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.xRotation - recoilPitch * 0.42F));
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yRotation + recoilYaw * 0.32F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.zRotation - recoilYaw * 0.18F));
        poseStack.translate(0.0D, 0.0D, Math.min(0.12D, recoilPitch * 0.012D));
    }

    public record Pose(double x, double y, double z, float xRotation, float yRotation, float zRotation) {
        private Pose lerp(Pose target, float amount) {
            return new Pose(
                    Mth.lerp(amount, x, target.x),
                    Mth.lerp(amount, y, target.y),
                    Mth.lerp(amount, z, target.z),
                    Mth.lerp(amount, xRotation, target.xRotation),
                    Mth.lerp(amount, yRotation, target.yRotation),
                    Mth.lerp(amount, zRotation, target.zRotation)
            );
        }
    }

    public record ArmAnchor(String boneName, Vec3 pivot, boolean left) {
    }
}
