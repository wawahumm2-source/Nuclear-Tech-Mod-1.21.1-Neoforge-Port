package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HbmConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final General GENERAL = new General(BUILDER);
    public static final Radiation RADIATION = new Radiation(BUILDER);
    public static final Machines MACHINES = new Machines(BUILDER);
    public static final Bombs BOMBS = new Bombs(BUILDER);
    public static final Weapons WEAPONS = new Weapons(BUILDER);
    public static final Worldgen WORLDGEN = new Worldgen(BUILDER);
    public static final Structures STRUCTURES = new Structures(BUILDER);
    public static final Fallout FALLOUT = new Fallout(BUILDER);

    public static final ModConfigSpec.IntValue RADIATION_TICK_INTERVAL = RADIATION.tickInterval;
    public static final ModConfigSpec.IntValue AMBIENT_RADIATION_RADIUS = RADIATION.ambientBlockRadius;
    public static final ModConfigSpec.DoubleValue RADIATION_DECAY_PER_TICK = RADIATION.decayPerTick;
    public static final ModConfigSpec.DoubleValue RADIATION_DAMAGE_THRESHOLD = RADIATION.damageThreshold;
    public static final ModConfigSpec.DoubleValue RADIATION_MAX_EXPOSURE = RADIATION.maxExposure;
    public static final ModConfigSpec.DoubleValue SHIELDING_ITEM_FACTOR = RADIATION.shieldingItemFactor;
    public static final ModConfigSpec.DoubleValue RADAWAY_REDUCTION = RADIATION.radawayReduction;
    public static final ModConfigSpec.IntValue BURNER_PRESS_PROCESS_TICKS = MACHINES.burnerPressProcessTicks;
    public static final ModConfigSpec.IntValue PROTOTYPE_NUKE_RADIUS = BOMBS.prototypeNukeRadius;
    public static final ModConfigSpec.DoubleValue PROTOTYPE_NUKE_FALLOUT = FALLOUT.prototypeNukeFallout;

    public static final ModConfigSpec SPEC = BUILDER.build();

    private HbmConfig() {
    }

    public static final class General {
        private General(ModConfigSpec.Builder builder) {
            builder.comment("General HBM systems. Source defaults are pending full original-config audit.").push("general");
            builder.pop();
        }
    }

    public static final class Radiation {
        public final ModConfigSpec.IntValue tickInterval;
        public final ModConfigSpec.IntValue ambientBlockRadius;
        public final ModConfigSpec.DoubleValue decayPerTick;
        public final ModConfigSpec.DoubleValue damageThreshold;
        public final ModConfigSpec.DoubleValue maxExposure;
        public final ModConfigSpec.DoubleValue shieldingItemFactor;
        public final ModConfigSpec.DoubleValue radawayReduction;

        private Radiation(ModConfigSpec.Builder builder) {
            builder.comment("Radiation, shielding, fallout exposure, and cleanup behavior.").push("radiation");
            this.tickInterval = builder
                    .comment("How often player radiation exposure is evaluated, in game ticks.")
                    .defineInRange("tickInterval", 20, 1, 200);
            this.ambientBlockRadius = builder
                    .comment("Block radius scanned around players for radioactive blocks.")
                    .defineInRange("ambientBlockRadius", 4, 1, 12);
            this.decayPerTick = builder
                    .comment("Passive radiation exposure removed per radiation tick.")
                    .defineInRange("decayPerTick", 0.025D, 0D, 100D);
            this.damageThreshold = builder
                    .comment("Exposure level where accumulated radiation begins hurting players.")
                    .defineInRange("damageThreshold", 20D, 0D, 10000D);
            this.maxExposure = builder
                    .comment("Maximum stored player radiation exposure.")
                    .defineInRange("maxExposure", 250D, 1D, 100000D);
            this.shieldingItemFactor = builder
                    .comment("Exposure reduction applied for each equipped item in hbm:radiation_shielding.")
                    .defineInRange("shieldingItemFactor", 0.20D, 0D, 0.95D);
            this.radawayReduction = builder
                    .comment("Radiation exposure removed by one RadAway item.")
                    .defineInRange("radawayReduction", 12D, 0D, 10000D);
            builder.pop();
        }
    }

    public static final class Machines {
        public final ModConfigSpec.IntValue burnerPressProcessTicks;

        private Machines(ModConfigSpec.Builder builder) {
            builder.comment("Shared machine behavior and temporary alpha machine defaults.").push("machines");
            this.burnerPressProcessTicks = builder
                    .comment("Ticks required for the Burner Press to process one item when a recipe omits a source-specific time.")
                    .defineInRange("burnerPressProcessTicks", 200, 20, 1200);
            builder.pop();
        }
    }

    public static final class Bombs {
        public final ModConfigSpec.IntValue prototypeNukeRadius;

        private Bombs(ModConfigSpec.Builder builder) {
            builder.comment("Bomb and explosion behavior. Prototype values remain scaffolds until source math is ported.").push("bombs");
            this.prototypeNukeRadius = builder
                    .comment("Explosion radius for the alpha prototype nuclear charge.")
                    .defineInRange("prototypeNukeRadius", 12, 2, 64);
            builder.pop();
        }
    }

    public static final class Weapons {
        private Weapons(ModConfigSpec.Builder builder) {
            builder.comment("Weapon behavior. No active weapon config has been ported yet.").push("weapons");
            builder.pop();
        }
    }

    public static final class Worldgen {
        private Worldgen(ModConfigSpec.Builder builder) {
            builder.comment("Ore, structure, and feature generation. Active worldgen config is pending source-default audit.").push("worldgen");
            builder.pop();
        }
    }

    public static final class Structures {
        private Structures(ModConfigSpec.Builder builder) {
            builder.comment("Generated structures. No active structure config has been ported yet.").push("structures");
            builder.pop();
        }
    }

    public static final class Fallout {
        public final ModConfigSpec.DoubleValue prototypeNukeFallout;

        private Fallout(ModConfigSpec.Builder builder) {
            builder.comment("Fallout storage and spread behavior. Prototype values remain scaffolds until source fallout is ported.").push("fallout");
            this.prototypeNukeFallout = builder
                    .comment("Chunk fallout added around a prototype nuclear charge detonation.")
                    .defineInRange("prototypeNukeFallout", 8D, 0D, 10000D);
            builder.pop();
        }
    }
}
