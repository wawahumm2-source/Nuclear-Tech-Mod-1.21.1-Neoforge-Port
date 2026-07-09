package com.hbm.client.render;

import com.hbm.HbmNuclearTech;
import com.hbm.blockentity.BurnerPressBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

public class BurnerPressRenderer implements BlockEntityRenderer<BurnerPressBlockEntity> {
    public static final ModelResourceLocation BODY_MODEL = standaloneModel("burner_press_body");
    public static final ModelResourceLocation HEAD_MODEL = standaloneModel("burner_press_head");
    private static final float HEAD_STROKE = 0.42F;

    private final BlockRenderDispatcher blockRenderer;

    public BurnerPressRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(BurnerPressBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel body = modelManager.getModel(BODY_MODEL);
        BakedModel head = modelManager.getModel(HEAD_MODEL);
        BlockState state = blockEntity.getBlockState();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        renderModel(body, modelManager, state, poseStack, bufferSource, packedLight, packedOverlay);

        float progress = net.minecraft.util.Mth.lerp(partialTick, blockEntity.getPreviousAnimationProgress(), blockEntity.getAnimationProgress());
        float headOffset = progress * HEAD_STROKE;
        poseStack.translate(0.0D, -headOffset, 0.0D);
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

    private static ModelResourceLocation standaloneModel(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "block/" + path));
    }
}
