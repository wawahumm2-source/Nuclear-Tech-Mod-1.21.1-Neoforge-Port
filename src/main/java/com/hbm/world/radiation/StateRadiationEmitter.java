package com.hbm.world.radiation;

import net.minecraft.world.level.block.state.BlockState;

/** Radiation source whose dose depends on persisted block state rather than block identity alone. */
public interface StateRadiationEmitter {
    double hbm$getBlockRadiationDose(BlockState state);

    default double hbm$getItemRadiationDose(BlockState state) {
        return hbm$getBlockRadiationDose(state);
    }
}
