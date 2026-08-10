package com.hbm.client.weapon.render;

import com.hbm.item.HbmGunItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;

public final class HbmGunGeoModel extends GeoModel<HbmGunItem> {
    @Override
    public ResourceLocation getModelResource(HbmGunItem gun) {
        return gun.modelResource();
    }

    @Override
    public ResourceLocation getTextureResource(HbmGunItem gun) {
        return gun.textureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(HbmGunItem gun) {
        return gun.animationResource();
    }

    @Override
    public BakedGeoModel getBakedModel(ResourceLocation location) {
        return ObjBakedGeoModelLoader.load(location);
    }

    @Override
    public RenderType getRenderType(HbmGunItem gun, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
