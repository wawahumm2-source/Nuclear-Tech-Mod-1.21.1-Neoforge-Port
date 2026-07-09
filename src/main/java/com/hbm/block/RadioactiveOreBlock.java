package com.hbm.block;

import com.hbm.world.radiation.RadiationEmitter;
import net.minecraft.world.level.block.Block;

public class RadioactiveOreBlock extends Block implements RadiationEmitter {
    private final double radiationDose;

    public RadioactiveOreBlock(double radiationDose, Properties properties) {
        super(properties);
        this.radiationDose = radiationDose;
    }

    @Override
    public double hbm$getRadiationDose() {
        return this.radiationDose;
    }
}
