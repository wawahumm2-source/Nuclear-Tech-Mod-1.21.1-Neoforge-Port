package com.hbm.world.explosion;

import com.hbm.block.SellafieldSlakedBlock;
import com.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Ordered modern wrapper for Tier 1 FalloutConfigJSON. */
final class HbmFalloutMapper {
    private HbmFalloutMapper() {
    }

    static Mapping map(BlockState state, BlockPos pos, double distancePercent, long seed, int woodEffectPercent) {
        if (state.isAir() || state.is(HbmBlocks.FALLOUT.get()) || state.is(HbmBlocks.NUCLEAR_FIRE.get())) {
            return Mapping.unchanged(state);
        }

        if (state.is(BlockTags.LOGS) && distancePercent <= woodEffectPercent) {
            return Mapping.converted(wasteLog(state), false);
        }
        if (state.is(Blocks.MUSHROOM_STEM) && distancePercent <= woodEffectPercent) {
            return Mapping.converted(HbmBlocks.WASTE_LOG.get().defaultBlockState(), false);
        }
        if ((state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK))
                && distancePercent <= woodEffectPercent) {
            return Mapping.converted(Blocks.AIR.defaultBlockState(), false);
        }
        if (state.is(Blocks.SNOW) && distancePercent <= woodEffectPercent) {
            return Mapping.converted(Blocks.AIR.defaultBlockState(), false);
        }
        if (state.is(BlockTags.PLANKS) && distancePercent <= woodEffectPercent) {
            return Mapping.converted(HbmBlocks.WASTE_PLANKS.get().defaultBlockState(), false);
        }

        // FalloutConfigJSON destroys remaining Material.wood blocks after explicit logs and planks.
        if (state.is(BlockTags.MINEABLE_WITH_AXE) && distancePercent <= woodEffectPercent) {
            return Mapping.converted(Blocks.AIR.defaultBlockState(), false);
        }

        if (state.is(HbmBlocks.WASTE_LEAVES.get()) && distancePercent <= woodEffectPercent) {
            return Mapping.converted(Blocks.AIR.defaultBlockState(), false);
        }
        if (state.is(BlockTags.LEAVES)) {
            if (distancePercent <= woodEffectPercent) {
                return Mapping.converted(Blocks.AIR.defaultBlockState(), false);
            }
            if (distancePercent >= woodEffectPercent - 5D) {
                return Mapping.converted(HbmBlocks.WASTE_LEAVES.get().defaultBlockState(), false);
            }
        }

        if (isPlantOrVine(state) && distancePercent <= woodEffectPercent) {
            return Mapping.converted(Blocks.AIR.defaultBlockState(), false);
        }

        if (state.is(Blocks.MOSSY_COBBLESTONE)) {
            return Mapping.converted(Blocks.COAL_ORE.defaultBlockState(), false);
        }

        int stage = HbmFalloutMath.sellafieldStage(distancePercent);
        if (stage >= 0) {
            if (state.is(HbmBlocks.SELLAFIELD_BEDROCK.get())) {
                int currentStage = state.getValue(SellafieldSlakedBlock.STAGE);
                return currentStage >= stage
                        ? Mapping.unchanged(state)
                        : Mapping.converted(staged(HbmBlocks.SELLAFIELD_BEDROCK.get(), pos, stage), true);
            }
            if (state.is(Blocks.BEDROCK)) {
                return Mapping.converted(staged(HbmBlocks.SELLAFIELD_BEDROCK.get(), pos, stage), true);
            }
            if (isCoalOre(state)) {
                return Mapping.converted(coalOutcome(pos, stage, seed), true);
            }
            if (isUraniumOre(state) && stage > 4) {
                Block block = HbmFalloutMath.deterministicRoll(seed, pos.getX(), pos.getY(), pos.getZ(), 43, 10) == 0
                        ? HbmBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get()
                        : HbmBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get();
                return Mapping.converted(staged(block, pos, stage), true);
            }
            if (isDiamondOre(state)) {
                return Mapping.converted(staged(HbmBlocks.ORE_SELLAFIELD_RADGEM.get(), pos, stage), true);
            }
            if (state.is(Blocks.MYCELIUM)
                    && distancePercent <= HbmFalloutMath.SOURCE_GRASS_SELLAFIELD_MAX_PERCENT) {
                return sellafield(state, pos, stage);
            }
            if (state.is(Blocks.GRASS_BLOCK)
                    && distancePercent <= HbmFalloutMath.SOURCE_GRASS_SELLAFIELD_MAX_PERCENT) {
                return sellafield(state, pos, stage);
            }
            if (isSellafieldMaterial(state)) {
                return sellafield(state, pos, stage);
            }
        }

        if (state.is(Blocks.MYCELIUM)) {
            return Mapping.converted(HbmBlocks.WASTE_MYCELIUM.get().defaultBlockState(), false);
        }
        if (state.is(Blocks.GRASS_BLOCK)) {
            return Mapping.converted(HbmBlocks.WASTE_EARTH.get().defaultBlockState(), false);
        }
        if (state.is(BlockTags.SAND)
                && HbmFalloutMath.deterministicUnit(seed, pos.getX(), pos.getY(), pos.getZ(), 31) < 0.05D) {
            return Mapping.converted(state.is(Blocks.RED_SAND)
                    ? HbmBlocks.WASTE_TRINITITE_RED.get().defaultBlockState()
                    : HbmBlocks.WASTE_TRINITITE.get().defaultBlockState(), false);
        }
        if (state.is(Blocks.CLAY)) {
            return Mapping.converted(Blocks.TERRACOTTA.defaultBlockState(), false);
        }

        return Mapping.unchanged(state);
    }

    private static Mapping sellafield(BlockState current, BlockPos pos, int stage) {
        if (current.is(HbmBlocks.SELLAFIELD_SLAKED.get())
                && current.getValue(SellafieldSlakedBlock.STAGE) >= stage) {
            return Mapping.unchanged(current);
        }
        return Mapping.converted(staged(HbmBlocks.SELLAFIELD_SLAKED.get(), pos, stage), true);
    }

    private static BlockState coalOutcome(BlockPos pos, int stage, long seed) {
        if (HbmFalloutMath.deterministicUnit(seed, pos.getX(), pos.getY(), pos.getZ(), 41) >= 0.5D) {
            return staged(HbmBlocks.SELLAFIELD_SLAKED.get(), pos, stage);
        }
        int weighted = HbmFalloutMath.deterministicRoll(seed, pos.getX(), pos.getY(), pos.getZ(), 42, 5);
        Block result = weighted < 3
                ? HbmBlocks.ORE_SELLAFIELD_DIAMOND.get()
                : HbmBlocks.ORE_SELLAFIELD_EMERALD.get();
        return staged(result, pos, stage);
    }

    private static BlockState staged(Block block, BlockPos pos, int stage) {
        return block.defaultBlockState()
                .setValue(SellafieldSlakedBlock.STAGE, stage)
                .setValue(SellafieldSlakedBlock.VARIANT,
                        HbmFalloutMath.sourceTextureVariant(pos.getX(), pos.getY(), pos.getZ()));
    }

    private static BlockState wasteLog(BlockState source) {
        BlockState result = HbmBlocks.WASTE_LOG.get().defaultBlockState();
        if (source.hasProperty(RotatedPillarBlock.AXIS)) {
            result = result.setValue(RotatedPillarBlock.AXIS, source.getValue(RotatedPillarBlock.AXIS));
        }
        return result;
    }

    private static boolean isPlantOrVine(BlockState state) {
        return state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS)
                || state.is(Blocks.VINE)
                || state.is(Blocks.GLOW_LICHEN)
                || state.is(Blocks.CAVE_VINES)
                || state.is(Blocks.CAVE_VINES_PLANT);
    }

    private static boolean isCoalOre(BlockState state) {
        return state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE);
    }

    private static boolean isDiamondOre(BlockState state) {
        return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE);
    }

    private static boolean isUraniumOre(BlockState state) {
        return state.is(HbmBlocks.URANIUM_ORE.get()) || state.is(HbmBlocks.DEEPSLATE_URANIUM_ORE.get());
    }

    private static boolean isSellafieldMaterial(BlockState state) {
        // Tier 1 handles grass and mycelium through narrower, explicit entries.
        // Letting the modern dirt tag catch them here incorrectly extends Sellafield grass to 50 percent.
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)) {
            return false;
        }
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.IRON_BARS)
                || state.is(Blocks.IRON_DOOR)
                || state.is(Blocks.IRON_TRAPDOOR)
                || state.is(Blocks.CHAIN)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL);
    }

    record Mapping(BlockState state, boolean consumesDepth) {
        static Mapping unchanged(BlockState state) {
            return new Mapping(state, false);
        }

        static Mapping converted(BlockState state, boolean consumesDepth) {
            return new Mapping(state, consumesDepth);
        }
    }
}
