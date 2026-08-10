package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only presentation controls. They never alter server explosion math. */
public final class HbmClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final Client CLIENT = new Client(BUILDER);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private HbmClientConfig() {
    }

    public enum NuclearVisualQuality {
        FULL,
        REDUCED,
        MINIMAL
    }

    public static final class Client {
        public final ModConfigSpec.EnumValue<NuclearVisualQuality> nuclearVisualQuality;
        public final ModConfigSpec.BooleanValue enableNuclearFlash;
        public final ModConfigSpec.IntValue nuclearFlashHoldTicks;
        public final ModConfigSpec.IntValue nuclearBlindnessFadeTicks;
        public final ModConfigSpec.BooleanValue enableNuclearScreenShake;
        public final ModConfigSpec.DoubleValue nuclearScreenShakeScale;
        public final ModConfigSpec.BooleanValue enableNuclearFalloutRain;
        public final ModConfigSpec.DoubleValue nuclearFalloutRainDensity;
        public final ModConfigSpec.DoubleValue nuclearFalloutRainSoundVolume;
        public final ModConfigSpec.BooleanValue showNuclearCalculationStatus;

        private Client(ModConfigSpec.Builder builder) {
            builder.comment("Client-only nuclear flash, shock front, and mushroom cloud presentation.").push("nuclearVisuals");
            this.nuclearVisualQuality = builder
                    .comment("FULL keeps the source event timeline with bounded cloudlets. REDUCED and MINIMAL lower particle density only.")
                    .defineEnum("quality", NuclearVisualQuality.FULL);
            this.enableNuclearFlash = builder
                    .comment("Shows the source-style immediate white-yellow nuclear flash when it is visible to the client.")
                    .define("enableFlash", true);
            this.nuclearFlashHoldTicks = builder
                    .comment("Bright nuclear impact interval in ticks. Only its opening is hard white; the remainder decays into the afterimage.")
                    .defineInRange("flashHoldTicks", 40, 1, 200);
            this.nuclearBlindnessFadeTicks = builder
                    .comment("Length of the nuclear afterimage/blindness fade in ticks after the full-white impact.")
                    .defineInRange("blindnessFadeTicks", 60, 1, 400);
            this.enableNuclearScreenShake = builder
                    .comment("Applies a short source-inspired camera shake when the shock front reaches the client.")
                    .define("enableScreenShake", true);
            this.nuclearScreenShakeScale = builder
                    .comment("Client-only multiplier for nuclear screen shake.")
                    .defineInRange("screenShakeScale", 1D, 0D, 4D);
            this.enableNuclearFalloutRain = builder
                    .comment("Shows local falling fallout particles while a nearby Fallout Rain job is active.")
                    .define("enableFalloutRain", true);
            this.nuclearFalloutRainDensity = builder
                    .comment("Client-only multiplier for visible Fallout Rain density. The rain area always follows the server fallout radius.")
                    .defineInRange("falloutRainDensity", 1D, 0D, 3D);
            this.nuclearFalloutRainSoundVolume = builder
                    .comment("Client-only weather-loop volume while standing inside active Fallout Rain.")
                    .defineInRange("falloutRainSoundVolume", 0.45D, 0D, 2D);
            this.showNuclearCalculationStatus = builder
                    .comment("Shows a restrained progress line while a large nuclear terrain or fallout job is active nearby.")
                    .define("showCalculationStatus", true);
            builder.pop();
        }
    }
}
