package com.hbm.client.explosion;

import com.hbm.config.HbmClientConfig;
import com.hbm.network.NuclearProgressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Client-only nuclear flash, pressure impact, and low-noise nuclear job status. */
public final class NuclearPresentationOverlay {
    private static float flashIntensity;
    private static int flashAgeTicks = -1;
    private static float shakeStrength;
    private static int shakeAgeTicks = -1;
    private static NuclearProgressPayload.Stage progressStage;
    private static int progressPercent;
    private static int progressTicks;

    /** Starts the immediate overexposure followed by a longer, configurable blindness fade. */
    public static void beginFlash(ClientLevel level, Vec3 origin, float flashRange) {
        if (!HbmClientConfig.CLIENT.enableNuclearFlash.get()) {
            return;
        }
        float exposure = radialExposure(origin, flashRange);
        if (exposure <= 0F) {
            return;
        }
        // This is intentionally a complete local white-out once the player is inside the flash range.
        flashIntensity = Math.max(flashIntensity, 1F);
        flashAgeTicks = 0;
    }

    /** Starts a short camera-safe impulse when the pressure front arrives. */
    public static void beginShock(ClientLevel level, Vec3 origin, float shockRange) {
        if (!HbmClientConfig.CLIENT.enableNuclearScreenShake.get()) {
            return;
        }
        float exposure = shockExposure(origin, shockRange);
        if (exposure <= 0F) {
            return;
        }
        shakeStrength = Math.max(shakeStrength, exposure * 1.5F);
        shakeAgeTicks = 0;
    }

    public static void acceptProgress(NuclearProgressPayload payload) {
        progressStage = payload.stage();
        progressPercent = payload.percent();
        progressTicks = 30;
    }

    public static void tick() {
        if (flashAgeTicks >= 0 && ++flashAgeTicks >= flashDurationTicks()) {
            flashAgeTicks = -1;
            flashIntensity = 0F;
        }
        if (shakeAgeTicks >= 0) {
            if (++shakeAgeTicks >= NuclearImpactTiming.SHAKE_DURATION_TICKS) {
                shakeAgeTicks = -1;
                shakeStrength = 0F;
            }
        }
        if (progressTicks > 0 && --progressTicks == 0) {
            progressStage = null;
        }
    }

    public static void beforeGui(RenderGuiEvent.Pre event) {
        if (flashAgeTicks < 0 || !HbmClientConfig.CLIENT.enableNuclearFlash.get()) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float envelope = NuclearImpactTiming.flashEnvelope(
                flashAgeTicks + partialTick,
                HbmClientConfig.CLIENT.nuclearFlashHoldTicks.get(),
                HbmClientConfig.CLIENT.nuclearBlindnessFadeTicks.get()
        );
        if (envelope <= 0F) {
            return;
        }

        float screenEffects = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float intensity = envelope * flashIntensity * screenEffects;
        if (intensity <= 0F) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int whiteAlpha = Math.clamp(Math.round(255F * intensity), 0, 255);
        graphics.fill(0, 0, width, height, whiteAlpha << 24 | 0x00FFFFFF);
    }

    public static void render(RenderGuiEvent.Post event) {
        GuiGraphics graphics = event.getGuiGraphics();
        if (progressStage == null || progressTicks <= 0 || !HbmClientConfig.CLIENT.showNuclearCalculationStatus.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component message = Component.translatable(progressTranslationKey(progressStage), progressPercent);
        int width = minecraft.font.width(message);
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 74;
        graphics.fill(x - 5, y - 3, x + width + 5, y + 11, 0xB0101010);
        graphics.drawString(minecraft.font, message, x, y, 0xFFF2E8C8, false);
    }

    public static void applyCameraShake(ViewportEvent.ComputeCameraAngles event) {
        if (shakeAgeTicks < 0 || !HbmClientConfig.CLIENT.enableNuclearScreenShake.get()) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float visualAge = shakeAgeTicks + partialTick;
        NuclearImpactTiming.ShakeSample sample = NuclearImpactTiming.shakeSample(visualAge);
        if (NuclearImpactTiming.shakeEnvelope(visualAge) <= 0F) {
            return;
        }

        float screenEffects = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float intensity = Mth.clamp(shakeStrength
                * HbmClientConfig.CLIENT.nuclearScreenShakeScale.get().floatValue()
                * screenEffects, 0F, 2F);
        if (intensity <= 0F) {
            return;
        }

        event.getCamera().move(
                sample.forward() * intensity,
                sample.vertical() * intensity,
                sample.lateral() * intensity
        );
        event.setYaw(event.getYaw() + sample.yaw() * intensity);
        event.setPitch(event.getPitch() + sample.pitch() * intensity);
        event.setRoll(event.getRoll() + sample.roll() * intensity);
    }

    public static void clear() {
        flashIntensity = 0F;
        flashAgeTicks = -1;
        shakeStrength = 0F;
        shakeAgeTicks = -1;
        progressStage = null;
        progressTicks = 0;
    }

    private static String progressTranslationKey(NuclearProgressPayload.Stage stage) {
        return switch (stage) {
            case CALCULATING -> "hud.hbm.nuclear.calculating";
            case EXCAVATING -> "hud.hbm.nuclear.excavating";
            case FALLOUT -> "hud.hbm.nuclear.fallout";
            case CONVERTING -> "hud.hbm.nuclear.converting";
            case VAPORIZING -> "hud.hbm.nuclear.vaporizing";
        };
    }

    private static float radialExposure(Vec3 origin, float range) {
        Player player = Minecraft.getInstance().player;
        if (player == null || range <= 0F) {
            return 0F;
        }
        double distance = player.getEyePosition().distanceTo(origin);
        if (distance >= range) {
            return 0F;
        }
        return Mth.clamp(1F - (float) distance / range, 0F, 1F);
    }

    private static float shockExposure(Vec3 origin, float range) {
        float radial = radialExposure(origin, range);
        return radial <= 0F ? 0F : 0.45F + radial * 0.55F;
    }

    private static int flashDurationTicks() {
        return NuclearImpactTiming.flashDurationTicks(
                HbmClientConfig.CLIENT.nuclearFlashHoldTicks.get(),
                HbmClientConfig.CLIENT.nuclearBlindnessFadeTicks.get()
        );
    }

    private NuclearPresentationOverlay() {
    }
}
