package com.hbm.client;

import com.hbm.HbmNuclearTech;
import com.hbm.client.render.BurnerPressRenderer;
import com.hbm.client.screen.BurnerPressScreen;
import com.hbm.registry.HbmBlockEntities;
import com.hbm.registry.HbmMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = HbmNuclearTech.MOD_ID, value = Dist.CLIENT)
public final class HbmNuclearTechClient {
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(HbmMenus.BURNER_PRESS.get(), BurnerPressScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HbmBlockEntities.BURNER_PRESS.get(), BurnerPressRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BurnerPressRenderer.BODY_MODEL);
        event.register(BurnerPressRenderer.HEAD_MODEL);
    }

    private HbmNuclearTechClient() {
    }
}
