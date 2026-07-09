package com.hbm.recipe;

import com.hbm.config.HbmConfig;
import com.hbm.item.StampItem;
import com.hbm.item.StampType;
import com.hbm.registry.HbmRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class BurnerPressRecipe implements Recipe<BurnerPressRecipeInput> {
    private final Ingredient ingredient;
    private final StampType stampType;
    private final ItemStack result;
    private final int processingTicks;

    public BurnerPressRecipe(Ingredient ingredient, StampType stampType, ItemStack result, int processingTicks) {
        this.ingredient = ingredient;
        this.stampType = stampType;
        this.result = result;
        this.processingTicks = processingTicks;
    }

    @Override
    public boolean matches(BurnerPressRecipeInput input, Level level) {
        return this.ingredient.test(input.input()) && StampItem.hasType(input.stamp(), this.stampType);
    }

    @Override
    public ItemStack assemble(BurnerPressRecipeInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.ingredient);
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipes.BURNER_PRESS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipes.BURNER_PRESS_TYPE.get();
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }

    public StampType getStampType() {
        return this.stampType;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public int getRawProcessingTicks() {
        return this.processingTicks;
    }

    public int getProcessingTicks() {
        return this.processingTicks > 0 ? this.processingTicks : HbmConfig.BURNER_PRESS_PROCESS_TICKS.get();
    }
}
