package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.block.BurnerPressBlock;
import com.hbm.block.PrototypeNukeBlock;
import com.hbm.block.RadioactiveOreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    private HbmBlocks() {
    }
}
