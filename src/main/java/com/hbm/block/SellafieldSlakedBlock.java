package com.hbm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/** Source-style Sellafield terrain with persisted fallout stage and texture variant. */
public class SellafieldSlakedBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 9);
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);

    public SellafieldSlakedBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, VARIANT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        var pos = context.getClickedPos();
        return state.setValue(VARIANT, sourceTextureVariant(pos.getX(), pos.getY(), pos.getZ()));
    }

    public static int sourceTextureVariant(int x, int y, int z) {
        long value = (long) (x * 3_129_871) ^ (long) y * 116_129_781L ^ z;
        value = value * value * 42_317_861L + value * 11L;
        return (int) (value >>> 16 & 3L);
    }
}
