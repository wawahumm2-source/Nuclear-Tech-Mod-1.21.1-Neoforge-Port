package com.hbm.registry;

import com.hbm.config.HbmConfig;
import com.hbm.world.hazard.HazardData;
import com.hbm.world.hazard.HazardRegistry;

public final class HbmHazards {
    public static void bootstrap() {
        HazardRegistry.registerBlock(HbmBlocks.URANIUM_ORE.get(), HazardData.radiation(0.35D));
        HazardRegistry.registerBlock(HbmBlocks.DEEPSLATE_URANIUM_ORE.get(), HazardData.radiation(0.35D));
        HazardRegistry.registerBlock(HbmBlocks.RADIOACTIVE_WASTE_BARREL.get(), HazardData.radiation(5D));
        HazardRegistry.registerBlock(HbmBlocks.PROTOTYPE_NUKE.get(), HazardData.radiation(0.35D));
        HazardRegistry.registerBlock(HbmBlocks.FALLOUT.get(), HazardData.radiation(0.5D));
        HazardRegistry.registerBlock(HbmBlocks.CONTAMINATED_WATER.get(),
                HazardData.radiation(HbmConfig.RADIATION.contaminatedWaterAmbientRate.get()));

        HazardRegistry.registerItem(HbmItems.URANIUM_ORE.get(), HazardData.radiation(0.35D));
        HazardRegistry.registerItem(HbmItems.DEEPSLATE_URANIUM_ORE.get(), HazardData.radiation(0.35D));
        HazardRegistry.registerItem(HbmItems.RADIOACTIVE_WASTE_BARREL.get(), HazardData.radiation(5D));
        HazardRegistry.registerItem(HbmItems.PROTOTYPE_NUKE.get(), HazardData.radiation(0.35D));
        HazardRegistry.registerItem(HbmItems.URANIUM_INGOT.get(), HazardData.radiation(0.35D));
        HazardRegistry.registerItem(HbmItems.URANIUM_FUEL_PELLET.get(), HazardData.radiation(0.05D));
        HazardRegistry.registerItem(HbmItems.FALLOUT.get(), HazardData.radiation(0.5D));
        HazardRegistry.registerItem(HbmItems.CONTAMINATED_WATER_BUCKET.get(),
                HazardData.radiation(HbmConfig.RADIATION.contaminatedWaterBucketRate.get()));
    }

    private HbmHazards() {
    }
}
