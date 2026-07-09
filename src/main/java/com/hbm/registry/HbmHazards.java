package com.hbm.registry;

import com.hbm.world.hazard.HazardData;
import com.hbm.world.hazard.HazardRegistry;

public final class HbmHazards {
    public static void bootstrap() {
        HazardRegistry.registerBlock(HbmBlocks.URANIUM_ORE.get(), HazardData.radiation(0.08D));
        HazardRegistry.registerBlock(HbmBlocks.DEEPSLATE_URANIUM_ORE.get(), HazardData.radiation(0.12D));
        HazardRegistry.registerBlock(HbmBlocks.RADIOACTIVE_WASTE_BARREL.get(), HazardData.radiation(0.20D));
        HazardRegistry.registerBlock(HbmBlocks.PROTOTYPE_NUKE.get(), HazardData.radiation(0.35D));

        HazardRegistry.registerItem(HbmItems.URANIUM_ORE.get(), HazardData.radiation(0.08D));
        HazardRegistry.registerItem(HbmItems.DEEPSLATE_URANIUM_ORE.get(), HazardData.radiation(0.12D));
        HazardRegistry.registerItem(HbmItems.RADIOACTIVE_WASTE_BARREL.get(), HazardData.radiation(0.20D));
        HazardRegistry.registerItem(HbmItems.PROTOTYPE_NUKE.get(), HazardData.radiation(0.35D));
        HazardRegistry.registerItem(HbmItems.URANIUM_INGOT.get(), HazardData.radiation(0.05D));
        HazardRegistry.registerItem(HbmItems.URANIUM_FUEL_PELLET.get(), HazardData.radiation(0.18D));
    }

    private HbmHazards() {
    }
}
