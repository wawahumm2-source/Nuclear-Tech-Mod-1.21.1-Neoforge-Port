package com.hbm.item;

import com.hbm.client.weapon.render.HbmGunGeoRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/** One reusable item class; weapon-specific behavior is selected by the loaded definition. */
public final class HbmGunItem extends Item implements GeoItem {
    public static final String ANIMATION_CONTROLLER = "weapon";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private final ResourceLocation definitionId;
    private final ResourceLocation modelResource;
    private final ResourceLocation textureResource;
    private final ResourceLocation animationResource;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public HbmGunItem(Properties properties, ResourceLocation definitionId,
                      ResourceLocation modelResource, ResourceLocation textureResource,
                      ResourceLocation animationResource) {
        super(properties);
        this.definitionId = definitionId;
        this.modelResource = modelResource;
        this.textureResource = textureResource;
        this.animationResource = animationResource;
        GeoItem.registerSyncedAnimatable(this);
    }

    public ResourceLocation definitionId() {
        return definitionId;
    }

    public ResourceLocation modelResource() {
        return modelResource;
    }

    public ResourceLocation textureResource() {
        return textureResource;
    }

    public ResourceLocation animationResource() {
        return animationResource;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<HbmGunItem> controller = new AnimationController<>(
                this,
                ANIMATION_CONTROLLER,
                2,
                state -> state.setAndContinue(IDLE)
        );
        controller.triggerableAnim("idle", RawAnimation.begin().thenLoop("idle"));
        controller.triggerableAnim("ads", RawAnimation.begin().thenPlayAndHold("ads"));
        controller.triggerableAnim("sprint", RawAnimation.begin().thenLoop("sprint"));
        controller.triggerableAnim("lower", RawAnimation.begin().thenPlayAndHold("lower"));
        for (String animation : new String[]{
                "equip", "fire", "dry_fire", "reload_start", "reload_loop", "reload_end",
                "reload_normal", "reload_empty", "inspect"
        }) {
            controller.triggerableAnim(animation, RawAnimation.begin().thenPlay(animation));
        }
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HbmGunGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new HbmGunGeoRenderer();
                }
                return renderer;
            }
        });
    }
}
