package com.hbm.client.weapon;

import com.hbm.HbmNuclearTech;
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
import com.hbm.weapon.state.GunState;
import com.hbm.weapon.state.RecoilAccumulator;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@EventBusSubscriber(modid = HbmNuclearTech.MOD_ID, value = Dist.CLIENT)
public final class ClientWeaponController {
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
    private static boolean lastSprintAnimation;

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

        if (fire != lastFire) {
            sendInput(WeaponInput.FIRE, fire);
            if (fire) {
                applyPredictedRecoil(minecraft);
                triggerAnimation(minecraft.player, "fire");
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
        }
        boolean sprintAnimation = valid && minecraft.player.isSprinting();
        if (sprintAnimation != lastSprintAnimation) {
            triggerAnimation(minecraft.player, sprintAnimation ? "sprint" : "idle");
            lastSprintAnimation = sprintAnimation;
        }
        predictedAnimationTicks = Math.max(0, predictedAnimationTicks - 1);
        recoverRecoil(minecraft);
        updateAdsSensitivity(minecraft, valid && (lastAds || authoritativeState != null && authoritativeState.ads()));
        screenShake *= 0.78F;
    }

    @SubscribeEvent
    public static void modifyFov(ComputeFovModifierEvent event) {
        if (authoritativeState != null && (authoritativeState.ads() || lastAds)) {
            event.setNewFovModifier(event.getNewFovModifier() * authoritativeState.adsFovMultiplier());
        }
    }

    @SubscribeEvent
    public static void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        if (screenShake <= 0.001F) {
            return;
        }
        double time = Minecraft.getInstance().level == null ? 0.0D : Minecraft.getInstance().level.getGameTime();
        event.setPitch(event.getPitch() + (float) Math.sin(time * 2.31D) * screenShake);
        event.setYaw(event.getYaw() + (float) Math.cos(time * 1.73D) * screenShake * 0.7F);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 1.17D) * screenShake * 0.35F);
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isHoldingGun(minecraft) || authoritativeState == null || minecraft.options.hideGui) {
            return;
        }
        GunState state = authoritativeState.state();
        GuiGraphics graphics = event.getGuiGraphics();
        String ammo = state.ammoCount() + "  |  " + state.selectedAmmoId().getPath();
        String mode = state.fireMode().name();
        if (authoritativeState.reloadPhase() != com.hbm.weapon.state.ReloadPhase.IDLE) {
            mode += "  \u2022  " + authoritativeState.reloadPhase().name();
        }
        int right = graphics.guiWidth() - 10;
        graphics.drawString(minecraft.font, Component.literal(ammo),
                right - minecraft.font.width(ammo), graphics.guiHeight() - 38, 0xF2F2F2, true);
        graphics.drawString(minecraft.font, Component.literal(mode),
                right - minecraft.font.width(mode), graphics.guiHeight() - 26, 0xB7CADB, true);
    }

    public static void acceptState(WeaponStatePayload payload) {
        if (payload.acknowledgedSequence() < lastAcknowledged) {
            return;
        }
        boolean changedStack = lastAuthoritativeStack == null
                || !lastAuthoritativeStack.equals(payload.state().stackIdentity());
        boolean changedAds = authoritativeState == null || authoritativeState.ads() != payload.ads();
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
            } else if (changedAds) {
                triggerAnimation(minecraft.player, payload.ads() ? "ads" : "idle");
            }
        }
    }

    public static void acceptEffect(WeaponEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        boolean local = minecraft.player != null && payload.sourceEntityId() == minecraft.player.getId();
        if (payload.effect() == WeaponEffectType.FIRE && local) {
            applyAuthoritativeRecoil(minecraft, payload);
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
            case RELOAD_START -> triggerAnimation(source, "reload_start");
            case RELOAD_INSERT -> triggerAnimation(source, "reload_loop");
            case RELOAD_END -> triggerAnimation(source, "reload_end");
            default -> {
            }
        }
        switch (payload.effect()) {
            case MUZZLE_FLASH -> minecraft.level.addParticle(ParticleTypes.FLASH,
                    payload.x(), payload.y(), payload.z(), 0.0D, 0.0D, 0.0D);
            case SMOKE -> minecraft.level.addParticle(ParticleTypes.SMOKE,
                    payload.x(), payload.y(), payload.z(), 0.0D, 0.025D, 0.0D);
            case CASING -> {
                if (HbmClientConfig.CASING_PARTICLES.get()) {
                    minecraft.level.addParticle(ParticleTypes.CRIT,
                            payload.x(), payload.y(), payload.z(), 0.08D, 0.04D, 0.0D);
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
        if (ads && authoritativeState != null) {
            if (sensitivityBeforeAds == null) {
                sensitivityBeforeAds = minecraft.options.sensitivity().get();
            }
            double multiplier = authoritativeState.adsSensitivityMultiplier()
                    * HbmClientConfig.ADS_SENSITIVITY_SCALE.get();
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

    private static KeyMapping key(String translation, int defaultKey) {
        return new KeyMapping(translation, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    private ClientWeaponController() {
    }
}
