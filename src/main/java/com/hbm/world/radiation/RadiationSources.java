package com.hbm.world.radiation;

import com.hbm.world.hazard.HazardRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;

public final class RadiationSources {
    public static double fromBlock(BlockState state) {
        if (state.getBlock() instanceof StateRadiationEmitter emitter) {
            return emitter.hbm$getBlockRadiationDose(state);
        }
        double registeredDose = HazardRegistry.getRadiation(state);
        return registeredDose > 0D ? registeredDose : state.getBlock() instanceof RadiationEmitter emitter ? emitter.hbm$getRadiationDose() : 0D;
    }

    public static double fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0D;
        }

        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof StateRadiationEmitter stateEmitter) {
            BlockItemStateProperties properties = stack.getOrDefault(
                    DataComponents.BLOCK_STATE,
                    BlockItemStateProperties.EMPTY
            );
            BlockState state = properties.apply(blockItem.getBlock().defaultBlockState());
            return stateEmitter.hbm$getItemRadiationDose(state) * stack.getCount();
        }

        double registeredDose = HazardRegistry.getRadiation(stack);
        if (registeredDose > 0D) {
            return registeredDose;
        }

        double itemDose = stack.getItem() instanceof RadiationEmitter emitter ? emitter.hbm$getRadiationDose() : 0D;
        double blockDose = stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof RadiationEmitter emitter
                ? emitter.hbm$getRadiationDose()
                : 0D;
        return Math.max(itemDose, blockDose) * stack.getCount();
    }

    private RadiationSources() {
    }
}
