package com.hbm.client.weapon.render;

import com.hbm.client.weapon.ClientWeaponController;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.UUID;

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
    private static float previousWalkPhase;
    private static float walkPhase;
    private static float previousSwayTime;
    private static float swayTime;
    private static float previousSwayX;
    private static float swayX;
    private static float previousSwayY;
    private static float swayY;
    private static float previousStrafe;
    private static float strafe;
    private static float previousVerticalVelocity;
    private static float verticalVelocity;
    private static float previousReloadTime;
    private static float reloadTime;
    private static float reloadDuration;
    private static float previousLower;
    private static float lower;
    private static boolean emptyReload;
    private static float lastPitch;
    private static float lastYaw;
    private static boolean wasValid;
    private static float recoilSide = 1.0F;
    private static UUID activeStackIdentity;

    public static void tick(Minecraft minecraft, boolean valid, UUID stackIdentity,
                            boolean lowerRequested) {
        previousMove = move;
        previousSprint = sprint;
        previousDraw = draw;
        previousFireTime = fireTime;
        previousFlash = flash;
        previousWalkPhase = walkPhase;
        previousSwayTime = swayTime;
        previousSwayX = swayX;
        previousSwayY = swayY;
        previousStrafe = strafe;
        previousVerticalVelocity = verticalVelocity;
        previousReloadTime = reloadTime;
        previousLower = lower;

        if (!valid || minecraft.player == null) {
            move = Mth.approach(move, 0.0F, 0.20F);
            sprint = Mth.approach(sprint, 0.0F, 0.22F);
            draw = Mth.approach(draw, 1.0F, 0.18F);
            lower = Mth.approach(lower, 0.0F, 0.25F);
            fireTime = 0.0F;
            flash = 0.0F;
            swayX *= 0.7F;
            swayY *= 0.7F;
            strafe *= 0.7F;
            verticalVelocity *= 0.7F;
            reloadTime = 0.0F;
            reloadDuration = 0.0F;
            wasValid = false;
            activeStackIdentity = null;
            return;
        }

        if (!wasValid || !Objects.equals(activeStackIdentity, stackIdentity)) {
            beginWeapon(minecraft, stackIdentity);
        }
        wasValid = true;

        double horizontalSpeed = minecraft.player.getDeltaMovement().horizontalDistance();
        float movementTarget = minecraft.player.onGround()
                ? Mth.clamp((float) (horizontalSpeed / 0.12D), 0.0F, 1.0F)
                : 0.04F;
        move = Mth.lerp(0.28F, move, movementTarget);
        boolean sprintingNow = minecraft.player.isSprinting();
        sprint = Mth.approach(sprint, sprintingNow ? 1.0F : 0.0F,
                sprintBlendStep(sprintingNow));
        draw = Mth.approach(draw, 0.0F, 0.18F);
        lower = Mth.approach(lower, lowerRequested ? 1.0F : 0.0F, 0.20F);
        float phaseSpeed = movementPhaseSpeed(sprint);
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

    private static void beginWeapon(Minecraft minecraft, UUID stackIdentity) {
        activeStackIdentity = stackIdentity;
        move = previousMove = 0.0F;
        sprint = previousSprint = 0.0F;
        draw = previousDraw = 1.0F;
        fireTime = previousFireTime = 0.0F;
        flash = previousFlash = 0.0F;
        walkPhase = previousWalkPhase = 0.0F;
        swayTime = previousSwayTime = 0.0F;
        swayX = previousSwayX = 0.0F;
        swayY = previousSwayY = 0.0F;
        strafe = previousStrafe = 0.0F;
        verticalVelocity = previousVerticalVelocity = 0.0F;
        reloadTime = previousReloadTime = 0.0F;
        reloadDuration = 0.0F;
        lower = previousLower = 0.0F;
        lastPitch = minecraft.player.getXRot();
        lastYaw = minecraft.player.getYRot();
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
        activeStackIdentity = null;
        move = previousMove = 0.0F;
        sprint = previousSprint = 0.0F;
        draw = previousDraw = 1.0F;
        fireTime = 0.0F;
        previousFireTime = 0.0F;
        flash = 0.0F;
        previousFlash = 0.0F;
        walkPhase = previousWalkPhase = 0.0F;
        swayTime = previousSwayTime = 0.0F;
        swayX = previousSwayX = 0.0F;
        swayY = previousSwayY = 0.0F;
        strafe = previousStrafe = 0.0F;
        verticalVelocity = previousVerticalVelocity = 0.0F;
        reloadTime = 0.0F;
        previousReloadTime = 0.0F;
        reloadDuration = 0.0F;
        lower = previousLower = 0.0F;
    }

    public static void applyFirstPerson(PoseStack poseStack, SuperbGunRig rig, boolean leftHand,
                                        float partialTick) {
        float ads = adsPresentationBlend(partialTick);
        SuperbGunRig.FirstPersonPose base = rig.hip().lerp(
                TargetPistolCalibrationState.adsPose(rig), ads);
        // Full ADS owns the presentation. Residual walking bob after sprint cancellation made
        // the sight picture drift for several ticks while movement smoothing settled.
        float locomotionScale = 1.0F - ads;
        float moveAmount = interpolate(previousMove, move, partialTick) * locomotionScale;
        float sprintAmount = interpolate(previousSprint, sprint, partialTick) * locomotionScale;
        float drawAmount = interpolate(previousDraw, draw, partialTick);
        float shotTime = interpolate(previousFireTime, fireTime, partialTick);
        float lowerAmount = interpolate(previousLower, lower, partialTick) * (1.0F - ads);
        float actionDamping = reloadActive() ? 0.22F : 1.0F;
        float phase = interpolate(previousWalkPhase, walkPhase, partialTick);
        float renderedSwayTime = interpolate(previousSwayTime, swayTime, partialTick);
        float renderedSwayX = interpolate(previousSwayX, swayX, partialTick);
        float renderedSwayY = interpolate(previousSwayY, swayY, partialTick);

        double x = leftHand ? -base.x() : base.x();
        double bobX = 0.2D * Math.sin(Math.PI * phase) * moveAmount / 16.0D;
        double bobY = -0.135D * Math.sin(2.0D * Math.PI * (phase - 0.25D))
                * moveAmount / 16.0D;
        double breathingY = 0.125D * Math.sin(renderedSwayTime - 1.585D)
                * locomotionScale / 16.0D;
        double strafeX = 0.58D * interpolate(previousStrafe, strafe, partialTick)
                * locomotionScale / 16.0D;
        double verticalY = -2.0D * interpolate(previousVerticalVelocity, verticalVelocity, partialTick)
                * locomotionScale / 16.0D;

        // Keep the sprint transition monotonic. The former midpoint parabola displaced the
        // weapon by roughly half a block at 50% blend, dropping it below the hotbar.
        double sprintX = (1.45D + 0.40D * Math.sin(Math.PI * phase))
                * sprintAmount / 16.0D;
        double sprintY = (-0.55D + 0.30D * Math.sin(2.0D * Math.PI * phase))
                * sprintAmount / 16.0D;
        double sprintZ = 0.90D * sprintAmount / 16.0D;

        poseStack.translate(x + (leftHand ? -bobX : bobX) + strafeX + sprintX,
                base.y() + bobY + breathingY + verticalY + sprintY - 0.13D * lowerAmount,
                base.z() + sprintZ + 0.05D * lowerAmount);

        float yaw = leftHand ? -base.yRotation() : base.yRotation();
        float roll = leftHand ? -base.zRotation() : base.zRotation();
        poseStack.mulPose(Axis.XP.rotationDegrees(base.xRotation()));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        // The mechanism animation moves the magazine, slide, and individual hands. This smooth
        // outer envelope makes the reload unmistakable even with a compact legacy pistol mesh.
        float reloadProgress = reloadProgress(partialTick);
        if (reloadProgress > 0.0F) {
            float raise = reloadProgress < 0.18F
                    ? smoothSegment(reloadProgress, 0.0F, 0.18F, 0.0F, 1.0F)
                    : reloadProgress > 0.78F
                    ? smoothSegment(reloadProgress, 0.78F, 1.0F, 1.0F, 0.0F)
                    : 1.0F;
            float handSide = leftHand ? -1.0F : 1.0F;
            float mechanismBeat = (float) Math.sin(Math.PI * reloadProgress);
            poseStack.translate(handSide * 0.10D * raise,
                    -0.085D * raise - 0.018D * mechanismBeat,
                    0.035D * raise);
            poseStack.mulPose(Axis.XP.rotationDegrees(8.0F * raise));
            poseStack.mulPose(Axis.YP.rotationDegrees(-handSide * 18.0F * raise));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-handSide * 15.0F * raise));
        }

        // Equip from a short lowered pose. The former 115-degree yaw exposed the side-on item
        // transform and swept the gun across the screen whenever a GUI reset presentation.
        poseStack.translate((leftHand ? -1.0D : 1.0D) * 0.08D * drawAmount,
                -0.18D * drawAmount, 0.04D * drawAmount);
        poseStack.mulPose(Axis.XP.rotationDegrees(22.0F * sprintAmount - 10.0F * drawAmount));
        poseStack.mulPose(Axis.YP.rotationDegrees((leftHand ? -1.0F : 1.0F)
                * 22.0F * sprintAmount));
        poseStack.mulPose(Axis.ZP.rotationDegrees((leftHand ? -1.0F : 1.0F)
                * (18.0F * sprintAmount + 4.0F * drawAmount)));
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F * lowerAmount));
        poseStack.mulPose(Axis.ZP.rotationDegrees((leftHand ? 1.0F : -1.0F)
                * 8.0F * lowerAmount));

        float swayScale = 1.0F - ads;
        float idleSway = (float) (-0.008D * Math.sin(renderedSwayTime)
                * (1.0D - ads));
        poseStack.mulPose(Axis.XP.rotation(idleSway * actionDamping));
        poseStack.mulPose(Axis.XP.rotationDegrees(renderedSwayX * swayScale * actionDamping));
        poseStack.mulPose(Axis.YP.rotationDegrees(renderedSwayY * swayScale * actionDamping));
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
        float t = reloadProgress(renderPartialTick());
        if (t <= 0.0F) return 0.0F;
        if (t < 0.20F) return smoothSegment(t, 0.0F, 0.20F, 0.0F, 1.25F);
        if (t < 0.48F) return smoothSegment(t, 0.20F, 0.48F, 1.25F, 0.55F);
        if (t < 0.68F) return smoothSegment(t, 0.48F, 0.68F, 0.55F, 1.15F);
        if (emptyReload && t < 0.84F) return smoothSegment(t, 0.68F, 0.84F, 1.15F, 1.75F);
        return smoothSegment(t, emptyReload ? 0.84F : 0.68F, 1.0F,
                emptyReload ? 1.75F : 1.15F, 0.0F);
    }

    public static float reloadCameraYaw() {
        float t = reloadProgress(renderPartialTick());
        if (t <= 0.0F) return 0.0F;
        return (float) Math.sin(Math.PI * t) * 0.30F;
    }

    public static float reloadCameraRoll() {
        float t = reloadProgress(renderPartialTick());
        if (t <= 0.0F) return 0.0F;
        return (float) -Math.sin(2.0D * Math.PI * t) * 0.34F;
    }

    public static boolean reloadActive() {
        return reloadTime > 0.0F && reloadDuration > 0.0F;
    }

    private static float reloadProgress(float partialTick) {
        if (!reloadActive()) return 0.0F;
        return Mth.clamp(interpolate(previousReloadTime, reloadTime, partialTick) / reloadDuration,
                0.0F, 1.0F);
    }

    public static float slideTravel() {
        float time = interpolate(previousFireTime, fireTime, renderPartialTick());
        if (time <= 0.0F || time > 0.5F) {
            return 0.0F;
        }
        return Math.min(1.0F, 1.2F * Mth.sin(2.0F * Mth.PI * time));
    }

    public static float flashStrength() {
        return interpolate(previousFlash, flash, renderPartialTick());
    }

    public static float flashRotation() {
        return recoilSide * 0.18F;
    }

    public static float crosshairSpread() {
        float partialTick = renderPartialTick();
        float ads = easeInOutQuint(ClientWeaponController.viewmodelAdsBlend(partialTick));
        float movement = interpolate(previousMove, move, partialTick);
        float sprinting = interpolate(previousSprint, sprint, partialTick);
        float firing = fireBloomAtTime(interpolate(previousFireTime, fireTime, partialTick));
        return Mth.clamp(1.0F + movement * 1.5F + sprinting * 4.0F + firing * 2.2F
                - ads * 0.82F, 0.35F, 8.0F);
    }

    /**
     * Drive reticle bloom from the same primary pitch curve as the visible weapon recoil.
     * A binary "shot active" flag kept the reticle expanded through the tiny settling tail,
     * making bloom feel substantially slower than the pistol itself.
     */
    static float fireBloomAtTime(float time) {
        if (time <= 0.0F || time >= 3.0F) {
            return 0.0F;
        }
        return Mth.clamp(Math.abs(boneRotX(time)) / 6.38564F, 0.0F, 1.0F);
    }

    /**
     * Shared render-frame ADS blend for the outer rig, weapon mesh, and both player arms.
     * A single eased value prevents the independently approved endpoints from drifting apart
     * during the transition.
     */
    public static float adsPresentationBlend(float partialTick) {
        return easeInOutQuint(ClientWeaponController.viewmodelAdsBlend(partialTick));
    }

    public static float adsPresentationBlend() {
        return adsPresentationBlend(renderPartialTick());
    }

    public static float crosshairOffsetX() {
        float partialTick = renderPartialTick();
        float ads = easeInOutQuint(ClientWeaponController.viewmodelAdsBlend(partialTick));
        return interpolate(previousSwayY, swayY, partialTick)
                * (1.0F - ads * 0.82F) * 0.55F
                + (float) Math.sin(interpolate(previousWalkPhase, walkPhase, partialTick))
                * interpolate(previousMove, move, partialTick) * 1.35F;
    }

    public static float crosshairOffsetY() {
        float partialTick = renderPartialTick();
        float ads = easeInOutQuint(ClientWeaponController.viewmodelAdsBlend(partialTick));
        return interpolate(previousSwayX, swayX, partialTick)
                * (1.0F - ads * 0.82F) * 0.45F
                + (float) Math.abs(Math.cos(interpolate(previousWalkPhase, walkPhase, partialTick)))
                * interpolate(previousMove, move, partialTick) * 0.75F;
    }

    private static float interpolate(float previous, float current, float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previous, current);
    }

    static float movementPhaseSpeed(float sprintBlend) {
        // One complete render bob cycle spans two phase units because the primary curve uses
        // sin(PI * phase). These rates keep walking near one cycle/second and sprinting near
        // 1.4 cycles/second instead of the former four-to-five cycles/second.
        return Mth.lerp(Mth.clamp(sprintBlend, 0.0F, 1.0F), 0.10F, 0.14F);
    }

    static float sprintBlendStep(boolean sprinting) {
        // Preserve the weighted raise into sprint, but let release return to the approved
        // hip-fire endpoint in about three ticks instead of lingering for six or seven.
        return sprinting ? 0.16F : 0.34F;
    }

    private static float renderPartialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
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
