package com.hbm.client.weapon.render;

import com.hbm.client.weapon.ClientWeaponController;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Continuous first-person gun motion adapted from Superb Warfare's ClientEventHandler and
 * M1911ItemModel at commit 9b5284f42ef79532e6fb7f03ab07425c693b0b43 (GPL-3.0).
 * HBM-specific pose and rig constants live in {@link SuperbGunRig}; no Superb Warfare asset data
 * is present here.
 */
public final class SuperbGunPresentationState {
    private static float previousMove;
    private static float move;
    private static float previousSprint;
    private static float sprint;
    private static float previousDraw;
    private static float draw = 1.0F;
    private static float previousFireTime;
    private static float fireTime;
    private static float previousFlash;
    private static float flash;
    private static float walkPhase;
    private static float swayX;
    private static float swayY;
    private static float lastPitch;
    private static float lastYaw;
    private static boolean wasValid;
    private static float recoilSide = 1.0F;

    public static void tick(Minecraft minecraft, boolean valid) {
        previousMove = move;
        previousSprint = sprint;
        previousDraw = draw;
        previousFireTime = fireTime;
        previousFlash = flash;

        if (!valid || minecraft.player == null) {
            move = Mth.approach(move, 0.0F, 0.20F);
            sprint = Mth.approach(sprint, 0.0F, 0.22F);
            draw = Mth.approach(draw, 1.0F, 0.18F);
            fireTime = 0.0F;
            flash = 0.0F;
            swayX *= 0.7F;
            swayY *= 0.7F;
            wasValid = false;
            return;
        }

        if (!wasValid) {
            draw = 1.0F;
            previousDraw = 1.0F;
            lastPitch = minecraft.player.getXRot();
            lastYaw = minecraft.player.getYRot();
        }
        wasValid = true;

        double horizontalSpeed = minecraft.player.getDeltaMovement().horizontalDistanceSqr();
        move = Mth.approach(move, horizontalSpeed > 0.0004D ? 1.0F : 0.0F, 0.18F);
        sprint = Mth.approach(sprint, minecraft.player.isSprinting() ? 1.0F : 0.0F, 0.20F);
        draw = Mth.approach(draw, 0.0F, 0.16F);
        walkPhase += 0.47F * Math.max(0.2F, move);

        float pitchDelta = Mth.wrapDegrees(minecraft.player.getXRot() - lastPitch);
        float yawDelta = Mth.wrapDegrees(minecraft.player.getYRot() - lastYaw);
        lastPitch = minecraft.player.getXRot();
        lastYaw = minecraft.player.getYRot();
        swayX = Mth.lerp(0.28F, swayX, Mth.clamp(-pitchDelta * 0.28F, -2.5F, 2.5F));
        swayY = Mth.lerp(0.28F, swayY, Mth.clamp(-yawDelta * 0.22F, -3.0F, 3.0F));

        if (fireTime > 0.0F) {
            fireTime += 0.34F;
            if (fireTime >= 3.0F) {
                fireTime = 0.0F;
            }
        }
        flash = Mth.approach(flash, 0.0F, 0.48F);
    }

    public static void beginEquip() {
        draw = 1.0F;
        previousDraw = 1.0F;
    }

    public static void fire() {
        fireTime = 0.001F;
        previousFireTime = 0.001F;
        flash = 1.0F;
        previousFlash = 1.0F;
        recoilSide = -recoilSide;
    }

    public static void reset() {
        wasValid = false;
        fireTime = 0.0F;
        flash = 0.0F;
    }

    public static void applyFirstPerson(PoseStack poseStack, SuperbGunRig rig, boolean leftHand) {
        float ads = easeInOutQuint(ClientWeaponController.viewmodelAdsBlend());
        SuperbGunRig.FirstPersonPose base = rig.hip().lerp(rig.ads(), ads);
        float moveAmount = midpoint(previousMove, move) * (1.0F - ads * 0.72F);
        float sprintAmount = midpoint(previousSprint, sprint) * (1.0F - ads);
        float drawAmount = midpoint(previousDraw, draw);
        float shotTime = midpoint(previousFireTime, fireTime);

        double x = leftHand ? -base.x() : base.x();
        double bobX = Math.sin(walkPhase) * 0.012D * moveAmount;
        double bobY = Math.abs(Math.cos(walkPhase)) * -0.010D * moveAmount;
        poseStack.translate(x + (leftHand ? -bobX : bobX), base.y() + bobY, base.z());

        float yaw = leftHand ? -base.yRotation() : base.yRotation();
        float roll = leftHand ? -base.zRotation() : base.zRotation();
        poseStack.mulPose(Axis.XP.rotationDegrees(base.xRotation()));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        poseStack.translate(
                (leftHand ? -1.0D : 1.0D) * (0.10D * sprintAmount + 0.28D * drawAmount),
                -0.18D * sprintAmount - 0.42D * drawAmount,
                0.06D * sprintAmount + 0.10D * drawAmount
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(28.0F * sprintAmount - 48.0F * drawAmount));
        poseStack.mulPose(Axis.YP.rotationDegrees((leftHand ? -1.0F : 1.0F)
                * (-24.0F * sprintAmount + 115.0F * drawAmount)));
        poseStack.mulPose(Axis.ZP.rotationDegrees((leftHand ? -1.0F : 1.0F)
                * (30.0F * sprintAmount + 45.0F * drawAmount)));

        float swayScale = 1.0F - ads * 0.78F;
        poseStack.mulPose(Axis.XP.rotationDegrees(swayX * swayScale));
        poseStack.mulPose(Axis.YP.rotationDegrees(swayY * swayScale));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(walkPhase) * 1.15F * moveAmount));

        if (shotTime > 0.0F) {
            float zoomScale = 1.0F - ads * 0.82F;
            poseStack.translate(0.0D, 0.0D, 0.018D * boneMoveZ(shotTime) * zoomScale);
            poseStack.mulPose(Axis.XP.rotationDegrees(-0.48F * boneRotX(shotTime) * zoomScale));
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    0.35F * boneRotY(shotTime) * recoilSide * zoomScale));
            poseStack.mulPose(Axis.ZP.rotationDegrees(
                    0.24F * boneRotZ(shotTime) * recoilSide * zoomScale));
        }

        poseStack.scale(base.scale(), base.scale(), base.scale());
    }

    public static float slideTravel() {
        float time = midpoint(previousFireTime, fireTime);
        if (time <= 0.0F || time > 0.5F) {
            return 0.0F;
        }
        return Math.min(1.0F, 1.2F * Mth.sin(2.0F * Mth.PI * time));
    }

    public static float flashStrength() {
        return midpoint(previousFlash, flash);
    }

    public static float flashRotation() {
        return recoilSide * 0.18F;
    }

    public static float crosshairSpread() {
        float ads = easeInOutQuint(ClientWeaponController.viewmodelAdsBlend());
        float movement = midpoint(previousMove, move);
        float sprinting = midpoint(previousSprint, sprint);
        float firing = midpoint(previousFireTime, fireTime) > 0.0F ? 1.0F : 0.0F;
        return Mth.clamp(1.0F + movement * 1.5F + sprinting * 4.0F + firing * 2.2F
                - ads * 0.82F, 0.35F, 8.0F);
    }

    public static float crosshairOffsetX() {
        float ads = easeInOutQuint(ClientWeaponController.viewmodelAdsBlend());
        return swayY * (1.0F - ads * 0.82F) * 0.55F
                + (float) Math.sin(walkPhase) * midpoint(previousMove, move) * 1.35F;
    }

    public static float crosshairOffsetY() {
        float ads = easeInOutQuint(ClientWeaponController.viewmodelAdsBlend());
        return swayX * (1.0F - ads * 0.82F) * 0.45F
                + (float) Math.abs(Math.cos(walkPhase)) * midpoint(previousMove, move) * 0.75F;
    }

    private static float midpoint(float previous, float current) {
        return (previous + current) * 0.5F;
    }

    private static float easeInOutQuint(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t < 0.5F
                ? 16.0F * t * t * t * t * t
                : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 5.0D) / 2.0F;
    }

    // GPL-3.0 curve adaptation from Superb Warfare ClientEventHandler.getBoneRotX/Y/Z.
    private static float boneRotX(float t) {
        if (t <= 0.25F) return segment(t, 0.0F, 0.25F, 0.0F, -5.82024F);
        if (t <= 0.5F) return segment(t, 0.25F, 0.5F, -5.82024F, -6.38564F);
        if (t <= 0.75F) return segment(t, 0.5F, 0.75F, -6.38564F, -6.0138F);
        if (t <= 1.0F) return segment(t, 0.75F, 1.0F, -6.0138F, -3.22698F);
        if (t <= 1.3333F) return segment(t, 1.0F, 1.3333F, -3.22698F, -0.42425F);
        if (t <= 1.75F) return segment(t, 1.3333F, 1.75F, -0.42425F, 0.23068F);
        if (t <= 2.0833F) return segment(t, 1.75F, 2.0833F, 0.23068F, -0.09988F);
        if (t <= 2.4167F) return segment(t, 2.0833F, 2.4167F, -0.09988F, 0.04509F);
        return segment(t, 2.4167F, 3.0F, 0.04509F, 0.0F);
    }

    private static float boneRotY(float t) {
        if (t <= 0.25F) return segment(t, 0.0F, 0.25F, 0.0F, 1.33042F);
        if (t <= 0.5F) return segment(t, 0.25F, 0.5F, 1.33042F, -0.61289F);
        if (t <= 0.75F) return segment(t, 0.5F, 0.75F, -0.61289F, -0.64862F);
        if (t <= 1.0F) return segment(t, 0.75F, 1.0F, -0.64862F, -0.95049F);
        if (t <= 1.3333F) return segment(t, 1.0F, 1.3333F, -0.95049F, 0.27786F);
        if (t <= 1.75F) return segment(t, 1.3333F, 1.75F, 0.27786F, -0.21405F);
        if (t <= 2.0833F) return segment(t, 1.75F, 2.0833F, -0.21405F, 0.076F);
        if (t <= 2.4167F) return segment(t, 2.0833F, 2.4167F, 0.076F, 0.01634F);
        return segment(t, 2.4167F, 3.0F, 0.01634F, 0.0F);
    }

    private static float boneRotZ(float t) {
        if (t <= 0.25F) return segment(t, 0.0F, 0.25F, 0.0F, 5.79388F);
        if (t <= 0.5F) return segment(t, 0.25F, 0.5F, 5.79388F, -1.91761F);
        if (t <= 0.75F) return segment(t, 0.5F, 0.75F, -1.91761F, -3.1926F);
        if (t <= 1.0F) return segment(t, 0.75F, 1.0F, -3.1926F, 1.89646F);
        if (t <= 1.3333F) return segment(t, 1.0F, 1.3333F, 1.89646F, 0.43549F);
        if (t <= 1.75F) return segment(t, 1.3333F, 1.75F, 0.43549F, -0.46178F);
        if (t <= 2.0833F) return segment(t, 1.75F, 2.0833F, -0.46178F, 0.12379F);
        if (t <= 2.4167F) return segment(t, 2.0833F, 2.4167F, 0.12379F, -0.04605F);
        return segment(t, 2.4167F, 3.0F, -0.04605F, 0.0F);
    }

    private static float boneMoveZ(float t) {
        if (t <= 0.1667F) return segment(t, 0.0F, 0.1667F, 0.0F, 5.205F);
        if (t <= 0.3333F) return segment(t, 0.1667F, 0.3333F, 5.205F, 2.775F);
        if (t <= 0.4167F) return segment(t, 0.3333F, 0.4167F, 2.775F, 0.66F);
        if (t <= 0.5833F) return segment(t, 0.4167F, 0.5833F, 0.66F, -0.005F);
        if (t <= 0.75F) return segment(t, 0.5833F, 0.75F, -0.005F, -0.485F);
        if (t <= 0.9167F) return segment(t, 0.75F, 0.9167F, -0.485F, -0.095F);
        if (t <= 1.1667F) return segment(t, 0.9167F, 1.1667F, -0.095F, 0.06F);
        if (t <= 1.3333F) return segment(t, 1.1667F, 1.3333F, 0.06F, 0.10F);
        if (t <= 1.5833F) return segment(t, 1.3333F, 1.5833F, 0.10F, -0.03F);
        return segment(t, 1.5833F, 2.0F, -0.03F, 0.0F);
    }

    private static float segment(float value, float fromTime, float toTime,
                                 float fromValue, float toValue) {
        return Mth.lerp((value - fromTime) / (toTime - fromTime), fromValue, toValue);
    }

    private SuperbGunPresentationState() {
    }
}
