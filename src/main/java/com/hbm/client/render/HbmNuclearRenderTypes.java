package com.hbm.client.render;

import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Vanilla-backed render types for persistent nuclear cloud and flare quads. */
public final class HbmNuclearRenderTypes {
    public static final Function<ResourceLocation, RenderType> NUCLEAR_CLOUD =
            Util.memoize((ResourceLocation texture) -> RenderType.entityTranslucent(texture));
    public static final Function<ResourceLocation, RenderType> NUCLEAR_FLARE =
            Util.memoize((ResourceLocation texture) -> RenderType.entityTranslucent(texture));
    public static final Function<ResourceLocation, RenderType> NUCLEAR_EMISSIVE =
            Util.memoize((ResourceLocation texture) -> RenderType.entityTranslucentEmissive(texture));

    private HbmNuclearRenderTypes() {
    }
}
