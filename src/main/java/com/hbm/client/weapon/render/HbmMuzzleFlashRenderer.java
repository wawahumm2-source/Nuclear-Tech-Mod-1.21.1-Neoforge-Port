package com.hbm.client.weapon.render;

import com.hbm.HbmNuclearTech;
import com.hbm.client.render.HbmNuclearRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Model-local muzzle flare. The bone-bound quad technique is adapted from Superb Warfare's
 * AnimationHelper at commit 9b5284f42ef79532e6fb7f03ab07425c693b0b43 (GPL-3.0). The texture,
 * rig location, scale, and timing are HBM resources and HBM-authored values.
 */
final class HbmMuzzleFlashRenderer {
    private static final ResourceLocation FLARE = ResourceLocation.fromNamespaceAndPath(
            HbmNuclearTech.MOD_ID, "textures/particle/flare.png");

    static void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       GeoBone bone, float baseSize, ItemDisplayContext perspective) {
        float strength = SuperbGunPresentationState.flashStrength();
        if (strength <= 0.01F || perspective == null || !perspective.firstPerson()) {
            return;
        }

        float size = baseSize * (0.82F + strength * 0.28F);
        poseStack.pushPose();
        try {
            RenderUtil.translateMatrixToBone(poseStack, bone);
            RenderUtil.translateToPivotPoint(poseStack, bone);
            RenderUtil.rotateMatrixAroundBone(poseStack, bone);
            RenderUtil.scaleMatrixForBone(poseStack, bone);
            RenderUtil.translateAwayFromPivotPoint(poseStack, bone);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(
                    SuperbGunPresentationState.flashRotation()));

            PoseStack.Pose pose = poseStack.last();
            VertexConsumer consumer = buffers.getBuffer(HbmNuclearRenderTypes.NUCLEAR_EMISSIVE.apply(FLARE));
            vertex(consumer, pose, -size, -size, 0.0F, 1.0F, packedLight);
            vertex(consumer, pose, size, -size, 1.0F, 1.0F, packedLight);
            vertex(consumer, pose, size, size, 1.0F, 0.0F, packedLight);
            vertex(consumer, pose, -size, size, 0.0F, 0.0F, packedLight);
        } finally {
            poseStack.popPose();
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float u, float v, int packedLight) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 235)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Math.max(packedLight, LightTexture.FULL_BRIGHT))
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private HbmMuzzleFlashRenderer() {
    }
}
