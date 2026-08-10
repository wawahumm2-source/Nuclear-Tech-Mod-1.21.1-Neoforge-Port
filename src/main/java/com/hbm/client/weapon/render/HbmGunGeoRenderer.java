package com.hbm.client.weapon.render;

import com.hbm.client.weapon.ClientWeaponController;
import com.hbm.item.HbmGunItem;
import com.hbm.registry.HbmDataComponents;
import com.hbm.weapon.state.GunState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class HbmGunGeoRenderer extends GeoItemRenderer<HbmGunItem> {
    public HbmGunGeoRenderer() {
        super(new HbmGunGeoModel());
        useAlternateGuiLighting();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        try {
            if (displayContext.firstPerson() && stack.getItem() instanceof HbmGunItem gun) {
                GunViewmodelProfile.find(gun.modelResource()).ifPresent(profile -> profile.apply(
                        poseStack,
                        ClientWeaponController.viewmodelAdsBlend(),
                        ClientWeaponController.viewmodelRecoilPitch(),
                        ClientWeaponController.viewmodelRecoilYaw(),
                        Minecraft.getInstance().level == null
                                ? 0.0F : Minecraft.getInstance().level.getGameTime()
                ));
            }
            super.renderByItem(stack, displayContext, poseStack, buffers, packedLight, packedOverlay);
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, HbmGunItem gun, GeoBone bone,
                                  RenderType renderType, MultiBufferSource buffers,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int renderColor) {
        GunViewmodelProfile profile = GunViewmodelProfile.find(gun.modelResource()).orElse(null);
        if (profile != null) {
            var arm = profile.arm(bone.getName());
            if (arm.isPresent()) {
                if (renderPerspective.firstPerson()) {
                    HbmPlayerArmRenderer.render(poseStack, buffers, packedLight, bone, arm.get().left());
                }
                return;
            }
            if ("Slide".equals(bone.getName()) && shouldHoldSlideOpen(currentItemStack)) {
                bone.setPosZ(-1.0F);
            }
        }
        super.renderRecursively(poseStack, gun, bone, renderType, buffers, buffer, isReRender,
                partialTick, packedLight, packedOverlay, renderColor);
    }

    private static boolean shouldHoldSlideOpen(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        GunState state = stack.get(HbmDataComponents.GUN_STATE.get());
        return state != null && state.ammoCount() == 0 && ClientWeaponController.reloadIdle();
    }
}
