package com.hbm.client.weapon.render;

import com.hbm.item.HbmGunItem;
import com.hbm.registry.HbmDataComponents;
import com.hbm.weapon.state.GunState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * HBM asset renderer using the bone-dispatch architecture of Superb Warfare SimpleGunRenderer
 * at commit 9b5284f42ef79532e6fb7f03ab07425c693b0b43 (GPL-3.0).
 */
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
                SuperbGunRig.find(gun.modelResource()).ifPresent(rig ->
                        SuperbGunPresentationState.applyFirstPerson(
                                poseStack, rig, displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND));
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
        SuperbGunRig rig = SuperbGunRig.find(gun.modelResource()).orElse(null);
        if (rig != null) {
            SuperbGunRig.VirtualBone virtualBone = rig.virtualBone(bone.getName()).orElse(null);
            if (virtualBone != null) {
                if (virtualBone.role() == SuperbGunRig.BoneRole.MUZZLE_FLASH) {
                    HbmMuzzleFlashRenderer.render(poseStack, buffers, packedLight, bone,
                            virtualBone.pivot(), virtualBone.effectSize(), renderPerspective);
                } else if (renderPerspective.firstPerson()) {
                    HbmPlayerArmRenderer.render(poseStack, buffers, packedLight, bone,
                            virtualBone.role() == SuperbGunRig.BoneRole.LEFT_HAND, renderType);
                }
                return;
            }
            if ("Slide".equals(bone.getName())
                    && com.hbm.client.weapon.ClientWeaponController.reloadIdle()) {
                bone.setPosZ(shouldHoldSlideOpen(currentItemStack)
                        ? -1.0F : -0.92F * SuperbGunPresentationState.slideTravel());
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
        return state != null && state.ammoCount() == 0
                && com.hbm.client.weapon.ClientWeaponController.reloadIdle();
    }
}
