package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.block.BurnerPressBlock;
import com.hbm.block.GasRadonBlock;
import com.hbm.block.GasRadonDenseBlock;
import com.hbm.block.GasRadonTombBlock;
import com.hbm.block.HbmTntBlock;
import com.hbm.block.FalloutDepositBlock;
import com.hbm.block.NuclearFireBlock;
import com.hbm.block.NukeBoyBlock;
import com.hbm.block.PrototypeNukeBlock;
import com.hbm.block.RadioactiveOreBlock;
import com.hbm.block.SellafieldSlakedBlock;
import com.hbm.block.SellafieldBlock;
import com.hbm.block.SellafieldOreBlock;
import com.hbm.block.WasteEarthBlock;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HbmNuclearTech.MOD_ID);

    public static final DeferredBlock<Block> URANIUM_ORE = BLOCKS.register("uranium_ore",
            () -> new RadioactiveOreBlock(0.08D, oreProperties(MapColor.STONE)));

    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = BLOCKS.register("deepslate_uranium_ore",
            () -> new RadioactiveOreBlock(0.12D, oreProperties(MapColor.DEEPSLATE).strength(4.5F, 4.5F).sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> LEAD_ORE = BLOCKS.register("lead_ore",
            () -> new Block(oreProperties(MapColor.STONE)));

    public static final DeferredBlock<Block> RADIOACTIVE_WASTE_BARREL = BLOCKS.register("radioactive_waste_barrel",
            () -> new RadioactiveOreBlock(0.20D, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .requiresCorrectToolForDrops()
                    .strength(3F, 6F)
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<Block> BURNER_PRESS = BLOCKS.register("burner_press",
            () -> new BurnerPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(4F, 8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> PRESS_PREHEATER = BLOCKS.register("press_preheater",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(4F, 8F)
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<Block> PROTOTYPE_NUKE = BLOCKS.register("prototype_nuke",
            () -> new PrototypeNukeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1F, 0F)
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<Block> NUKE_BOY = BLOCKS.register("nuke_boy",
            () -> new NukeBoyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1F, 0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> TNT = BLOCKS.register("tnt",
            () -> new HbmTntBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0F, 0F)
                    .sound(SoundType.GRASS)));

    public static final DeferredBlock<Block> SELLAFIELD_SLAKED = BLOCKS.register("sellafield_slaked",
            () -> new SellafieldSlakedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(4F, 6F)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> SELLAFIELD = BLOCKS.register("sellafield",
            () -> new SellafieldBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .requiresCorrectToolForDrops()
                    .strength(4F, 6F)
                    .sound(SoundType.STONE)
                    .randomTicks()));

    public static final DeferredBlock<Block> SELLAFIELD_BEDROCK = BLOCKS.register("sellafield_bedrock",
            () -> new SellafieldSlakedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(-1F, 3_600_000F)
                    .sound(SoundType.STONE)
                    .noLootTable()));

    public static final DeferredBlock<Block> ORE_SELLAFIELD_DIAMOND = BLOCKS.register("ore_sellafield_diamond",
            () -> new SellafieldOreBlock(UniformInt.of(3, 7), sellafieldOreProperties()));

    public static final DeferredBlock<Block> ORE_SELLAFIELD_EMERALD = BLOCKS.register("ore_sellafield_emerald",
            () -> new SellafieldOreBlock(UniformInt.of(3, 7), sellafieldOreProperties()));

    public static final DeferredBlock<Block> ORE_SELLAFIELD_URANIUM_SCORCHED = BLOCKS.register(
            "ore_sellafield_uranium_scorched",
            () -> new SellafieldOreBlock(ConstantInt.ZERO, sellafieldOreProperties()));

    public static final DeferredBlock<Block> ORE_SELLAFIELD_SCHRABIDIUM = BLOCKS.register(
            "ore_sellafield_schrabidium",
            () -> new SellafieldOreBlock(ConstantInt.ZERO, sellafieldOreProperties()));

    public static final DeferredBlock<Block> ORE_SELLAFIELD_RADGEM = BLOCKS.register("ore_sellafield_radgem",
            () -> new SellafieldOreBlock(UniformInt.of(3, 7), sellafieldOreProperties()));

    public static final DeferredBlock<Block> WASTE_EARTH = BLOCKS.register("waste_earth",
            () -> new WasteEarthBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .randomTicks()));

    public static final DeferredBlock<Block> GAS_RADON = BLOCKS.register("gas_radon",
            () -> new GasRadonBlock(gasProperties()));

    public static final DeferredBlock<Block> GAS_RADON_DENSE = BLOCKS.register("gas_radon_dense",
            () -> new GasRadonDenseBlock(gasProperties()));

    public static final DeferredBlock<Block> GAS_RADON_TOMB = BLOCKS.register("gas_radon_tomb",
            () -> new GasRadonTombBlock(gasProperties()));

    public static final DeferredBlock<Block> WASTE_LOG = BLOCKS.register("waste_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2F)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<Block> WASTE_PLANKS = BLOCKS.register("waste_planks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2F)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<Block> WASTE_LEAVES = BLOCKS.register("waste_leaves",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()));

    public static final DeferredBlock<Block> WASTE_MYCELIUM = BLOCKS.register("waste_mycelium",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)));

    public static final DeferredBlock<Block> WASTE_TRINITITE = BLOCKS.register("waste_trinitite",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.6F)
                    .sound(SoundType.SAND)));

    public static final DeferredBlock<Block> WASTE_TRINITITE_RED = BLOCKS.register("waste_trinitite_red",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.6F)
                    .sound(SoundType.SAND)));

    public static final DeferredBlock<Block> FALLOUT = BLOCKS.register("fallout",
            () -> new FalloutDepositBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .replaceable()
                    .noCollission()
                    .strength(0.1F)
                    .sound(SoundType.SAND)));

    public static final DeferredBlock<Block> NUCLEAR_FIRE = BLOCKS.register("nuclear_fire",
            () -> new NuclearFireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .replaceable()
                    .noCollission()
                    .lightLevel(state -> 15)
                    .strength(0F)
                    .sound(SoundType.WOOL)
                    .noLootTable()));

    public static final DeferredBlock<LiquidBlock> CONTAMINATED_WATER = BLOCKS.register(
            "contaminated_water",
            () -> new LiquidBlock(HbmFluids.CONTAMINATED_WATER.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .replaceable()
                    .liquid()
                    .noCollission()
                    .strength(100F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable())
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static BlockBehaviour.Properties oreProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .requiresCorrectToolForDrops()
                .strength(3F, 3F)
                .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties gasProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .replaceable()
                .noCollission()
                .noOcclusion()
                .strength(0F)
                .noLootTable();
    }

    private static BlockBehaviour.Properties sellafieldOreProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(5F, 6F)
                .sound(SoundType.STONE);
    }

    private HbmBlocks() {
    }
}
