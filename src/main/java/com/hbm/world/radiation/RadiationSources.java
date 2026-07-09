package com.hbm.world.radiation;

import com.hbm.world.hazard.HazardRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class RadiationSources {
    public static double fromBlock(BlockState state) {
        double registeredDose = HazardRegistry.getRadiation(state);
        return registeredDose > 0D ? registeredDose : state.getBlock() instanceof RadiationEmitter emitter ? emitter.hbm$getRadiationDose() : 0D;
    }

    public static double fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0D;
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
