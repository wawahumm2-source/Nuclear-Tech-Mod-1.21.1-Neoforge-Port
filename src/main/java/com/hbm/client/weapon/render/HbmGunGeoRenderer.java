package com.hbm.client.weapon.render;

import com.hbm.item.HbmGunItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class HbmGunGeoRenderer extends GeoItemRenderer<HbmGunItem> {
    public HbmGunGeoRenderer() {
        super(new HbmGunGeoModel());
        useAlternateGuiLighting();
    }
}
