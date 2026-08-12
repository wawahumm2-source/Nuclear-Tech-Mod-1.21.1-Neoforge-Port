package com.hbm.client.weapon.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.PlayerModelPart;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

/**
 * Renders the local player's real skin and enabled sleeve layer over animated weapon hand bones.
 * The Gecko bone transform/render sequence is adapted from Superb Warfare AnimationHelper at
 * commit 9b5284f42ef79532e6fb7f03ab07425c693b0b43 (GPL-3.0).
 */
final class HbmPlayerArmRenderer {
    static void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       GeoBone bone, boolean left, RenderType gunRenderType, float armScale) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null || !(minecraft.getEntityRenderDispatcher().getRenderer(player)
                instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        ModelPart arm = left ? model.leftArm : model.rightArm;
        ModelPart sleeve = left ? model.leftSleeve : model.rightSleeve;
        PartState armState = PartState.capture(arm);
        PartState sleeveState = PartState.capture(sleeve);

        poseStack.pushPose();
        try {
            RenderUtil.translateMatrixToBone(poseStack, bone);
            RenderUtil.translateToPivotPoint(poseStack, bone);
            RenderUtil.rotateMatrixAroundBone(poseStack, bone);
            RenderUtil.scaleMatrixForBone(poseStack, bone);
            RenderUtil.translateAwayFromPivotPoint(poseStack, bone);
            poseStack.translate(left ? -0.0625D : 0.0625D, 0.125D, 0.0D);
            poseStack.scale(armScale, armScale, armScale);

            configure(arm, bone);
            configure(sleeve, bone);
            var skin = player.getSkin().texture();
            arm.render(poseStack, buffers.getBuffer(RenderType.entitySolid(skin)),
                    packedLight, OverlayTexture.NO_OVERLAY);

            PlayerModelPart sleevePart = left ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE;
            if (minecraft.options.isModelPartEnabled(sleevePart)) {
                sleeve.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(skin)),
                        packedLight, OverlayTexture.NO_OVERLAY);
            }
            if (gunRenderType != null) {
                buffers.getBuffer(gunRenderType);
            }
        } finally {
            poseStack.popPose();
            armState.restore(arm);
            sleeveState.restore(sleeve);
        }
    }

    private static void configure(ModelPart part, GeoBone bone) {
        part.visible = true;
        // Exact setupModelFromBone2 transform used by Superb Warfare's MP-443.
        part.setPos(bone.getPivotX(), bone.getPivotY() + 7.0F, bone.getPivotZ());
        part.xRot = 0.0F;
        part.yRot = (float) Math.PI;
        part.zRot = (float) Math.PI;
    }

    private record PartState(float x, float y, float z, float xRot, float yRot, float zRot,
                             boolean visible, boolean skipDraw) {
        private static PartState capture(ModelPart part) {
            return new PartState(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                    part.visible, part.skipDraw);
        }

        private void restore(ModelPart part) {
            part.setPos(x, y, z);
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
            part.visible = visible;
            part.skipDraw = skipDraw;
        }
    }

    private HbmPlayerArmRenderer() {
    }
}
