package com.hbm.client.weapon;

import com.hbm.HbmNuclearTech;
import com.hbm.client.weapon.render.SuperbGunPresentationState;
import com.hbm.client.weapon.render.HbmBulletTracerRenderer;
import com.hbm.config.HbmClientConfig;
import com.hbm.item.HbmGunItem;
import com.hbm.network.WeaponCommand;
import com.hbm.network.WeaponCommandPayload;
import com.hbm.network.WeaponEffectPayload;
import com.hbm.network.WeaponEffectType;
import com.hbm.network.WeaponInput;
import com.hbm.network.WeaponInputPayload;
import com.hbm.network.WeaponStatePayload;
import com.hbm.registry.HbmDataComponents;
import com.hbm.registry.HbmItems;
import com.hbm.weapon.state.ReloadPhase;
import com.hbm.weapon.state.GunState;
import com.hbm.weapon.state.RecoilAccumulator;
import com.hbm.weapon.state.WeaponSession;
import com.hbm.weapon.ballistics.WeaponAim;
import com.hbm.weapon.data.GunDefinition;
import com.hbm.weapon.data.GunDefinitionRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client input, prediction, and presentation for HBM firearms. The segmented crosshair and
 * bottom-right ammunition presentation adapt the behavior and information hierarchy of Superb
 * Warfare's CrossHairOverlay and AmmoBarOverlay at commit
 * 9b5284f42ef79532e6fb7f03ab07425c693b0b43 (GPL-3.0), using only HBM/vanilla rendering assets.
 */
@EventBusSubscriber(modid = HbmNuclearTech.MOD_ID, value = Dist.CLIENT)
public final class ClientWeaponController {
    private static final ResourceLocation TARGET_PISTOL = ResourceLocation.fromNamespaceAndPath(
            HbmNuclearTech.MOD_ID, "gun_star_f");
    private static final float TARGET_PISTOL_FOV = 0.82F;
    private static final double TARGET_PISTOL_SENSITIVITY = 0.76D;
    private static final String CATEGORY = "key.categories.hbm.weapons";
    private static final KeyMapping RELOAD = key("key.hbm.reload", GLFW.GLFW_KEY_R);
    private static final KeyMapping FIRE_MODE = key("key.hbm.fire_mode", GLFW.GLFW_KEY_B);
    private static final KeyMapping AMMO = key("key.hbm.ammo", GLFW.GLFW_KEY_N);
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final SprintFireTransition SPRINT_FIRE = new SprintFireTransition();
    private static final AdsSprintMomentum ADS_SPRINT_MOMENTUM = new AdsSprintMomentum();

    private static boolean lastFire;
    private static boolean lastAds;
    private static WeaponStatePayload authoritativeState;
    private static int lastAcknowledged = -1;
    private static boolean predictedRecoilPending;
    private static final RecoilAccumulator RECOIL = new RecoilAccumulator();
    private static float lastRecoilRecovery = 0.2F;
    private static int predictedAnimationTicks;
    private static float screenShake;
    private static Double sensitivityBeforeAds;
    private static Boolean viewBobbingBeforeAds;
    private static UUID lastAuthoritativeStack;
    private static String lastPresentationPose;
    private static float previousAdsBlend;
    private static float adsBlend;
    private static final HitFeedbackAnimation HIT_FEEDBACK = new HitFeedbackAnimation();
    private static final Map<Integer, Long> THIRD_PERSON_RELOAD_UNTIL = new HashMap<>();

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RELOAD);
        event.register(FIRE_MODE);
        event.register(AMMO);
    }

    @SubscribeEvent
    public static void interceptVanillaActions(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (isHoldingGun(minecraft) && (event.isAttack() || event.isUseItem())) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void suppressOffhandGun(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.OFF_HAND
                && event.getItemStack().getItem() instanceof HbmGunItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        // Opening chat/inventory blocks weapon input, but it must not invalidate the held
        // viewmodel. Invalidating here reset draw/stack state and exposed the fallback pose.
        boolean valid = isHoldingGun(minecraft);
        boolean inputAllowed = valid && minecraft.screen == null;
        GunState heldState = valid
                ? minecraft.player.getMainHandItem().get(HbmDataComponents.GUN_STATE.get())
                : null;
        boolean authoritativeHeld = authoritativeMatchesHeldStack(heldState);
        boolean actionPlaying = authoritativeHeld
                && authoritativeState.reloadPhase() != ReloadPhase.IDLE;
        boolean movingForward = minecraft.player != null
                && minecraft.player.input.hasForwardImpulse();
        boolean wasSprinting = valid && minecraft.player.isSprinting();
        boolean fire = inputAllowed && minecraft.options.keyAttack.isDown();
        boolean ads = inputAllowed && !actionPlaying && minecraft.options.keyUse.isDown();
        boolean adsPressed = !lastAds && ads;
        if (adsPressed && wasSprinting && minecraft.player.onGround()) {
            ADS_SPRINT_MOMENTUM.capture(
                    minecraft.player.getDeltaMovement().horizontalDistance());
        }
        if (ads && wasSprinting) {
            // ADS wins over sprint, matching Superb Warfare's input priority.
            minecraft.player.setSprinting(false);
        }

        previousAdsBlend = adsBlend;
        boolean adsActive = valid && !actionPlaying
                && (ads || authoritativeHeld && authoritativeState.ads());
        float adsTarget = adsActive ? 1.0F : 0.0F;
        adsBlend = Mth.approach(adsBlend, adsTarget, 0.18F);

        if (fire != lastFire) {
            // Ctrl may briefly reassert vanilla sprint while ADS is held. ADS fire must remain
            // immediate; only a genuine hip/sprint press enters the settle-fire-recovery path.
            boolean sprintTransition = fire
                    && WeaponSprintPolicy.shouldUseSprintFireTransition(wasSprinting, adsActive);
            sendInput(WeaponInput.FIRE, fire, sprintTransition);
            if (fire) {
                if (sprintTransition) {
                    // Drop to hipfire now; recoil occurs only after the server-owned settle
                    // window rather than while the weapon is still in its sprint pose.
                    minecraft.player.setSprinting(false);
                    SPRINT_FIRE.begin(
                            WeaponSession.SPRINT_FIRE_SETTLE_TICKS,
                            WeaponSession.SPRINT_FIRE_RECOVERY_TICKS);
                } else {
                    predictShot(minecraft, heldState);
                }
            }
            lastFire = fire;
        }
        boolean adsReleased = lastAds && !ads;
        if (ads != lastAds) {
            sendInput(WeaponInput.ADS, ads, false);
            lastAds = ads;
        }

        if (!valid) {
            SPRINT_FIRE.cancel();
        }
        SprintFireTransition.Result sprintFire = valid
                ? SPRINT_FIRE.tick(
                        minecraft.options.keySprint.isDown(),
                        movingForward,
                        adsActive)
                : SprintFireTransition.IDLE;
        if (sprintFire.holdHipfire()) {
            minecraft.player.setSprinting(false);
        }
        if (sprintFire.predictShot()) {
            predictShot(minecraft, heldState);
        }
        if (sprintFire.resumeSprint()) {
            minecraft.player.setSprinting(true);
        }
        if (WeaponSprintPolicy.shouldRestartAfterAds(
                adsReleased,
                inputAllowed,
                minecraft.options.keySprint.isDown(),
                movingForward,
                actionPlaying,
                SPRINT_FIRE.active())) {
            ADS_SPRINT_MOMENTUM.beginRestore();
        }
        AdsSprintMomentum.Result adsSprintRestart = ADS_SPRINT_MOMENTUM.tick(
                valid
                        && inputAllowed
                        && !ads
                        && minecraft.options.keySprint.isDown()
                        && movingForward
                        && !actionPlaying
                        && !SPRINT_FIRE.active(),
                valid ? minecraft.player.getDeltaMovement().horizontalDistance() : 0.0D);
        if (adsSprintRestart.restartSprint()) {
            // Reassert sprint while the server removes the ADS movement modifier, and restore
            // only the pace captured on ADS entry. Repeated RMB taps therefore cannot stack
            // velocity or manufacture momentum from a standing start.
            minecraft.player.setSprinting(true);
            restoreHorizontalMomentum(minecraft, adsSprintRestart.targetHorizontalSpeed());
        }

        if (inputAllowed) {
            while (RELOAD.consumeClick()) {
                SPRINT_FIRE.cancel();
                sendCommand(WeaponCommand.RELOAD);
            }
            while (FIRE_MODE.consumeClick()) {
                sendCommand(WeaponCommand.CYCLE_FIRE_MODE);
            }
            while (AMMO.consumeClick()) {
                sendCommand(WeaponCommand.CYCLE_AMMO);
            }
        }
        if (!valid) {
            authoritativeState = null;
            lastAuthoritativeStack = null;
            predictedRecoilPending = false;
            SPRINT_FIRE.cancel();
            ADS_SPRINT_MOMENTUM.cancel();
            SuperbGunPresentationState.reset();
        }
        boolean targetPistol = valid && isHoldingTargetPistol(minecraft);
        boolean lowerRequested = targetPistol && !ads && shouldLowerAtWall(minecraft);
        if (valid && !actionPlaying) {
            // Target Pistol equip, ADS, sprint, wall lowering, and recoil all belong to one
            // continuous presentation state. Gecko owns only mechanism and reload clips.
            String presentationPose = targetPistol ? "idle"
                    : !ads && shouldLowerAtWall(minecraft) ? "lower"
                    : minecraft.player.isSprinting() ? "sprint" : ads ? "ads" : "idle";
            if (!presentationPose.equals(lastPresentationPose)) {
                triggerAnimation(minecraft.player, presentationPose);
                lastPresentationPose = presentationPose;
            }
        } else if (!valid) {
            lastPresentationPose = null;
        }
        predictedAnimationTicks = Math.max(0, predictedAnimationTicks - 1);
        recoverRecoil(minecraft);
        updateAdsSensitivity(minecraft, adsActive);
        updateViewBobbing(minecraft, adsActive);
        SuperbGunPresentationState.tick(minecraft, valid,
                heldState == null ? null : heldState.stackIdentity(), lowerRequested);
        screenShake *= 0.78F;
        HIT_FEEDBACK.tick();
        if (minecraft.level != null) {
            long now = minecraft.level.getGameTime();
            THIRD_PERSON_RELOAD_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        } else {
            THIRD_PERSON_RELOAD_UNTIL.clear();
        }
    }

    @SubscribeEvent
    public static void modifyFov(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (isHoldingGun(minecraft) && adsBlend > 0.001F) {
            GunState heldState = minecraft.player.getMainHandItem().get(HbmDataComponents.GUN_STATE.get());
            float target = !authoritativeMatchesHeldStack(heldState)
                    ? TARGET_PISTOL_FOV : authoritativeState.adsFovMultiplier();
            float multiplier = Mth.lerp(easeInOutQuint(viewmodelAdsBlend()), 1.0F, target);
            event.setNewFovModifier(event.getNewFovModifier() * multiplier);
        }
    }

    @SubscribeEvent
    public static void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        double time = Minecraft.getInstance().level == null ? 0.0D : Minecraft.getInstance().level.getGameTime();
        float firePitch = (float) Math.sin(time * 2.31D) * screenShake;
        float fireYaw = (float) Math.cos(time * 1.73D) * screenShake * 0.7F;
        float fireRoll = (float) Math.sin(time * 1.17D) * screenShake * 0.35F;
        float reloadScale = HbmClientConfig.SCREEN_SHAKE_SCALE.get().floatValue();
        event.setPitch(event.getPitch() + firePitch
                + SuperbGunPresentationState.reloadCameraPitch() * reloadScale);
        event.setYaw(event.getYaw() + fireYaw
                + SuperbGunPresentationState.reloadCameraYaw() * reloadScale);
        event.setRoll(event.getRoll() + fireRoll
                + SuperbGunPresentationState.reloadCameraRoll() * reloadScale);
    }

    private static void restoreHorizontalMomentum(Minecraft minecraft, double targetSpeed) {
        Vec3 velocity = minecraft.player.getDeltaMovement();
        double currentSpeed = velocity.horizontalDistance();
        if (targetSpeed <= currentSpeed + 1.0E-6D) {
            return;
        }
        Vec3 direction = currentSpeed > 1.0E-4D
                ? new Vec3(velocity.x, 0.0D, velocity.z).normalize()
                : new Vec3(minecraft.player.getLookAngle().x, 0.0D,
                        minecraft.player.getLookAngle().z).normalize();
        if (direction.lengthSqr() > 1.0E-8D) {
            minecraft.player.setDeltaMovement(
                    direction.x * targetSpeed,
                    velocity.y,
                    direction.z * targetSpeed);
        }
    }

    @SubscribeEvent
    public static void replaceVanillaCrosshair(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName()) && isHoldingGun(minecraft)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHoldingGun(minecraft) || minecraft.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        renderCrosshair(graphics, minecraft,
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
        GunState heldState = minecraft.player.getMainHandItem().get(HbmDataComponents.GUN_STATE.get());
        if (!authoritativeMatchesHeldStack(heldState)) {
            return;
        }
        renderAmmoPanel(graphics, minecraft, authoritativeState.state());
    }

    public static void acceptState(WeaponStatePayload payload) {
        if (payload.acknowledgedSequence() < lastAcknowledged) {
            return;
        }
        boolean changedStack = lastAuthoritativeStack == null
                || !lastAuthoritativeStack.equals(payload.state().stackIdentity());
        lastAcknowledged = payload.acknowledgedSequence();
        authoritativeState = payload;
        lastRecoilRecovery = Math.max(0.0F, payload.recoilRecoveryPerTick());
        lastAuthoritativeStack = payload.state().stackIdentity();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            GunState local = minecraft.player.getMainHandItem().get(HbmDataComponents.GUN_STATE.get());
            boolean matchesHeld = local != null
                    && local.stackIdentity().equals(payload.state().stackIdentity());
            if (matchesHeld) {
                minecraft.player.getMainHandItem().set(HbmDataComponents.GUN_STATE.get(), payload.state());
            }
            if (changedStack && matchesHeld) {
                if (!isHoldingTargetPistol(minecraft)) {
                    triggerAnimation(minecraft.player, "equip");
                }
                SuperbGunPresentationState.beginEquip();
                lastPresentationPose = null;
            }
        }
    }

    public static void acceptEffect(WeaponEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        boolean local = minecraft.player != null && payload.sourceEntityId() == minecraft.player.getId();
        boolean localFirstPerson = local && minecraft.options.getCameraType().isFirstPerson();
        if (local) {
            switch (payload.effect()) {
                case HIT -> HIT_FEEDBACK.start(HitFeedbackAnimation.Kind.HIT);
                case HEADSHOT -> HIT_FEEDBACK.start(HitFeedbackAnimation.Kind.HEADSHOT);
                case KILL -> HIT_FEEDBACK.start(HitFeedbackAnimation.Kind.KILL);
                case HEADSHOT_KILL -> HIT_FEEDBACK.start(HitFeedbackAnimation.Kind.HEADSHOT_KILL);
                default -> {
                }
            }
        }
        if (local && (payload.effect() == WeaponEffectType.FIRE
                || payload.effect() == WeaponEffectType.DRY_FIRE)) {
            SPRINT_FIRE.acknowledgeAttempt();
        }
        if (payload.effect() == WeaponEffectType.FIRE && local) {
            boolean alreadyPredicted = predictedRecoilPending;
            applyAuthoritativeRecoil(minecraft, payload);
            if (!alreadyPredicted) {
                SuperbGunPresentationState.fire();
            }
            screenShake = Math.max(screenShake,
                    payload.pitch() * 0.12F * HbmClientConfig.SCREEN_SHAKE_SCALE.get().floatValue());
        }
        Entity source = payload.sourceEntityId() < 0 ? null : minecraft.level.getEntity(payload.sourceEntityId());
        switch (payload.effect()) {
            case FIRE -> {
                if (!local || predictedAnimationTicks == 0) {
                    triggerAnimation(source, "fire");
                }
                predictedAnimationTicks = 0;
            }
            case DRY_FIRE -> {
                predictedRecoilPending = false;
                predictedAnimationTicks = 0;
                triggerAnimation(source, "dry_fire");
            }
            case RELOAD_START -> {
                rememberThirdPersonReload(payload);
                if (TARGET_PISTOL.equals(payload.gunId())) {
                    boolean empty = payload.variant() != 0;
                    triggerAnimation(source, empty ? "reload_empty" : "reload_normal");
                    if (local) {
                        SuperbGunPresentationState.beginReload(empty);
                    }
                } else {
                    triggerAnimation(source, "reload_start");
                }
            }
            case RELOAD_INSERT -> {
                if (!TARGET_PISTOL.equals(payload.gunId())) {
                    triggerAnimation(source, "reload_loop");
                }
            }
            case RELOAD_END -> {
                if (!TARGET_PISTOL.equals(payload.gunId())) {
                    triggerAnimation(source, "reload_end");
                }
            }
            default -> {
            }
        }
        switch (payload.effect()) {
            // The first-person model renders its own muzzle flash. World-space flame particles
            // were oversized and visibly detached from the pistol in third person.
            case MUZZLE_FLASH -> {
            }
            case SMOKE -> {
                if (localFirstPerson) {
                    minecraft.level.addParticle(ParticleTypes.SMOKE,
                            payload.x(), payload.y(), payload.z(), 0.0D, 0.025D, 0.0D);
                }
            }
            case CASING -> {
                if (localFirstPerson && HbmClientConfig.CASING_PARTICLES.get()) {
                    Vec3 look = source == null ? new Vec3(0.0D, 0.0D, 1.0D) : source.getLookAngle();
                    Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
                    if (right.lengthSqr() < 1.0E-6D) {
                        right = new Vec3(1.0D, 0.0D, 0.0D);
                    } else {
                        right = right.normalize();
                    }
                    Vec3 velocity = right.scale(0.11D)
                            .add(0.0D, 0.075D, 0.0D)
                            .subtract(look.scale(0.018D));
                    Particle particle = minecraft.particleEngine.createParticle(
                            new ItemParticleOption(ParticleTypes.ITEM,
                                    HbmItems.CASING_SMALL.get().getDefaultInstance()),
                            payload.x(), payload.y(), payload.z(),
                            velocity.x, velocity.y, velocity.z);
                    if (particle != null) {
                        particle.scale(0.42F);
                    }
                }
            }
            case TRACER -> {
                if (HbmClientConfig.TRACERS.get()) {
                    HbmBulletTracerRenderer.enqueue(
                            minecraft.level,
                            new Vec3(payload.x(), payload.y(), payload.z()),
                            new Vec3(payload.endX(), payload.endY(), payload.endZ()),
                            source == null ? null : source.getEyePosition(),
                            payload.variant(),
                            local);
                }
            }
            case IMPACT -> minecraft.level.addParticle(ParticleTypes.CRIT,
                    payload.x(), payload.y(), payload.z(), 0.0D, 0.0D, 0.0D);
            case EXPLOSION -> minecraft.level.addParticle(ParticleTypes.EXPLOSION_EMITTER,
                    payload.x(), payload.y(), payload.z(), 0.0D, 0.0D, 0.0D);
            default -> {
            }
        }
    }

    private static void applyPredictedRecoil(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        float scale = HbmClientConfig.RECOIL_CAMERA_SCALE.get().floatValue();
        float pitch = 0.35F * scale;
        minecraft.player.setXRot(Mth.clamp(minecraft.player.getXRot() - pitch, -90.0F, 90.0F));
        RECOIL.add(pitch, 0.0F);
        predictedRecoilPending = true;
    }

    private static void applyAuthoritativeRecoil(Minecraft minecraft, WeaponEffectPayload payload) {
        if (minecraft.player == null) {
            return;
        }
        float scale = HbmClientConfig.RECOIL_CAMERA_SCALE.get().floatValue();
        float predicted = predictedRecoilPending ? 0.35F : 0.0F;
        predictedRecoilPending = false;
        float pitch = Math.max(0.0F, payload.pitch() - predicted) * scale;
        float yaw = payload.yaw() * scale * (minecraft.player.getRandom().nextBoolean() ? 1.0F : -1.0F);
        minecraft.player.setXRot(Mth.clamp(minecraft.player.getXRot() - pitch, -90.0F, 90.0F));
        minecraft.player.setYRot(minecraft.player.getYRot() + yaw);
        RECOIL.add(pitch, yaw);
    }

    private static void recoverRecoil(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        float recovery = lastRecoilRecovery
                * HbmClientConfig.RECOIL_CAMERA_SCALE.get().floatValue();
        if (recovery <= 0.0F) {
            return;
        }
        RecoilAccumulator.Recovery step = RECOIL.recover(recovery);
        if (step.pitch() > 0.0F) {
            minecraft.player.setXRot(Mth.clamp(minecraft.player.getXRot() + step.pitch(), -90.0F, 90.0F));
        }
        if (step.yaw() != 0.0F) {
            minecraft.player.setYRot(minecraft.player.getYRot() - step.yaw());
        }
    }

    private static void updateAdsSensitivity(Minecraft minecraft, boolean ads) {
        if (ads && isHoldingGun(minecraft)) {
            if (sensitivityBeforeAds == null) {
                sensitivityBeforeAds = minecraft.options.sensitivity().get();
            }
            GunState heldState = minecraft.player.getMainHandItem().get(HbmDataComponents.GUN_STATE.get());
            double targetMultiplier = (!authoritativeMatchesHeldStack(heldState)
                    ? TARGET_PISTOL_SENSITIVITY : authoritativeState.adsSensitivityMultiplier())
                    * HbmClientConfig.ADS_SENSITIVITY_SCALE.get();
            double multiplier = Mth.lerp(easeInOutQuint(viewmodelAdsBlend()),
                    1.0D, targetMultiplier);
            minecraft.options.sensitivity().set(sensitivityBeforeAds * multiplier);
        } else if (sensitivityBeforeAds != null) {
            minecraft.options.sensitivity().set(sensitivityBeforeAds);
            sensitivityBeforeAds = null;
        }
    }

    private static void predictShot(Minecraft minecraft, GunState heldState) {
        boolean predictedLiveRound = heldState == null || heldState.ammoCount() > 0;
        if (predictedLiveRound) {
            applyPredictedRecoil(minecraft);
            SuperbGunPresentationState.fire();
            triggerAnimation(minecraft.player, "fire");
        } else {
            triggerAnimation(minecraft.player, "dry_fire");
        }
        predictedAnimationTicks = 6;
    }

    private static void updateViewBobbing(Minecraft minecraft, boolean ads) {
        if (ads) {
            if (viewBobbingBeforeAds == null) {
                viewBobbingBeforeAds = minecraft.options.bobView().get();
            }
            if (minecraft.options.bobView().get()) {
                minecraft.options.bobView().set(false);
            }
        } else if (viewBobbingBeforeAds != null) {
            minecraft.options.bobView().set(viewBobbingBeforeAds);
            viewBobbingBeforeAds = null;
        }
    }

    private static void sendInput(WeaponInput input, boolean pressed,
                                  boolean sprintTransitionRequested) {
        PacketDistributor.sendToServer(new WeaponInputPayload(
                input, pressed, sprintTransitionRequested, nextSequence()));
    }

    private static void sendCommand(WeaponCommand command) {
        PacketDistributor.sendToServer(new WeaponCommandPayload(command, nextSequence()));
    }

    private static void triggerAnimation(Entity entity, String animation) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        ItemStack stack = living.getMainHandItem();
        if (stack.getItem() instanceof HbmGunItem gun) {
            gun.triggerAnim(entity, GeoItem.getId(stack), HbmGunItem.ANIMATION_CONTROLLER, animation);
        }
    }

    private static int nextSequence() {
        return SEQUENCE.updateAndGet(value -> value >= 1_000_000_000 ? 0 : value + 1);
    }

    private static boolean isHoldingGun(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player.getMainHandItem().getItem() instanceof HbmGunItem;
    }

    private static boolean isHoldingTargetPistol(Minecraft minecraft) {
        if (!isHoldingGun(minecraft)) {
            return false;
        }
        return minecraft.player.getMainHandItem().getItem() instanceof HbmGunItem gun
                && TARGET_PISTOL.equals(gun.definitionId());
    }

    private static boolean authoritativeMatchesHeldStack(GunState heldState) {
        return heldState != null && authoritativeState != null
                && heldState.stackIdentity().equals(authoritativeState.state().stackIdentity());
    }

    private static void renderCrosshair(GuiGraphics graphics, Minecraft minecraft,
                                        float partialTick) {
        if (minecraft.player == null
                || minecraft.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            return;
        }
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        float currentAds = viewmodelAdsBlend();
        if (!minecraft.player.isSprinting() && (!firstPerson || currentAds <= 0.20F)) {
            // Hip fire remains centered on the authoritative camera ray. Movement changes
            // bloom, not the reticle zero, so the visible crosshair and trajectory agree.
            int centerX = graphics.guiWidth() / 2;
            int centerY = graphics.guiHeight() / 2;
            int gap = (firstPerson ? 3 : 5)
                    + Math.round(2.8F * SuperbGunPresentationState.crosshairSpread());
            int arm = firstPerson ? 5 : 4;
            int white = 0xEEFFFFFF;
            int shadow = 0xB0000000;

            if (firstPerson) {
                graphics.fill(centerX, centerY, centerX + 1, centerY + 1, white);
            }
            horizontalLine(graphics, centerX - gap - arm, centerX - gap, centerY, shadow, white);
            horizontalLine(graphics, centerX + gap, centerX + gap + arm, centerY, shadow, white);
            verticalLine(graphics, centerX, centerY - gap - arm, centerY - gap, shadow, white);
            verticalLine(graphics, centerX, centerY + gap, centerY + gap + arm, shadow, white);
        }

        int feedbackX = graphics.guiWidth() / 2;
        int feedbackY = graphics.guiHeight() / 2 + (firstPerson
                ? adsAimOffsetY(minecraft, graphics.guiHeight(), currentAds) : 0);
        drawHitFeedback(graphics, feedbackX, feedbackY,
                HIT_FEEDBACK.sample(partialTick));
    }

    private static void renderAmmoPanel(GuiGraphics graphics, Minecraft minecraft, GunState state) {
        ItemStack held = minecraft.player.getMainHandItem();
        ItemStack ammoIcon = BuiltInRegistries.ITEM.get(state.selectedAmmoId()).getDefaultInstance();
        String gunName = held.getHoverName().getString();
        String ammoName = ammoIcon.isEmpty()
                ? state.selectedAmmoId().getPath()
                : ammoIcon.getHoverName().getString();
        String fireMode = state.fireMode().name().replace('_', ' ');
        String status = authoritativeState.reloadPhase() == ReloadPhase.IDLE
                ? fireMode
                : authoritativeState.reloadPhase().name().replace('_', ' ');
        int reserve = countReserveAmmo(minecraft, state.selectedAmmoId());
        String reserveText = minecraft.player.getAbilities().instabuild ? "\u221e" : Integer.toString(reserve);

        int right = graphics.guiWidth() - 9;
        int bottom = graphics.guiHeight() - 9;
        int left = right - 104;
        int top = bottom - 43;
        graphics.drawString(minecraft.font, Component.literal(gunName), left, top,
                0xE8FFFFFF, true);
        graphics.drawString(minecraft.font, Component.literal(ammoName), left, top + 11,
                0xC8CBD5DC, true);
        graphics.drawString(minecraft.font, Component.literal(status), left, bottom - 9,
                0xB8CAD5DD, true);

        String magazine = Integer.toString(state.ammoCount());
        float countScale = 1.65F;
        graphics.pose().pushPose();
        graphics.pose().translate(right - 22 - minecraft.font.width(magazine) * countScale,
                top + 17, 0.0F);
        graphics.pose().scale(countScale, countScale, 1.0F);
        graphics.drawString(minecraft.font, Component.literal(magazine), 0, 0,
                0xFFFFFFFF, true);
        graphics.pose().popPose();
        graphics.drawString(minecraft.font, Component.literal("/ " + reserveText),
                right - 19, top + 24, 0xD8B7C5CF, true);
        if (!ammoIcon.isEmpty()) {
            graphics.renderItem(ammoIcon, right - 17, top - 1);
        }
    }

    private static int countReserveAmmo(Minecraft minecraft, net.minecraft.resources.ResourceLocation ammoId) {
        if (minecraft.player == null) {
            return 0;
        }
        var ammoItem = BuiltInRegistries.ITEM.get(ammoId);
        int count = 0;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack.is(ammoItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void horizontalLine(GuiGraphics graphics, int x1, int x2, int y,
                                       int shadow, int color) {
        graphics.fill(x1 - 1, y - 1, x2 + 1, y + 2, shadow);
        graphics.fill(x1, y, x2, y + 1, color);
    }

    private static void verticalLine(GuiGraphics graphics, int x, int y1, int y2,
                                     int shadow, int color) {
        graphics.fill(x - 1, y1 - 1, x + 2, y2 + 1, shadow);
        graphics.fill(x, y1, x + 1, y2, color);
    }

    private static void drawHitFeedback(GuiGraphics graphics, int centerX, int centerY,
                                        HitFeedbackAnimation.Frame frame) {
        if (frame.kind() == HitFeedbackAnimation.Kind.NONE || frame.alpha() <= 0.01F) {
            return;
        }
        int gap = 3 + Math.round(5.0F * frame.expansion());
        int alpha = Mth.clamp(Math.round(frame.alpha() * 255.0F), 0, 255);
        int rgb = switch (frame.kind()) {
            case HIT, HEADSHOT -> 0xFFFFFF;
            case KILL, HEADSHOT_KILL -> 0xFF2525;
            case NONE -> 0;
        };
        int color = alpha << 24 | rgb;
        int shadow = Math.round(alpha * 0.45F) << 24;
        for (int step = gap; step < gap + frame.armLength(); step++) {
            pixel(graphics, centerX - step, centerY - step, shadow, color);
            pixel(graphics, centerX + step, centerY - step, shadow, color);
            pixel(graphics, centerX - step, centerY + step, shadow, color);
            pixel(graphics, centerX + step, centerY + step, shadow, color);
        }
        if (frame.kind() == HitFeedbackAnimation.Kind.HEADSHOT
                || frame.kind() == HitFeedbackAnimation.Kind.HEADSHOT_KILL) {
            int plus = alpha << 24 | 0xFF2525;
            // Four separated strokes form the inner headshot plus without reintroducing the
            // former center dot.
            graphics.fill(centerX - 3, centerY, centerX, centerY + 1, plus);
            graphics.fill(centerX + 1, centerY, centerX + 4, centerY + 1, plus);
            graphics.fill(centerX, centerY - 3, centerX + 1, centerY, plus);
            graphics.fill(centerX, centerY + 1, centerX + 1, centerY + 4, plus);
        }
    }

    private static void pixel(GuiGraphics graphics, int x, int y, int shadow, int color) {
        graphics.fill(x - 1, y - 1, x + 2, y + 2, shadow);
        graphics.fill(x, y, x + 1, y + 1, color);
    }

    private static int adsAimOffsetY(Minecraft minecraft, int guiHeight, float adsAmount) {
        if (minecraft.player == null || adsAmount <= 0.001F) {
            return 0;
        }
        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof HbmGunItem gun)) {
            return 0;
        }
        GunDefinition definition = GunDefinitionRegistry.gun(gun.definitionId());
        if (definition == null) {
            return 0;
        }
        double zeroPitch = definition.getAds().getZeroPitchDegrees() * adsAmount;
        double baseFov = minecraft.options.fov().get();
        double currentFov = Mth.lerp(adsAmount, baseFov,
                baseFov * definition.getAds().getFovMultiplier());
        return WeaponAim.screenOffsetY(zeroPitch, currentFov, guiHeight);
    }

    private static boolean shouldLowerAtWall(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return minecraft.player.getEyePosition().distanceToSqr(minecraft.hitResult.getLocation()) < 1.44D;
    }

    public static float viewmodelAdsBlend() {
        return viewmodelAdsBlend(Minecraft.getInstance().getTimer()
                .getGameTimeDeltaPartialTick(true));
    }

    public static float viewmodelAdsBlend(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previousAdsBlend, adsBlend);
    }

    public static float viewmodelRecoilPitch() {
        return RECOIL.pitchDebt();
    }

    public static float viewmodelRecoilYaw() {
        return RECOIL.yawDebt();
    }

    public static boolean reloadIdle() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHoldingGun(minecraft)) {
            return true;
        }
        GunState heldState = minecraft.player.getMainHandItem().get(HbmDataComponents.GUN_STATE.get());
        return !authoritativeMatchesHeldStack(heldState)
                || authoritativeState.reloadPhase() == ReloadPhase.IDLE;
    }

    public static boolean thirdPersonReloading(LivingEntity entity) {
        if (entity == null || entity.level() == null) {
            return false;
        }
        Long until = THIRD_PERSON_RELOAD_UNTIL.get(entity.getId());
        return until != null && until >= entity.level().getGameTime();
    }

    private static void rememberThirdPersonReload(WeaponEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || payload.sourceEntityId() < 0) {
            return;
        }
        GunDefinition definition = GunDefinitionRegistry.gun(payload.gunId());
        if (definition == null) {
            return;
        }
        GunDefinition.ReloadProfile reload = definition.getReload();
        int ending = payload.variant() != 0 ? reload.getEmptyEndTicks() : reload.getEndTicks();
        int duration = Math.max(1, reload.getStartTicks() + reload.getTransferTicks() + ending);
        THIRD_PERSON_RELOAD_UNTIL.put(payload.sourceEntityId(),
                minecraft.level.getGameTime() + duration);
    }

    private static float easeInOutQuint(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t < 0.5F
                ? 16.0F * t * t * t * t * t
                : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 5.0D) / 2.0F;
    }

    private static KeyMapping key(String translation, int defaultKey) {
        return new KeyMapping(translation, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    private ClientWeaponController() {
    }
}
