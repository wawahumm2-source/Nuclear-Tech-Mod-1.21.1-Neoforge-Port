package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.recipe.BurnerPressRecipe;
import com.hbm.recipe.BurnerPressRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, HbmNuclearTech.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, HbmNuclearTech.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BurnerPressRecipe>> BURNER_PRESS_TYPE =
            RECIPE_TYPES.register("burner_press", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "burner_press")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BurnerPressRecipe>> BURNER_PRESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("burner_press", BurnerPressRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    private HbmRecipes() {
    }
}
