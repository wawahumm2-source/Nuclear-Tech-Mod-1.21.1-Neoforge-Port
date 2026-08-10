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
    public static final ModConfigSpec.DoubleValue RADIATION_DAMAGE_THRESHOLD = RADIATION.effectThreshold;
    public static final ModConfigSpec.DoubleValue RADIATION_MAX_EXPOSURE = RADIATION.maxExposure;
    public static final ModConfigSpec.DoubleValue SHIELDING_ITEM_FACTOR = RADIATION.tagShieldingResistance;
    public static final ModConfigSpec.DoubleValue RADAWAY_REDUCTION = RADIATION.radawayReduction;
    public static final ModConfigSpec.IntValue BURNER_PRESS_PROCESS_TICKS = MACHINES.burnerPressProcessTicks;
    public static final ModConfigSpec.IntValue PROTOTYPE_NUKE_RADIUS = BOMBS.prototypeNukeTerrainRadius;
    public static final ModConfigSpec.BooleanValue GRENADE_BLOCK_DAMAGE = WEAPONS.grenadeBlockDamage;

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
        public final ModConfigSpec.IntValue mobTickInterval;
        public final ModConfigSpec.IntValue ambientBlockRadius;
        public final ModConfigSpec.DoubleValue ambientBlockFalloffExponent;
        public final ModConfigSpec.DoubleValue decayPerTick;
        public final ModConfigSpec.BooleanValue enableContamination;
        public final ModConfigSpec.BooleanValue enableChunkRadiation;
        public final ModConfigSpec.BooleanValue enableMobRadiation;
        public final ModConfigSpec.BooleanValue enablePlayerEffects;
        public final ModConfigSpec.BooleanValue enableVisualEffects;
        public final ModConfigSpec.DoubleValue netherAmbientRate;
        public final ModConfigSpec.DoubleValue contaminatedWaterAmbientRate;
        public final ModConfigSpec.DoubleValue contaminatedWaterImmersionRate;
        public final ModConfigSpec.DoubleValue contaminatedWaterBucketRate;
        public final ModConfigSpec.DoubleValue inventorySourceMultiplier;
        public final ModConfigSpec.DoubleValue blockSourceMultiplier;
        public final ModConfigSpec.DoubleValue falloutSourceMultiplier;
        public final ModConfigSpec.DoubleValue explosionSourceMultiplier;
        public final ModConfigSpec.DoubleValue effectThreshold;
        public final ModConfigSpec.DoubleValue moderateEffectThreshold;
        public final ModConfigSpec.DoubleValue severeEffectThreshold;
        public final ModConfigSpec.DoubleValue criticalEffectThreshold;
        public final ModConfigSpec.DoubleValue fatalEffectThreshold;
        public final ModConfigSpec.DoubleValue maxExposure;
        public final ModConfigSpec.DoubleValue tagShieldingResistance;
        public final ModConfigSpec.DoubleValue radawayReduction;
        public final ModConfigSpec.IntValue radawayTicks;
        public final ModConfigSpec.DoubleValue radawayPerTick;
        public final ModConfigSpec.IntValue radawayStrongTicks;
        public final ModConfigSpec.DoubleValue radawayStrongPerTick;
        public final ModConfigSpec.IntValue radawayFlushTicks;
        public final ModConfigSpec.DoubleValue radawayFlushPerTick;
        public final ModConfigSpec.IntValue radXDuration;
        public final ModConfigSpec.DoubleValue radXResistance;
        public final ModConfigSpec.IntValue clientSyncInterval;
        public final ModConfigSpec.IntValue chunkUpdateInterval;
        public final ModConfigSpec.DoubleValue chunkSpreadCenter;
        public final ModConfigSpec.DoubleValue chunkSpreadCardinal;
        public final ModConfigSpec.DoubleValue chunkSpreadDiagonal;
        public final ModConfigSpec.DoubleValue chunkSpreadDecay;
        public final ModConfigSpec.DoubleValue chunkSpreadFloor;
        public final ModConfigSpec.BooleanValue enableCraterRadiation;
        public final ModConfigSpec.DoubleValue craterInnerRadiationRate;
        public final ModConfigSpec.DoubleValue craterRadiationRate;
        public final ModConfigSpec.DoubleValue craterOuterRadiationRate;
        public final ModConfigSpec.BooleanValue cleanupDeadDirt;
        public final ModConfigSpec.DoubleValue developerFalloutAmount;
        public final ModConfigSpec.DoubleValue developerExplosionDose;

        private Radiation(ModConfigSpec.Builder builder) {
            builder.comment("Radiation, shielding, fallout exposure, and cleanup behavior. Tier 1 defaults are used where verified.").push("radiation");
            this.tickInterval = builder
                    .comment("How often radiation is evaluated, in game ticks. One tick preserves source RAD/s behavior.")
                    .defineInRange("tickInterval", 1, 1, 200);
            this.mobTickInterval = builder
                    .comment("How often loaded mobs evaluate radiation. Total RAD/s is preserved across the batched interval.")
                    .defineInRange("mobTickInterval", 5, 1, 200);
            this.ambientBlockRadius = builder
                    .comment("Block radius scanned around players for radioactive blocks.")
                    .defineInRange("ambientBlockRadius", 4, 1, 12);
            this.ambientBlockFalloffExponent = builder
                    .comment("Local block-aura falloff exponent. One is linear; two keeps nearby emitters strong and fades to zero at the radius.")
                    .defineInRange("ambientBlockFalloffExponent", 2D, 0.1D, 10D);
            this.decayPerTick = builder
                    .comment("Passive radiation removed every evaluation. Tier 1 radiation does not decay naturally.")
                    .defineInRange("decayPerTick", 0D, 0D, 100D);
            this.enableContamination = builder
                    .comment("Whether player radiation accumulation and treatment are active.")
                    .define("enableContamination", true);
            this.enableChunkRadiation = builder
                    .comment("Whether persistent environmental radiation fields are active.")
                    .define("enableChunkRadiation", true);
            this.enableMobRadiation = builder
                    .comment("Whether loaded mobs accumulate radiation from environmental, equipment, and explosion sources.")
                    .define("enableMobRadiation", true);
            this.enablePlayerEffects = builder
                    .comment("Whether source-shaped radiation sickness and fatal thresholds are active for living entities.")
                    .define("enablePlayerEffects", true);
            this.enableVisualEffects = builder
                    .comment("Whether client radiation feedback is allowed. Developer diagnostics are unaffected.")
                    .define("enableVisualEffects", true);
            this.netherAmbientRate = builder
                    .comment("Minimum ambient radiation in the Nether, in RAD/s.")
                    .defineInRange("netherAmbientRate", 0.1D, 0D, 10000D);
            this.contaminatedWaterAmbientRate = builder
                    .comment("RAD/s emitted by each nearby contaminated-water block. This modern flood wrapper has no live Tier 1 equivalent.")
                    .defineInRange("contaminatedWaterAmbientRate", 0.05D, 0D, 10000D);
            this.contaminatedWaterImmersionRate = builder
                    .comment("Additional RAD/s received while a living entity is immersed in contaminated water.")
                    .defineInRange("contaminatedWaterImmersionRate", 5D, 0D, 10000D);
            this.contaminatedWaterBucketRate = builder
                    .comment("RAD/s emitted by a carried contaminated-water bucket.")
                    .defineInRange("contaminatedWaterBucketRate", 0.5D, 0D, 10000D);
            this.inventorySourceMultiplier = builder
                    .comment("Global multiplier for carried radioactive-item intake.")
                    .defineInRange("inventorySourceMultiplier", 1D, 0D, 10000D);
            this.blockSourceMultiplier = builder
                    .comment("Global multiplier for radioactive-block intake.")
                    .defineInRange("blockSourceMultiplier", 1D, 0D, 10000D);
            this.falloutSourceMultiplier = builder
                    .comment("Global multiplier for persistent fallout-field intake.")
                    .defineInRange("falloutSourceMultiplier", 1D, 0D, 10000D);
            this.explosionSourceMultiplier = builder
                    .comment("Global multiplier for direct explosion radiation.")
                    .defineInRange("explosionSourceMultiplier", 1D, 0D, 10000D);
            this.effectThreshold = builder
                    .comment("Accumulated radiation where source-style sickness begins.")
                    .defineInRange("effectThreshold", 200D, 0D, 10000D);
            this.moderateEffectThreshold = builder
                    .comment("Accumulated radiation where source-style moderate sickness begins.")
                    .defineInRange("moderateEffectThreshold", 400D, 0D, 10000D);
            this.severeEffectThreshold = builder
                    .comment("Accumulated radiation where severe sickness begins.")
                    .defineInRange("severeEffectThreshold", 600D, 0D, 10000D);
            this.criticalEffectThreshold = builder
                    .comment("Accumulated radiation where critical sickness begins.")
                    .defineInRange("criticalEffectThreshold", 800D, 0D, 10000D);
            this.fatalEffectThreshold = builder
                    .comment("Accumulated radiation where source-style fatal exposure occurs.")
                    .defineInRange("fatalEffectThreshold", 1000D, 1D, 10000D);
            this.maxExposure = builder
                    .comment("Maximum stored player radiation. Tier 1 caps this at 2500 RAD.")
                    .defineInRange("maxExposure", 2500D, 1D, 100000D);
            this.tagShieldingResistance = builder
                    .comment("Resistance granted by each hbm:radiation_shielding armor item. Resistance uses 10^-resistance.")
                    .defineInRange("tagShieldingResistance", 0.20D, 0D, 20D);
            this.radawayReduction = builder
                    .comment("Total RAD removed by a normal RadAway treatment.")
                    .defineInRange("radawayReduction", 140D, 0D, 10000D);
            this.radawayTicks = builder
                    .comment("Normal RadAway treatment duration in ticks.")
                    .defineInRange("radawayTicks", 14, 1, 1200);
            this.radawayPerTick = builder
                    .comment("RAD removed by normal RadAway every treatment tick.")
                    .defineInRange("radawayPerTick", 10D, 0D, 10000D);
            this.radawayStrongTicks = builder
                    .comment("Strong RadAway treatment duration in ticks.")
                    .defineInRange("radawayStrongTicks", 35, 1, 1200);
            this.radawayStrongPerTick = builder
                    .comment("RAD removed by strong RadAway every treatment tick.")
                    .defineInRange("radawayStrongPerTick", 10D, 0D, 10000D);
            this.radawayFlushTicks = builder
                    .comment("RadAway Flush treatment duration in ticks.")
                    .defineInRange("radawayFlushTicks", 50, 1, 1200);
            this.radawayFlushPerTick = builder
                    .comment("RAD removed by RadAway Flush every treatment tick.")
                    .defineInRange("radawayFlushPerTick", 20D, 0D, 10000D);
            this.radXDuration = builder
                    .comment("Rad-X protection duration in ticks.")
                    .defineInRange("radXDuration", 3600, 1, 72000);
            this.radXResistance = builder
                    .comment("Additional radiation resistance while Rad-X is active.")
                    .defineInRange("radXResistance", 0.20D, 0D, 20D);
            this.clientSyncInterval = builder
                    .comment("Ticks between radiation diagnostic payloads sent to a player.")
                    .defineInRange("clientSyncInterval", 10, 1, 200);
            this.chunkUpdateInterval = builder
                    .comment("Ticks between persistent chunk radiation field updates.")
                    .defineInRange("chunkUpdateInterval", 20, 1, 1200);
            this.chunkSpreadCenter = builder
                    .comment("Source simple-field share retained by the originating chunk.")
                    .defineInRange("chunkSpreadCenter", 0.60D, 0D, 1D);
            this.chunkSpreadCardinal = builder
                    .comment("Source simple-field share distributed to each cardinal neighboring chunk.")
                    .defineInRange("chunkSpreadCardinal", 0.075D, 0D, 1D);
            this.chunkSpreadDiagonal = builder
                    .comment("Source simple-field share distributed to each diagonal neighboring chunk.")
                    .defineInRange("chunkSpreadDiagonal", 0.025D, 0D, 1D);
            this.chunkSpreadDecay = builder
                    .comment("Decay multiplier applied to established chunk radiation during spread.")
                    .defineInRange("chunkSpreadDecay", 0.99D, 0D, 1D);
            this.chunkSpreadFloor = builder
                    .comment("Flat decay removed from established chunk radiation during spread.")
                    .defineInRange("chunkSpreadFloor", 0.05D, 0D, 10000D);
            this.enableCraterRadiation = builder
                    .comment("Whether completed Fallout Rain marks persistent radioactive crater zones.")
                    .define("enableCraterRadiation", true);
            this.craterInnerRadiationRate = builder
                    .comment("RAD/s for the inner crater zone. Rebirth preserves the source-family default of 25.")
                    .defineInRange("craterInnerRadiationRate", 25D, 0D, 100000D);
            this.craterRadiationRate = builder
                    .comment("RAD/s for the main crater zone. Rebirth preserves the source-family default of 5.")
                    .defineInRange("craterRadiationRate", 5D, 0D, 100000D);
            this.craterOuterRadiationRate = builder
                    .comment("RAD/s for the outer crater zone. Rebirth preserves the source-family default of 0.5.")
                    .defineInRange("craterOuterRadiationRate", 0.5D, 0D, 100000D);
            this.cleanupDeadDirt = builder
                    .comment("Whether Waste Earth and Waste Mycelium can decay back into dirt. Tier 1 defaults false.")
                    .define("cleanupDeadDirt", false);
            this.developerFalloutAmount = builder
                    .comment("Chunk radiation injected by the developer fallout tool.")
                    .defineInRange("developerFalloutAmount", 40D, 0D, 100000D);
            this.developerExplosionDose = builder
                    .comment("Direct radiation applied by the developer explosion pulse tool.")
                    .defineInRange("developerExplosionDose", 100D, 0D, 100000D);
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
        public final ModConfigSpec.IntValue hbmTntStrength;
        public final ModConfigSpec.IntValue hbmTntFuseTicks;
        public final ModConfigSpec.IntValue hbmTntChainFuseTicks;
        public final ModConfigSpec.IntValue prototypeNukeTerrainRadius;
        public final ModConfigSpec.IntValue prototypeNukeKillRadius;
        public final ModConfigSpec.DoubleValue prototypeNukeMaxDamage;
        public final ModConfigSpec.IntValue prototypeNukeRayResolution;
        public final ModConfigSpec.IntValue prototypeNukeRadiationLevel;
        public final ModConfigSpec.IntValue nukeBoyRadius;
        public final ModConfigSpec.BooleanValue nukeBoyUseHybridPlanner;
        public final ModConfigSpec.IntValue nukeBoyRayCount;
        public final ModConfigSpec.DoubleValue nukeBoyCraterDepthMultiplier;
        public final ModConfigSpec.IntValue nukeBoyRadiationBurstTicks;
        public final ModConfigSpec.DoubleValue nukeBoyRadiationBurstBaseDose;
        public final ModConfigSpec.BooleanValue nukeBoyVaporizeWater;
        public final ModConfigSpec.DoubleValue nukeBoyWaterVaporRadiusMultiplier;
        public final ModConfigSpec.IntValue nukeBoyWaterVaporTransitionBlocks;
        public final ModConfigSpec.IntValue nukeBoyMaxContainedWaterBlocks;
        public final ModConfigSpec.IntValue nuclearImmediateTerrainStrength;
        public final ModConfigSpec.IntValue nuclearRayWorkPerTick;
        public final ModConfigSpec.IntValue nuclearBlockWorkPerTick;
        public final ModConfigSpec.IntValue nuclearPlanningTimeBudgetMs;
        public final ModConfigSpec.IntValue nuclearSnapshotSectionsPerTick;
        public final ModConfigSpec.IntValue nuclearSnapshotTimeBudgetMs;
        public final ModConfigSpec.IntValue nuclearHybridCubeResolution;
        public final ModConfigSpec.IntValue nuclearHybridWorkerThreads;
        public final ModConfigSpec.IntValue nuclearHybridQueueCapacity;
        public final ModConfigSpec.IntValue nuclearSourceRayBatchSize;
        public final ModConfigSpec.IntValue nuclearDestructionTimeBudgetMs;
        public final ModConfigSpec.IntValue nuclearSchedulerTimeBudgetMs;
        public final ModConfigSpec.IntValue nuclearWaterVaporBlockWorkPerTick;
        public final ModConfigSpec.IntValue nuclearWaterVaporTimeBudgetMs;
        public final ModConfigSpec.IntValue nuclearWaterForceLoadChunksPerTick;
        public final ModConfigSpec.BooleanValue forceLoadNuclearWork;
        public final ModConfigSpec.IntValue nuclearForceLoadChunksPerTick;
        public final ModConfigSpec.IntValue nuclearActiveChunkWindow;

        private Bombs(ModConfigSpec.Builder builder) {
            builder.comment("Bomb and explosion behavior. Defaults below are verified from the Tier 1 TNT and PARAMS_LOW paths.").push("bombs");
            this.hbmTntStrength = builder
                    .comment("Tier 1 HBM TNT explosion strength.")
                    .defineInRange("hbmTntStrength", 10, 1, 64);
            this.hbmTntFuseTicks = builder
                    .comment("Tier 1 HBM TNT fuse duration in ticks.")
                    .defineInRange("hbmTntFuseTicks", 80, 1, 1200);
            this.hbmTntChainFuseTicks = builder
                    .comment("Tier 1 HBM TNT chain-detonation base fuse. Actual fuse is base/2 through base/2+base-1.")
                    .defineInRange("hbmTntChainFuseTicks", 20, 2, 240);
            this.prototypeNukeTerrainRadius = builder
                    .comment("Tier 1 PARAMS_LOW terrain strength for the Prototype Nuclear Charge pilot.")
                    .defineInRange("prototypeNukeTerrainRadius", 15, 2, 64);
            this.prototypeNukeKillRadius = builder
                    .comment("Tier 1 PARAMS_LOW nuclear blast kill radius.")
                    .defineInRange("prototypeNukeKillRadius", 45, 1, 256);
            this.prototypeNukeMaxDamage = builder
                    .comment("Tier 1 ExplosionNukeGeneric maximum nuclear blast damage.")
                    .defineInRange("prototypeNukeMaxDamage", 250D, 0D, 100000D);
            this.prototypeNukeRayResolution = builder
                    .comment("Tier 1 PARAMS_LOW ExplosionNT boundary-ray resolution.")
                    .defineInRange("prototypeNukeRayResolution", 64, 4, 128);
            this.prototypeNukeRadiationLevel = builder
                    .comment("Tier 1 PARAMS_LOW radiation level used by the 25-chunk fallout diamond.")
                    .defineInRange("prototypeNukeRadiationLevel", 2, 0, 32);
            this.nukeBoyRadius = builder
                    .comment("Tier 1 Little Boy terrain radius. The official NukeBoy uses 120 by default; its MK5 carrier doubles this to 240 ray strength while preserving the 120-block reach.")
                    .defineInRange("nukeBoyRadius", 120, 8, 256);
            this.nukeBoyUseHybridPlanner = builder
                    .comment("Uses the lower-density radial planner intended for weak hardware. The default source-density generalized spiral preserves blast-resistance behavior.")
                    .define("nukeBoyUseHybridPlanner", false);
            this.nukeBoyRayCount = builder
                    .comment("Little Boy MK5 generalized-spiral point count. Tier 1 derives 452389 points at its default strength of 240.")
                    .defineInRange("nukeBoyRayCount", 452389, 8000, 500000);
            this.nukeBoyCraterDepthMultiplier = builder
                    .comment("Downward penetration multiplier used only by the optional hybrid planner. Source-density MK5 always uses the Tier 1 value of 1.")
                    .defineInRange("nukeBoyCraterDepthMultiplier", 2.5D, 1D, 8D);
            this.nukeBoyRadiationBurstTicks = builder
                    .comment("Source MK5 direct-radiation burst duration. Tier 1 applies it for the first ten ticks.")
                    .defineInRange("nukeBoyRadiationBurstTicks", 10, 0, 200);
            this.nukeBoyRadiationBurstBaseDose = builder
                    .comment("Source MK5 direct-radiation numerator before tick, shielding, and inverse-square attenuation.")
                    .defineInRange("nukeBoyRadiationBurstBaseDose", 2_500_000D, 0D, 100_000_000D);
            this.nukeBoyVaporizeWater = builder
                    .comment("Whether Little Boy runs the approved full-height cylindrical liquid pass across its Fallout Rain radius. This remains an MK5 extension.")
                    .define("nukeBoyVaporizeWater", true);
            this.nukeBoyWaterVaporRadiusMultiplier = builder
                    .comment("Multiplier applied to Little Boy's Fallout Rain radius for the persisted water-vaporization pass.")
                    .defineInRange("nukeBoyWaterVaporRadiusMultiplier", 1D, 0D, 4D);
            this.nukeBoyWaterVaporTransitionBlocks = builder
                    .comment("Optional shoreline transition beyond the Little Boy vaporization radius. Tier 1 parity uses a hard boundary of zero.")
                    .defineInRange("nukeBoyWaterVaporTransitionBlocks", 0, 0, 96);
            this.nukeBoyMaxContainedWaterBlocks = builder
                    .comment("Largest fully enclosed connected water body that Little Boy may evaporate. Larger or boundary-connected bodies persist and refill the crater as contaminated water.")
                    .defineInRange("nukeBoyMaxContainedWaterBlocks", 131072, 1, 2000000);
            this.nuclearImmediateTerrainStrength = builder
                    .comment("Terrain strengths at or below this value finish immediately. Larger nuclear profiles use bounded terrain jobs.")
                    .defineInRange("nuclearImmediateTerrainStrength", 15, 0, 64);
            this.nuclearRayWorkPerTick = builder
                    .comment("Maximum source-style nuclear ray steps planned on the main server thread per tick. A separate wall-time cap still protects tick rate.")
                    .defineInRange("nuclearRayWorkPerTick", 48000, 256, 250000);
            this.nuclearBlockWorkPerTick = builder
                    .comment("Maximum planned nuclear blocks removed on the main server thread per tick. The fast default accepts a temporary TPS drop; a wall-time cap remains authoritative.")
                    .defineInRange("nuclearBlockWorkPerTick", 8192, 64, 250000);
            this.nuclearPlanningTimeBudgetMs = builder
                    .comment("Maximum wall time spent planning batched nuclear rays during one server tick.")
                    .defineInRange("nuclearPlanningTimeBudgetMs", 8, 1, 40);
            this.nuclearSnapshotSectionsPerTick = builder
                    .comment("Maximum loaded chunk sections copied into immutable nuclear-planner snapshots per server tick.")
                    .defineInRange("nuclearSnapshotSectionsPerTick", 128, 1, 512);
            this.nuclearSnapshotTimeBudgetMs = builder
                    .comment("Maximum main-thread wall time spent capturing immutable nuclear terrain snapshots per server tick.")
                    .defineInRange("nuclearSnapshotTimeBudgetMs", 20, 1, 40);
            this.nuclearHybridCubeResolution = builder
                    .comment("Direction samples per cube-map face for the deterministic hybrid MK5 radial field.")
                    .defineInRange("nuclearHybridCubeResolution", 64, 16, 128);
            this.nuclearHybridWorkerThreads = builder
                    .comment("CPU worker count for immutable nuclear mathematics. Two is the four-core target default.")
                    .defineInRange("nuclearHybridWorkerThreads", 2, 1, 4);
            this.nuclearHybridQueueCapacity = builder
                    .comment("Maximum queued immutable nuclear calculations in addition to active workers.")
                    .defineInRange("nuclearHybridQueueCapacity", 6, 1, 32);
            this.nuclearSourceRayBatchSize = builder
                    .comment("Exact source-density MK5 rays calculated per immutable worker batch. Smaller batches improve fairness between simultaneous explosions.")
                    .defineInRange("nuclearSourceRayBatchSize", 8192, 128, 16384);
            this.nuclearDestructionTimeBudgetMs = builder
                    .comment("Maximum wall time spent excavating planned nuclear terrain during one server tick.")
                    .defineInRange("nuclearDestructionTimeBudgetMs", 18, 1, 40);
            this.nuclearSchedulerTimeBudgetMs = builder
                    .comment("Maximum total main-thread time spent by all nuclear queues in one server tick. Active planning, mutation, water, and fallout lanes receive rotating fair shares within this cap.")
                    .defineInRange("nuclearSchedulerTimeBudgetMs", 35, 1, 45);
            this.nuclearWaterVaporBlockWorkPerTick = builder
                    .comment("Maximum liquid candidates inspected per level per server tick after empty chunk sections are skipped.")
                    .defineInRange("nuclearWaterVaporBlockWorkPerTick", 250000, 128, 250000);
            this.nuclearWaterVaporTimeBudgetMs = builder
                    .comment("Maximum wall time spent vaporizing water during one server tick.")
                    .defineInRange("nuclearWaterVaporTimeBudgetMs", 30, 1, 40);
            this.nuclearWaterForceLoadChunksPerTick = builder
                    .comment("Maximum water-vaporization share of new nuclear chunk tickets while other work lanes are active. Water may use the full global cap when uncontended.")
                    .defineInRange("nuclearWaterForceLoadChunksPerTick", 16, 1, 64);
            this.forceLoadNuclearWork = builder
                    .comment("Whether large nuclear work force-loads unrendered affected chunks. Disabling this restores loaded-chunks-only processing.")
                    .define("forceLoadNuclearWork", true);
            this.nuclearForceLoadChunksPerTick = builder
                    .comment("Maximum new non-ticking chunk tickets issued for nuclear work per level per server tick.")
                    .defineInRange("nuclearForceLoadChunksPerTick", 16, 1, 256);
            this.nuclearActiveChunkWindow = builder
                    .comment("Maximum prefetched force-loaded chunks held by each post-plan nuclear work queue.")
                    .defineInRange("nuclearActiveChunkWindow", 24, 1, 256);
            builder.pop();
        }
    }

    public static final class Weapons {
        public final ModConfigSpec.BooleanValue grenadeBlockDamage;

        private Weapons(ModConfigSpec.Builder builder) {
            builder.comment("Server-authoritative HBM weapon behavior.").push("weapons");
            this.grenadeBlockDamage = builder
                    .comment("Whether Congo Lake HE/HEAT explosions may damage blocks when the ammo profile permits it.")
                    .define("grenadeBlockDamage", true);
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
        public final ModConfigSpec.DoubleValue nuclearFalloutRadiusMultiplier;
        public final ModConfigSpec.IntValue nuclearFalloutDepth;
        public final ModConfigSpec.IntValue nuclearWoodEffectPercent;
        public final ModConfigSpec.IntValue nuclearFireChanceDenominator;
        public final ModConfigSpec.IntValue nuclearFireLifetimeTicks;
        public final ModConfigSpec.IntValue nuclearFalloutColumnsPerTick;
        public final ModConfigSpec.IntValue nuclearBlastTerrainPlanColumnsPerTick;
        public final ModConfigSpec.IntValue nuclearBlastTerrainColumnsPerTick;
        public final ModConfigSpec.IntValue nuclearBlastTerrainTimeBudgetMs;
        public final ModConfigSpec.BooleanValue enableNuclearTerrainTransformation;
        public final ModConfigSpec.BooleanValue enableNuclearFalloutDeposits;
        public final ModConfigSpec.BooleanValue enableNuclearWildfire;

        private Fallout(ModConfigSpec.Builder builder) {
            builder.comment("Source-shaped nuclear Fallout Rain settings. Values are captured when a bomb detonates.").push("fallout");
            this.nuclearFalloutRadiusMultiplier = builder
                    .comment("EntityNukeExplosionMK5 Fallout Rain radius multiplier. Tier 1 Little Boy uses 2.5.")
                    .defineInRange("nuclearFalloutRadiusMultiplier", 2.5D, 0.1D, 16D);
            this.nuclearFalloutDepth = builder
                    .comment("How many solid blocks Fallout Rain processes in each column. Tier 1 uses 3.")
                    .defineInRange("nuclearFalloutDepth", 3, 1, 16);
            this.nuclearWoodEffectPercent = builder
                    .comment("Inner Fallout Rain percentage that petrifies wood and permits source-style fire.")
                    .defineInRange("nuclearWoodEffectPercent", 65, 1, 100);
            this.nuclearFireChanceDenominator = builder
                    .comment("One-in-N source-style fire placement roll on qualifying flammable surfaces.")
                    .defineInRange("nuclearFireChanceDenominator", 5, 1, 100);
            this.nuclearFireLifetimeTicks = builder
                    .comment("Lifetime of non-spreading modern nuclear fire. This prevents uncontrolled vanilla wildfire.")
                    .defineInRange("nuclearFireLifetimeTicks", 80, 1, 12000);
            this.nuclearFalloutColumnsPerTick = builder
                    .comment("Maximum Fallout Rain columns processed per level per server tick.")
                    .defineInRange("nuclearFalloutColumnsPerTick", 512, 1, 20000);
            this.nuclearBlastTerrainPlanColumnsPerTick = builder
                    .comment("Maximum deterministic blast-terrain columns prepared per tick while nuclear rays are calculated.")
                    .defineInRange("nuclearBlastTerrainPlanColumnsPerTick", 16384, 128, 250000);
            this.nuclearBlastTerrainColumnsPerTick = builder
                    .comment("Maximum Sellafield, waste, and tree-char columns applied beside crater excavation per tick.")
                    .defineInRange("nuclearBlastTerrainColumnsPerTick", 8192, 1, 20000);
            this.nuclearBlastTerrainTimeBudgetMs = builder
                    .comment("Maximum wall time spent applying blast-driven terrain conversion during one server tick.")
                    .defineInRange("nuclearBlastTerrainTimeBudgetMs", 18, 1, 40);
            this.enableNuclearTerrainTransformation = builder
                    .comment("Whether Fallout Rain converts terrain into the source-derived waste and Sellafield blocks.")
                    .define("enableNuclearTerrainTransformation", true);
            this.enableNuclearFalloutDeposits = builder
                    .comment("Whether Fallout Rain leaves physical radioactive fallout deposits.")
                    .define("enableNuclearFalloutDeposits", true);
            this.enableNuclearWildfire = builder
                    .comment("Whether Fallout Rain places source-shaped one-in-five fire inside its inner 65 percent. Disabled by default.")
                    .define("enableNuclearWildfire", false);
            builder.pop();
        }
    }
}
