package com.hbm.block;

import com.hbm.registry.HbmItems;
import com.hbm.world.radiation.ChunkRadiationService;
import com.hbm.world.radiation.RadiationManager;
import com.hbm.world.radiation.RadiationSourceType;
import com.hbm.world.radiation.SellafieldMath;
import com.hbm.world.radiation.StateRadiationEmitter;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

/** Six source Sellafite tiers. This is distinct from the ten-stage gray Sellafield Slaked terrain. */
public final class SellafieldBlock extends Block implements StateRadiationEmitter {
    public static final int LEVEL_COUNT = 6;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, LEVEL_COUNT - 1);
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);

    public SellafieldBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, VARIANT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        var pos = context.getClickedPos();
        return state.setValue(VARIANT,
                SellafieldSlakedBlock.sourceTextureVariant(pos.getX(), pos.getY(), pos.getZ()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int sellafiteLevel = state.getValue(LEVEL);
        ChunkRadiationService.incrementRad(level, pos, 0.5D * (sellafiteLevel + 1));
        if (random.nextInt(sellafiteLevel == 0 ? 25 : 15) != 0) {
            return;
        }
        if (sellafiteLevel > 0) {
            level.setBlock(pos, state.setValue(LEVEL, sellafiteLevel - 1), 2);
        } else {
            level.setBlock(pos, com.hbm.registry.HbmBlocks.SELLAFIELD_SLAKED.get().defaultBlockState()
                    .setValue(SellafieldSlakedBlock.VARIANT, state.getValue(VARIANT)), 3);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            int sellafiteLevel = state.getValue(LEVEL);
            int amplifier = sellafiteLevel < 5 ? sellafiteLevel : sellafiteLevel * 2;
            RadiationManager.applyDirectExposure(
                    living,
                    RadiationSourceType.BLOCK,
                    0.05D * (amplifier + 1),
                    true
            );
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return SellafieldBlockItem.createStack(HbmItems.SELLAFIELD.get(), state.getValue(LEVEL));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(SellafieldBlockItem.createStack(HbmItems.SELLAFIELD.get(), state.getValue(LEVEL)));
    }

    @Override
    public double hbm$getBlockRadiationDose(BlockState state) {
        return 0.5D * (state.getValue(LEVEL) + 1);
    }

    @Override
    public double hbm$getItemRadiationDose(BlockState state) {
        return SellafieldMath.itemRadiation(state.getValue(LEVEL));
    }

    public static double itemRadiation(int level) {
        return SellafieldMath.itemRadiation(level);
    }
}
