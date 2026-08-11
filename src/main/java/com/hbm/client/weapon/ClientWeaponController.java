package com.hbm.client.weapon;

import com.hbm.HbmNuclearTech;
import com.hbm.client.weapon.render.SuperbGunPresentationState;
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
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.UUID;
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
    private static UUID lastAuthoritativeStack;
    private static String lastPresentationPose;
    private static float previousAdsBlend;
    private static float adsBlend;
    private static int hitMarkerTicks;
    private static int headshotMarkerTicks;

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
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean valid = isHoldingGun(minecraft) && minecraft.screen == null;
        boolean fire = valid && minecraft.options.keyAttack.isDown();
        boolean ads = valid && minecraft.options.keyUse.isDown() && !minecraft.player.isSprinting();

        previousAdsBlend = adsBlend;
        float adsTarget = valid && (ads || authoritativeState != null && authoritativeState.ads()) ? 1.0F : 0.0F;
        adsBlend = Mth.approach(adsBlend, adsTarget, 0.18F);

        if (fire != lastFire) {
            sendInput(WeaponInput.FIRE, fire);
            if (fire) {
                GunState localState = minecraft.player.getMainHandItem().get(HbmDataComponents.GUN_STATE.get());
                boolean predictedLiveRound = localState == null || localState.ammoCount() > 0;
                if (predictedLiveRound) {
                    applyPredictedRecoil(minecraft);
                    SuperbGunPresentationState.fire();
                    triggerAnimation(minecraft.player, "fire");
                } else {
                    triggerAnimation(minecraft.player, "dry_fire");
                }
                predictedAnimationTicks = 6;
            }
            lastFire = fire;
        }
        if (ads != lastAds) {
            sendInput(WeaponInput.ADS, ads);
            lastAds = ads;
        }

        if (valid) {
            while (RELOAD.consumeClick()) {
                sendCommand(WeaponCommand.RELOAD);
            }
            while (FIRE_MODE.consumeClick()) {
                sendCommand(WeaponCommand.CYCLE_FIRE_MODE);
            }
            while (AMMO.consumeClick()) {
                sendCommand(WeaponCommand.CYCLE_AMMO);
            }
        } else {
            authoritativeState = null;
            lastAuthoritativeStack = null;
            predictedRecoilPending = false;
            SuperbGunPresentationState.reset();
        }
        boolean actionPlaying = authoritativeState != null
                && authoritativeState.reloadPhase() != ReloadPhase.IDLE;
        if (valid && !actionPlaying) {
            boolean targetPistol = isHoldingTargetPistol(minecraft);
            // Procedural presentation already owns Target Pistol ADS and sprinting. Replaying
            // Gecko pose clips on the same root doubled those transforms and caused the
            // intermittent below-hotbar and sideways-hand transitions seen in the audit video.
            String presentationPose = !ads && shouldLowerAtWall(minecraft)
                    ? "lower"
                    : targetPistol ? "idle"
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
        updateAdsSensitivity(minecraft, valid && (lastAds || authoritativeState != null && authoritativeState.ads()));
        SuperbGunPresentationState.tick(minecraft, valid);
        screenShake *= 0.78F;
        hitMarkerTicks = Math.max(0, hitMarkerTicks - 1);
        headshotMarkerTicks = Math.max(0, headshotMarkerTicks - 1);
    }

    @SubscribeEvent
    public static void modifyFov(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (isHoldingGun(minecraft) && adsBlend > 0.001F) {
            float target = authoritativeState == null
                    ? TARGET_PISTOL_FOV : authoritativeState.adsFovMultiplier();
            float multiplier = Mth.lerp(easeInOutQuint(adsBlend), 1.0F, target);
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
        renderCrosshair(graphics, minecraft);
        if (authoritativeState == null) {
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
            if (local != null && local.stackIdentity().equals(payload.state().stackIdentity())) {
                minecraft.player.getMainHandItem().set(HbmDataComponents.GUN_STATE.get(), payload.state());
            }
            if (changedStack) {
                triggerAnimation(minecraft.player, "equip");
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
        if (local && payload.effect() == WeaponEffectType.IMPACT) {
            hitMarkerTicks = Math.max(hitMarkerTicks, 8);
        } else if (local && payload.effect() == WeaponEffectType.HEADSHOT) {
            hitMarkerTicks = Math.max(hitMarkerTicks, 10);
            headshotMarkerTicks = Math.max(headshotMarkerTicks, 12);
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
            case MUZZLE_FLASH -> {
                if (!localFirstPerson) {
                    minecraft.level.addParticle(ParticleTypes.FLAME,
                            payload.x(), payload.y(), payload.z(), 0.0D, 0.004D, 0.0D);
                }
            }
            case SMOKE -> {
                if (!localFirstPerson) {
                    minecraft.level.addParticle(ParticleTypes.SMOKE,
                            payload.x(), payload.y(), payload.z(), 0.0D, 0.025D, 0.0D);
                }
            }
            case CASING -> {
                if (HbmClientConfig.CASING_PARTICLES.get()) {
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
                    minecraft.level.addParticle(
                            new ItemParticleOption(ParticleTypes.ITEM,
                                    HbmItems.CASING_SMALL.get().getDefaultInstance()),
                            payload.x(), payload.y(), payload.z(),
                            velocity.x, velocity.y, velocity.z);
                }
            }
            case TRACER -> {
                if (HbmClientConfig.TRACERS.get()) {
                    minecraft.level.addParticle(ParticleTypes.END_ROD,
                            payload.x(), payload.y(), payload.z(), 0.0D, 0.0D, 0.0D);
                }
            }
            case IMPACT -> minecraft.level.addParticle(ParticleTypes.CRIT,
                    payload.x(), payload.y(), payload.z(), 0.0D, 0.0D, 0.0D);
            case HEADSHOT -> minecraft.level.addParticle(ParticleTypes.ENCHANTED_HIT,
                    payload.x(), payload.y(), payload.z(), 0.0D, 0.05D, 0.0D);
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
            double targetMultiplier = (authoritativeState == null
                    ? TARGET_PISTOL_SENSITIVITY : authoritativeState.adsSensitivityMultiplier())
                    * HbmClientConfig.ADS_SENSITIVITY_SCALE.get();
            double multiplier = Mth.lerp(easeInOutQuint(adsBlend), 1.0D, targetMultiplier);
            minecraft.options.sensitivity().set(sensitivityBeforeAds * multiplier);
        } else if (sensitivityBeforeAds != null) {
            minecraft.options.sensitivity().set(sensitivityBeforeAds);
            sensitivityBeforeAds = null;
        }
    }

    private static void sendInput(WeaponInput input, boolean pressed) {
        PacketDistributor.sendToServer(new WeaponInputPayload(input, pressed, nextSequence()));
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

    private static void renderCrosshair(GuiGraphics graphics, Minecraft minecraft) {
        if (!minecraft.options.getCameraType().isFirstPerson() || minecraft.player == null) {
            return;
        }
        float currentAds = viewmodelAdsBlend();
        if (!minecraft.player.isSprinting() && currentAds <= 0.20F) {
            int centerX = graphics.guiWidth() / 2
                    + Math.round(SuperbGunPresentationState.crosshairOffsetX());
            int centerY = graphics.guiHeight() / 2
                    + Math.round(SuperbGunPresentationState.crosshairOffsetY());
            int gap = 3 + Math.round(2.8F * SuperbGunPresentationState.crosshairSpread());
            int arm = 5;
            int white = 0xEEFFFFFF;
            int shadow = 0xB0000000;

            graphics.fill(centerX, centerY, centerX + 1, centerY + 1, white);
            horizontalLine(graphics, centerX - gap - arm, centerX - gap, centerY, shadow, white);
            horizontalLine(graphics, centerX + gap, centerX + gap + arm, centerY, shadow, white);
            verticalLine(graphics, centerX, centerY - gap - arm, centerY - gap, shadow, white);
            verticalLine(graphics, centerX, centerY + gap, centerY + gap + arm, shadow, white);
        } else if (!minecraft.player.isSprinting()) {
            // Preserve a restrained centre reference while the iron sights settle. The former
            // hard disappearance made it impossible to distinguish aligned ADS from a bad pose.
            int centerX = graphics.guiWidth() / 2;
            int centerY = graphics.guiHeight() / 2;
            graphics.fill(centerX, centerY, centerX + 1, centerY + 1, 0xB8FFFFFF);
        }

        if (hitMarkerTicks > 0) {
            drawHitMarker(graphics, graphics.guiWidth() / 2, graphics.guiHeight() / 2,
                    headshotMarkerTicks > 0 ? 0xFFFFC95A : 0xFFFFFFFF);
        }
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

    private static void drawHitMarker(GuiGraphics graphics, int centerX, int centerY, int color) {
        for (int step = 3; step <= 7; step++) {
            graphics.fill(centerX - step, centerY - step, centerX - step + 1, centerY - step + 1, color);
            graphics.fill(centerX + step, centerY - step, centerX + step + 1, centerY - step + 1, color);
            graphics.fill(centerX - step, centerY + step, centerX - step + 1, centerY + step + 1, color);
            graphics.fill(centerX + step, centerY + step, centerX + step + 1, centerY + step + 1, color);
        }
    }

    private static boolean shouldLowerAtWall(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return minecraft.player.getEyePosition().distanceToSqr(minecraft.hitResult.getLocation()) < 1.44D;
    }

    public static float viewmodelAdsBlend() {
        return (previousAdsBlend + adsBlend) * 0.5F;
    }

    public static float viewmodelRecoilPitch() {
        return RECOIL.pitchDebt();
    }

    public static float viewmodelRecoilYaw() {
        return RECOIL.yawDebt();
    }

    public static boolean reloadIdle() {
        return authoritativeState == null || authoritativeState.reloadPhase() == ReloadPhase.IDLE;
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
