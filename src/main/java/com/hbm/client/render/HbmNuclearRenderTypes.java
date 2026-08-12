package com.hbm.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Vanilla-backed render types for persistent nuclear cloud and flare quads. */
public final class HbmNuclearRenderTypes {
    public static final RenderType NUCLEAR_FLASH = createNuclearFlash();
    public static final Function<ResourceLocation, RenderType> NUCLEAR_CLOUD =
            Util.memoize(HbmNuclearRenderTypes::createNuclearCloud);
    public static final Function<ResourceLocation, RenderType> NUCLEAR_FLARE =
            Util.memoize(HbmNuclearRenderTypes::createNuclearFlare);
    public static final Function<ResourceLocation, RenderType> NUCLEAR_EMISSIVE =
            Util.memoize((ResourceLocation texture) -> RenderType.entityTranslucentEmissive(texture));

    private static RenderType createNuclearCloud(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("hbm_nuclear_cloud", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 1_536, false, true, state);
    }

    private static RenderType createNuclearFlare(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("hbm_nuclear_flare", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 1_536, false, true, state);
    }

    private static RenderType createNuclearFlash() {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setCullState(RenderStateShard.CULL)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("hbm_nuclear_flash", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES, 1_536, false, false, state);
    }

    private HbmNuclearRenderTypes() {
    }
}
