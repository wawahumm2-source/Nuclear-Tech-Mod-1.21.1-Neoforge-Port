package com.hbm.client.weapon.render;

import com.hbm.client.weapon.ClientWeaponController;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Continuous first-person gun motion adapted from Superb Warfare's ClientEventHandler and
 * Mp443ItemModel at commit 9b5284f42ef79532e6fb7f03ab07425c693b0b43 (GPL-3.0).
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
    private static float swayTime;
    private static float swayX;
    private static float swayY;
    private static float previousStrafe;
    private static float strafe;
    private static float previousVerticalVelocity;
    private static float verticalVelocity;
    private static float previousReloadTime;
    private static float reloadTime;
    private static float reloadDuration;
    private static boolean emptyReload;
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
        previousStrafe = strafe;
        previousVerticalVelocity = verticalVelocity;
        previousReloadTime = reloadTime;

        if (!valid || minecraft.player == null) {
            move = Mth.approach(move, 0.0F, 0.20F);
            sprint = Mth.approach(sprint, 0.0F, 0.22F);
            draw = Mth.approach(draw, 1.0F, 0.18F);
            fireTime = 0.0F;
            flash = 0.0F;
            swayX *= 0.7F;
            swayY *= 0.7F;
            strafe *= 0.7F;
            verticalVelocity *= 0.7F;
            reloadTime = 0.0F;
            reloadDuration = 0.0F;
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

        double horizontalSpeed = minecraft.player.getDeltaMovement().horizontalDistance();
        float movementTarget = minecraft.player.onGround()
                ? Mth.clamp((float) (horizontalSpeed / 0.12D), 0.0F, 1.0F)
                : 0.04F;
        move = Mth.lerp(0.28F, move, movementTarget);
        sprint = Mth.approach(sprint, minecraft.player.isSprinting() ? 1.0F : 0.0F, 0.16F);
        draw = Mth.approach(draw, 0.0F, 0.13F);
        float phaseSpeed = Mth.lerp(sprint, 0.31F, 0.48F);
        walkPhase += phaseSpeed * Math.max(0.08F, move);
        swayTime += 0.10F;

        float strafeTarget = 0.0F;
        if (minecraft.options.keyLeft.isDown()) {
            strafeTarget -= 1.0F;
        }
        if (minecraft.options.keyRight.isDown()) {
            strafeTarget += 1.0F;
        }
        strafe = Mth.lerp(0.24F, strafe, strafeTarget);
        float verticalTarget = Mth.clamp((float) minecraft.player.getDeltaMovement().y + 0.078F,
                -0.8F, 0.8F);
        verticalVelocity = Mth.lerp(0.23F, verticalVelocity, verticalTarget);

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
        if (reloadTime > 0.0F) {
            reloadTime += 1.0F;
            if (reloadTime >= reloadDuration) {
                reloadTime = 0.0F;
                reloadDuration = 0.0F;
            }
        }
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

    public static void beginReload(boolean empty) {
        emptyReload = empty;
        reloadDuration = empty ? 53.0F : 45.0F;
        reloadTime = 0.001F;
        previousReloadTime = 0.001F;
    }

    public static void reset() {
        wasValid = false;
        fireTime = 0.0F;
        flash = 0.0F;
        reloadTime = 0.0F;
        reloadDuration = 0.0F;
    }

    public static void applyFirstPerson(PoseStack poseStack, SuperbGunRig rig, boolean leftHand) {
        float ads = easeInOutQuint(ClientWeaponController.viewmodelAdsBlend());
        SuperbGunRig.FirstPersonPose base = rig.hip().lerp(rig.ads(), ads);
        float moveAmount = midpoint(previousMove, move) * (1.0F - ads * 0.72F);
        float sprintAmount = midpoint(previousSprint, sprint) * (1.0F - ads);
        float drawAmount = midpoint(previousDraw, draw);
        float shotTime = midpoint(previousFireTime, fireTime);
        float actionDamping = reloadActive() ? 0.22F : 1.0F;
        float phase = walkPhase;

        double x = leftHand ? -base.x() : base.x();
        double bobX = 0.2D * Math.sin(Math.PI * phase) * moveAmount / 16.0D;
        double bobY = -0.135D * Math.sin(2.0D * Math.PI * (phase - 0.25D))
                * moveAmount / 16.0D;
        double breathingY = 0.125D * Math.sin(swayTime - 1.585D)
                * (1.0D - 0.95D * ads) / 16.0D;
        double strafeX = 0.58D * midpoint(previousStrafe, strafe) * (1.0D - ads) / 16.0D;
        double verticalY = -2.0D * midpoint(previousVerticalVelocity, verticalVelocity)
                * (1.0D - 0.5D * ads) / 16.0D;

        double sprintCurve = 4.0D * sprintAmount * (1.0D - sprintAmount);
        double sprintX = (3.5D * sprintAmount
                + 2.0D * Math.sin(Math.PI * phase) * sprintAmount) / 16.0D;
        double sprintY = ((-0.35D - 8.0D * sprintCurve) * sprintAmount
                + Math.sin(2.0D * Math.PI * phase) * sprintAmount) / 16.0D;
        double sprintZ = 2.45D * sprintAmount / 16.0D;

        poseStack.translate(x + (leftHand ? -bobX : bobX) + strafeX + sprintX,
                base.y() + bobY + breathingY + verticalY + sprintY,
                base.z() + sprintZ);

        float yaw = leftHand ? -base.yRotation() : base.yRotation();
        float roll = leftHand ? -base.zRotation() : base.zRotation();
        poseStack.mulPose(Axis.XP.rotationDegrees(base.xRotation()));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        poseStack.translate((leftHand ? -1.0D : 1.0D) * 0.28D * drawAmount,
                -0.42D * drawAmount, 0.10D * drawAmount);
        poseStack.mulPose(Axis.XP.rotationDegrees(39.0F * sprintAmount - 48.0F * drawAmount));
        poseStack.mulPose(Axis.YP.rotationDegrees((leftHand ? -1.0F : 1.0F)
                * (35.6F * sprintAmount + 115.0F * drawAmount)));
        poseStack.mulPose(Axis.ZP.rotationDegrees((leftHand ? -1.0F : 1.0F)
                * (34.7F * sprintAmount + 45.0F * drawAmount)));

        float swayScale = 1.0F - ads * 0.78F;
        float idleSway = (float) (-0.008D * Math.sin(swayTime) * (1.0D - 0.95D * ads));
        poseStack.mulPose(Axis.XP.rotation(idleSway * actionDamping));
        poseStack.mulPose(Axis.XP.rotationDegrees(swayX * swayScale * actionDamping));
        poseStack.mulPose(Axis.YP.rotationDegrees(swayY * swayScale * actionDamping));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(Math.PI * phase)
                * 1.15F * moveAmount * actionDamping));

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

    public static float reloadCameraPitch() {
        float t = reloadProgress();
        if (t <= 0.0F) return 0.0F;
        if (t < 0.20F) return smoothSegment(t, 0.0F, 0.20F, 0.0F, 1.25F);
        if (t < 0.48F) return smoothSegment(t, 0.20F, 0.48F, 1.25F, 0.55F);
        if (t < 0.68F) return smoothSegment(t, 0.48F, 0.68F, 0.55F, 1.15F);
        if (emptyReload && t < 0.84F) return smoothSegment(t, 0.68F, 0.84F, 1.15F, 1.75F);
        return smoothSegment(t, emptyReload ? 0.84F : 0.68F, 1.0F,
                emptyReload ? 1.75F : 1.15F, 0.0F);
    }

    public static float reloadCameraYaw() {
        float t = reloadProgress();
        if (t <= 0.0F) return 0.0F;
        return (float) Math.sin(Math.PI * t) * 0.30F;
    }

    public static float reloadCameraRoll() {
        float t = reloadProgress();
        if (t <= 0.0F) return 0.0F;
        return (float) -Math.sin(2.0D * Math.PI * t) * 0.34F;
    }

    public static boolean reloadActive() {
        return reloadTime > 0.0F && reloadDuration > 0.0F;
    }

    private static float reloadProgress() {
        if (!reloadActive()) return 0.0F;
        return Mth.clamp(midpoint(previousReloadTime, reloadTime) / reloadDuration, 0.0F, 1.0F);
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

    private static float smoothSegment(float value, float fromTime, float toTime,
                                       float fromValue, float toValue) {
        float t = Mth.clamp((value - fromTime) / (toTime - fromTime), 0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        return Mth.lerp(t, fromValue, toValue);
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
