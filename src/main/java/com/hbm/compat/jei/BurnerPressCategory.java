package com.hbm.compat.jei;

import com.hbm.HbmNuclearTech;
import com.hbm.client.gui.BurnerPressGuiLayout;
import com.hbm.item.StampType;
import com.hbm.recipe.BurnerPressRecipe;
import com.hbm.registry.HbmItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.List;

public class BurnerPressCategory implements IRecipeCategory<BurnerPressRecipe> {
    public static final RecipeType<BurnerPressRecipe> RECIPE_TYPE =
            RecipeType.create(HbmNuclearTech.MOD_ID, "burner_press", BurnerPressRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final List<ItemStack> fuels;

    public BurnerPressCategory(IGuiHelper guiHelper) {
        this.background = new PressRecipeDrawable(
                guiHelper.createAnimatedRecipeFlame(200),
                guiHelper.createAnimatedRecipeArrow(200));
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(HbmItems.BURNER_PRESS.get()));
        this.fuels = BuiltInRegistries.ITEM.stream()
                .map(item -> item.getDefaultInstance())
                .filter(AbstractFurnaceBlockEntity::isFuel)
                .toList();
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
        builder.addSlot(RecipeIngredientRole.INPUT, BurnerPressGuiLayout.FUEL_X, BurnerPressGuiLayout.FUEL_Y)
                .addItemStacks(this.fuels);
        builder.addSlot(RecipeIngredientRole.INPUT, BurnerPressGuiLayout.INPUT_X, BurnerPressGuiLayout.INPUT_Y)
                .addIngredients(recipe.getIngredient());
        builder.addSlot(RecipeIngredientRole.INPUT, BurnerPressGuiLayout.STAMP_X, BurnerPressGuiLayout.STAMP_Y)
                .addItemStack(getStampStack(recipe.getStampType()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, BurnerPressGuiLayout.OUTPUT_X, BurnerPressGuiLayout.OUTPUT_Y)
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

    private static final class PressRecipeDrawable implements IDrawable {
        private final IDrawableAnimated flame;
        private final IDrawableAnimated arrow;

        private PressRecipeDrawable(IDrawableAnimated flame, IDrawableAnimated arrow) {
            this.flame = flame;
            this.arrow = arrow;
        }

        @Override
        public int getWidth() {
            return BurnerPressGuiLayout.WIDTH;
        }

        @Override
        public int getHeight() {
            return BurnerPressGuiLayout.MACHINE_PANEL_HEIGHT;
        }

        @Override
        public void draw(net.minecraft.client.gui.GuiGraphics guiGraphics, int xOffset, int yOffset) {
            BurnerPressGuiLayout.drawMachinePanel(guiGraphics, xOffset, yOffset);
            this.flame.draw(guiGraphics, xOffset + 27, yOffset + 36 + BurnerPressGuiLayout.MACHINE_OFFSET_Y);
            BurnerPressGuiLayout.drawGaugeNeedle(guiGraphics, xOffset + 34, yOffset + 25 + BurnerPressGuiLayout.MACHINE_OFFSET_Y, 0.60D);
            BurnerPressGuiLayout.clearArrowLane(guiGraphics, xOffset, yOffset);
            this.arrow.draw(guiGraphics, xOffset + 101, yOffset + 37 + BurnerPressGuiLayout.MACHINE_OFFSET_Y);
        }
    }
}
