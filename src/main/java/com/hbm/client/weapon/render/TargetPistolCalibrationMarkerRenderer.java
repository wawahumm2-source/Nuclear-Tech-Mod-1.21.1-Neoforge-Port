package com.hbm.client.weapon.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

/** Colored model/hand anchors shown only while the temporary calibration HUD is visible. */
final class TargetPistolCalibrationMarkerRenderer {
    private static final Vec3 HBM_RIGHT_GRIP = new Vec3(0.10D, -1.80D, 3.10D);
    private static final Vec3 HBM_LEFT_GRIP = new Vec3(-0.32D, -1.20D, 2.35D);
    private static final Vec3 HBM_MUZZLE = new Vec3(0.04D, 2.45D, 6.18D);

    static void renderModelAnchors(PoseStack poseStack, MultiBufferSource buffers,
                                   GeoBone modelSpace) {
        renderModelAnchor(poseStack, buffers, modelSpace, HBM_RIGHT_GRIP, 255, 65, 65);
        renderModelAnchor(poseStack, buffers, modelSpace, HBM_LEFT_GRIP, 70, 135, 255);
        renderModelAnchor(poseStack, buffers, modelSpace, HBM_MUZZLE, 70, 255, 95);
    }

    static void renderFixedHandAnchor(PoseStack poseStack, MultiBufferSource buffers,
                                      GeoBone hand, boolean left) {
        poseStack.pushPose();
        try {
            // A virtual hand bone has no geometry. Its pivot is the stable MP-443 anchor in its
            // parent controller space; show that point without inheriting model_space calibration.
            poseStack.translate(hand.getPivotX() / 16.0D,
                    hand.getPivotY() / 16.0D, hand.getPivotZ() / 16.0D);
            renderCross(poseStack, buffers, left ? 65 : 255, 255, left ? 255 : 65);
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderModelAnchor(PoseStack poseStack, MultiBufferSource buffers,
                                          GeoBone modelSpace, Vec3 source,
                                          int red, int green, int blue) {
        poseStack.pushPose();
        try {
            RenderUtil.prepMatrixForBone(poseStack, modelSpace);
            // OBJ vertices are baked into Gecko space as (-x, y, z) / 16.
            poseStack.translate(-source.x / 16.0D, source.y / 16.0D, source.z / 16.0D);
            renderCross(poseStack, buffers, red, green, blue);
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderCross(PoseStack poseStack, MultiBufferSource buffers,
                                    int red, int green, int blue) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        float size = 0.035F;
        line(consumer, pose, -size, 0.0F, 0.0F, size, 0.0F, 0.0F,
                red, green, blue, 1.0F, 0.0F, 0.0F);
        line(consumer, pose, 0.0F, -size, 0.0F, 0.0F, size, 0.0F,
                red, green, blue, 0.0F, 1.0F, 0.0F);
        line(consumer, pose, 0.0F, 0.0F, -size, 0.0F, 0.0F, size,
                red, green, blue, 0.0F, 0.0F, 1.0F);
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             int red, int green, int blue,
                             float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose.pose(), x1, y1, z1)
                .setColor(red, green, blue, 255)
                .setNormal(pose, normalX, normalY, normalZ);
        consumer.addVertex(pose.pose(), x2, y2, z2)
                .setColor(red, green, blue, 255)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private TargetPistolCalibrationMarkerRenderer() {
    }
}
