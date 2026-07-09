package com.hbm.compat.jei;

import com.hbm.HbmNuclearTech;
import com.hbm.item.StampType;
import com.hbm.recipe.BurnerPressRecipe;
import com.hbm.registry.HbmItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class BurnerPressCategory implements IRecipeCategory<BurnerPressRecipe> {
    public static final RecipeType<BurnerPressRecipe> RECIPE_TYPE =
            RecipeType.create(HbmNuclearTech.MOD_ID, "burner_press", BurnerPressRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public BurnerPressCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(
                        ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "textures/gui/nei/gui_nei_press.png"),
                        0,
                        0,
                        116,
                        54)
                .setTextureSize(256, 256)
                .build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(HbmItems.BURNER_PRESS.get()));
    }

    @Override
    public RecipeType<BurnerPressRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm.burner_press");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BurnerPressRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 19)
                .addIngredients(recipe.getIngredient());
        builder.addSlot(RecipeIngredientRole.INPUT, 47, 19)
                .addItemStack(getStampStack(recipe.getStampType()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 87, 19)
                .addItemStack(recipe.getResult().copy());
    }

    private static ItemStack getStampStack(StampType stampType) {
        return switch (stampType) {
            case FLAT -> HbmItems.STAMP_FLAT.get().getDefaultInstance();
            case PLATE -> HbmItems.STAMP_PLATE.get().getDefaultInstance();
            case WIRE -> HbmItems.STAMP_WIRE.get().getDefaultInstance();
            case CIRCUIT -> HbmItems.STAMP_CIRCUIT.get().getDefaultInstance();
            case C357 -> HbmItems.STAMP_357.get().getDefaultInstance();
            case C44 -> HbmItems.STAMP_44.get().getDefaultInstance();
            case C50 -> HbmItems.STAMP_50.get().getDefaultInstance();
            case C9 -> HbmItems.STAMP_9.get().getDefaultInstance();
            case PRINTING1 -> HbmItems.STAMP_PRINTING_1.get().getDefaultInstance();
            case PRINTING2 -> HbmItems.STAMP_PRINTING_2.get().getDefaultInstance();
            case PRINTING3 -> HbmItems.STAMP_PRINTING_3.get().getDefaultInstance();
            case PRINTING4 -> HbmItems.STAMP_PRINTING_4.get().getDefaultInstance();
            case PRINTING5 -> HbmItems.STAMP_PRINTING_5.get().getDefaultInstance();
            case PRINTING6 -> HbmItems.STAMP_PRINTING_6.get().getDefaultInstance();
            case PRINTING7 -> HbmItems.STAMP_PRINTING_7.get().getDefaultInstance();
            case PRINTING8 -> HbmItems.STAMP_PRINTING_8.get().getDefaultInstance();
        };
    }
}
