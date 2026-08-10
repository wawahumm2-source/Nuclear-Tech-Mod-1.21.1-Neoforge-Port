package com.hbm.client.explosion;

import com.hbm.HbmNuclearTech;
import com.hbm.client.render.HbmNuclearRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** NeoForge-safe rendering wrapper for Reloaded 1.12.2's RenderSmallNukeMK4 mushroom. */
final class ReloadedMushroomRenderer {
    private static final ResourceLocation[] FIREBALL = textureSeries("fireball");
    private static final ResourceLocation[] FIREBALL_LIGHTMAP = textureSeries("fireball_lightmap");

    static void render(MultiBufferSource.BufferSource buffer, Vec3 cameraPosition, Vec3 origin,
            int age, float partialTick, float sourceRadius, int maxAge, int fadeStartAge) {
        float visualAge = age + partialTick;
        float scale = ReloadedMushroomTimeline.modelScale(sourceRadius);
        if (scale <= 0F) {
            return;
        }

        int stage = ReloadedMushroomTimeline.textureStage(visualAge, sourceRadius);
        float width = ReloadedMushroomTimeline.headWidth(visualAge);
        float scroll = ReloadedMushroomTimeline.textureScroll(visualAge, maxAge);
        float alpha = ReloadedMushroomTimeline.alpha(visualAge, fadeStartAge, maxAge);
        int color = packColor(1F, 1F, 1F, alpha);

        PoseStack basePose = new PoseStack();
        basePose.translate(origin.x - cameraPosition.x, origin.y - cameraPosition.y, origin.z - cameraPosition.z);
        basePose.scale(scale, scale, scale);
        VertexConsumer base = buffer.getBuffer(HbmNuclearRenderTypes.NUCLEAR_CLOUD.apply(FIREBALL[stage]));
        renderMesh(basePose, base, color, scroll, width);

        PoseStack emissivePose = new PoseStack();
        emissivePose.translate(origin.x - cameraPosition.x, origin.y - cameraPosition.y, origin.z - cameraPosition.z);
        emissivePose.scale(scale * 1.01F, scale * 1.01F, scale * 1.01F);
        VertexConsumer emissive = buffer.getBuffer(
                HbmNuclearRenderTypes.NUCLEAR_EMISSIVE.apply(FIREBALL_LIGHTMAP[stage]));
        renderMesh(emissivePose, emissive, color, scroll, width);
    }

    private static void renderMesh(PoseStack poseStack, VertexConsumer consumer, int color,
            float scroll, float headWidth) {
        ReloadedMushroomModel.renderPart("Stem", poseStack.last(), consumer, color, scroll);
        poseStack.pushPose();
        poseStack.scale(headWidth, 1F, headWidth);
        ReloadedMushroomModel.renderPart("Ball", poseStack.last(), consumer, color, scroll);
        poseStack.popPose();
    }

    private static ResourceLocation[] textureSeries(String folder) {
        ResourceLocation[] textures = new ResourceLocation[11];
        for (int stage = 0; stage < textures.length; stage++) {
            textures[stage] = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID,
                    "textures/models/explosion/" + folder + "/fireball_" + stage + ".png");
        }
        return textures;
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int r = Math.round(Mth.clamp(red, 0F, 1F) * 255F);
        int g = Math.round(Mth.clamp(green, 0F, 1F) * 255F);
        int b = Math.round(Mth.clamp(blue, 0F, 1F) * 255F);
        int a = Math.round(Mth.clamp(alpha, 0F, 1F) * 255F);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private ReloadedMushroomRenderer() {
    }
}
