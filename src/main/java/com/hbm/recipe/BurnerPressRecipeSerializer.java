package com.hbm.recipe;

import com.hbm.item.StampType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class BurnerPressRecipeSerializer implements RecipeSerializer<BurnerPressRecipe> {
    public static final MapCodec<BurnerPressRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(BurnerPressRecipe::getIngredient),
            StampType.CODEC.fieldOf("stamp").forGetter(BurnerPressRecipe::getStampType),
            ItemStack.CODEC.fieldOf("result").forGetter(BurnerPressRecipe::getResult),
            Codec.INT.optionalFieldOf("processing_time", 0).forGetter(BurnerPressRecipe::getRawProcessingTicks)
    ).apply(instance, BurnerPressRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BurnerPressRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, BurnerPressRecipe::getIngredient,
            StampType.STREAM_CODEC, BurnerPressRecipe::getStampType,
            ItemStack.STREAM_CODEC, BurnerPressRecipe::getResult,
            ByteBufCodecs.VAR_INT, BurnerPressRecipe::getRawProcessingTicks,
            BurnerPressRecipe::new
    );

    @Override
    public MapCodec<BurnerPressRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, BurnerPressRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
