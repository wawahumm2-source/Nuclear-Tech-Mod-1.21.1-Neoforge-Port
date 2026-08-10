package com.hbm.client;

import com.hbm.HbmNuclearTech;
import com.hbm.client.radiation.RadiationClientState;
import com.hbm.client.radiation.RadiationInspectorOverlay;
import com.hbm.client.explosion.NuclearVisualEffectManager;
import com.hbm.client.explosion.NuclearPresentationOverlay;
import com.hbm.client.particle.NuclearCloudParticle;
import com.hbm.client.render.BurnerPressRenderer;
import com.hbm.client.render.BurnerPressItemRenderer;
import com.hbm.client.screen.BurnerPressScreen;
import com.hbm.client.weapon.render.ObjBakedGeoModelLoader;
import com.hbm.network.HbmPayloads;
import com.hbm.registry.HbmBlockEntities;
import com.hbm.registry.HbmBlocks;
import com.hbm.registry.HbmEntities;
import com.hbm.registry.HbmFluids;
import com.hbm.registry.HbmItems;
import com.hbm.registry.HbmMenus;
import com.hbm.registry.HbmParticles;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = HbmNuclearTech.MOD_ID, value = Dist.CLIENT)
public final class HbmNuclearTechClient {
    private static final float CONTAMINATED_WATER_BRIGHTNESS = 0.88F;
    private static final int DEFAULT_WATER_COLOR = 0x3F76E4;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        HbmPayloads.setClientRadiationHandler(RadiationClientState::accept);
        HbmPayloads.setClientEffectHandler(NuclearVisualEffectManager::accept);
        HbmPayloads.setClientNuclearProgressHandler(NuclearVisualEffectManager::acceptProgress);
        NeoForge.EVENT_BUS.addListener(RadiationInspectorOverlay::render);
        NeoForge.EVENT_BUS.addListener(NuclearVisualEffectManager::tick);
        NeoForge.EVENT_BUS.addListener(NuclearVisualEffectManager::render);
        NeoForge.EVENT_BUS.addListener(NuclearPresentationOverlay::beforeGui);
        NeoForge.EVENT_BUS.addListener(NuclearPresentationOverlay::render);
        NeoForge.EVENT_BUS.addListener(NuclearPresentationOverlay::applyCameraShake);
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(HbmFluids.CONTAMINATED_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(HbmFluids.FLOWING_CONTAMINATED_WATER.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(HbmMenus.BURNER_PRESS.get(), BurnerPressScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HbmBlockEntities.BURNER_PRESS.get(), BurnerPressRenderer::new);
        event.registerEntityRenderer(HbmEntities.HBM_TNT.get(), TntRenderer::new);
        event.registerEntityRenderer(HbmEntities.BALLISTIC_PROJECTILE.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BurnerPressRenderer.BODY_MODEL);
        event.register(BurnerPressRenderer.HEAD_MODEL);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(HbmParticles.NUCLEAR_CLOUD.get(),
                sprites -> new NuclearCloudParticle.Provider(sprites, NuclearCloudParticle.Style.CLOUD));
        event.registerSpriteSet(HbmParticles.NUCLEAR_SHOCK.get(),
                sprites -> new NuclearCloudParticle.Provider(sprites, NuclearCloudParticle.Style.SHOCK));
        event.registerSpriteSet(HbmParticles.NUCLEAR_RING.get(),
                sprites -> new NuclearCloudParticle.Provider(sprites, NuclearCloudParticle.Style.RING));
        event.registerSpriteSet(HbmParticles.NUCLEAR_FLASH.get(),
                sprites -> new NuclearCloudParticle.Provider(sprites, NuclearCloudParticle.Style.FLASH));
        event.registerSpriteSet(HbmParticles.NUCLEAR_CAP.get(),
                sprites -> new NuclearCloudParticle.Provider(sprites, NuclearCloudParticle.Style.CAP));
        event.registerSpriteSet(HbmParticles.NUCLEAR_FALLOUT.get(),
                sprites -> new NuclearCloudParticle.Provider(sprites, NuclearCloudParticle.Style.FALLOUT));
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(SellafieldColors::blockColor,
                HbmBlocks.SELLAFIELD_SLAKED.get(),
                HbmBlocks.SELLAFIELD_BEDROCK.get(),
                HbmBlocks.ORE_SELLAFIELD_DIAMOND.get(),
                HbmBlocks.ORE_SELLAFIELD_EMERALD.get(),
                HbmBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get(),
                HbmBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get(),
                HbmBlocks.ORE_SELLAFIELD_RADGEM.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(SellafieldColors::itemColor,
                HbmItems.SELLAFIELD_SLAKED.get(),
                HbmItems.SELLAFIELD_BEDROCK.get(),
                HbmItems.ORE_SELLAFIELD_DIAMOND.get(),
                HbmItems.ORE_SELLAFIELD_EMERALD.get(),
                HbmItems.ORE_SELLAFIELD_URANIUM_SCORCHED.get(),
                HbmItems.ORE_SELLAFIELD_SCHRABIDIUM.get(),
                HbmItems.ORE_SELLAFIELD_RADGEM.get());
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        BurnerPressItemRenderer itemRenderer = new BurnerPressItemRenderer();
        event.registerItem(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return itemRenderer;
            }
        }, HbmItems.BURNER_PRESS.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(
                    "minecraft", "block/water_still");
            private static final ResourceLocation FLOWING = ResourceLocation.fromNamespaceAndPath(
                    "minecraft", "block/water_flow");

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }

            @Override
            public int getTintColor() {
                return darkenWaterColor(DEFAULT_WATER_COLOR);
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return darkenWaterColor(BiomeColors.getAverageWaterColor(getter, pos));
            }
        }, HbmFluids.CONTAMINATED_WATER_TYPE.get());
    }

    private static int darkenWaterColor(int color) {
        int red = Math.round(((color >> 16) & 0xFF) * CONTAMINATED_WATER_BRIGHTNESS);
        int green = Math.round(((color >> 8) & 0xFF) * CONTAMINATED_WATER_BRIGHTNESS);
        int blue = Math.round((color & 0xFF) * CONTAMINATED_WATER_BRIGHTNESS);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) ObjBakedGeoModelLoader::clearCache);
    }

    private HbmNuclearTechClient() {
    }
}
