package com.hbm.client.render;

import com.hbm.registry.HbmBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class BurnerPressItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float ITEM_SCALE = 0.28F;
    private static final float HEAD_IDLE_LIFT = 0.55F;

    private final BlockRenderDispatcher blockRenderer;

    public BurnerPressItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BlockState state = HbmBlocks.BURNER_PRESS.get().defaultBlockState();
        BakedModel body = modelManager.getModel(BurnerPressRenderer.BODY_MODEL);
        BakedModel head = modelManager.getModel(BurnerPressRenderer.HEAD_MODEL);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.08D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(22.5F));
        poseStack.mulPose(Axis.YP.rotationDegrees(35.0F));
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        renderModel(body, modelManager, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.translate(0.0D, HEAD_IDLE_LIFT, 0.0D);
        renderModel(head, modelManager, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderModel(BakedModel model, ModelManager modelManager, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (model == modelManager.getMissingModel()) {
            return;
        }

        for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY)) {
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(renderType, false));
            this.blockRenderer.getModelRenderer().renderModel(
                    poseStack.last(),
                    consumer,
                    state,
                    model,
                    1.0F,
                    1.0F,
                    1.0F,
                    packedLight,
                    packedOverlay,
                    ModelData.EMPTY,
                    renderType);
        }
    }
}
