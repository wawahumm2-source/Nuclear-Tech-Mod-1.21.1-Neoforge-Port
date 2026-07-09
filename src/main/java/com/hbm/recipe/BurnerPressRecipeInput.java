package com.hbm.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record BurnerPressRecipeInput(ItemStack input, ItemStack stamp) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> this.input;
            case 1 -> this.stamp;
            default -> throw new IllegalArgumentException("Burner Press recipe input index out of range: " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
