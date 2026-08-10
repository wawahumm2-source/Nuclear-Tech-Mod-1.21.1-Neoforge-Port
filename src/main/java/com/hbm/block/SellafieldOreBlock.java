package com.hbm.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.valueproviders.IntProvider;
import org.jetbrains.annotations.Nullable;

/** Modern metadata wrapper for the source Sellafield ore multipass blocks. */
public final class SellafieldOreBlock extends DropExperienceBlock {
    public static final IntegerProperty STAGE = SellafieldSlakedBlock.STAGE;
    public static final IntegerProperty VARIANT = SellafieldSlakedBlock.VARIANT;

    public SellafieldOreBlock(IntProvider experience, BlockBehaviour.Properties properties) {
        super(experience, properties);
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
        return state.setValue(VARIANT,
                SellafieldSlakedBlock.sourceTextureVariant(pos.getX(), pos.getY(), pos.getZ()));
    }
}
