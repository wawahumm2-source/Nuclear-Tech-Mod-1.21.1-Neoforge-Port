package com.hbm;

import com.hbm.config.HbmConfig;
import com.hbm.network.HbmPayloads;
import com.hbm.registry.HbmBlockEntities;
import com.hbm.registry.HbmBlocks;
import com.hbm.registry.HbmCapabilities;
import com.hbm.registry.HbmCreativeTabs;
import com.hbm.registry.HbmEntities;
import com.hbm.registry.HbmFluids;
import com.hbm.registry.HbmHazards;
import com.hbm.registry.HbmItems;
import com.hbm.registry.HbmMenus;
import com.hbm.registry.HbmRecipes;
import com.hbm.registry.HbmSounds;
import com.hbm.world.radiation.HbmCommonEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(HbmNuclearTech.MOD_ID)
public final class HbmNuclearTech {
    public static final String MOD_ID = "hbm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HbmNuclearTech(IEventBus modEventBus, ModContainer modContainer) {
        HbmBlocks.register(modEventBus);
        HbmItems.register(modEventBus);
        HbmBlockEntities.register(modEventBus);
        HbmMenus.register(modEventBus);
        HbmRecipes.register(modEventBus);
        HbmSounds.register(modEventBus);
        HbmEntities.register(modEventBus);
        HbmFluids.register(modEventBus);
        HbmCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(HbmPayloads::register);
        NeoForge.EVENT_BUS.register(HbmCommonEvents.class);

        modContainer.registerConfig(ModConfig.Type.COMMON, HbmConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HbmHazards::bootstrap);
        LOGGER.info("Hbm's Nuclear Tech NeoForge port foundation loaded.");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        HbmCapabilities.register(event);
    }
}
