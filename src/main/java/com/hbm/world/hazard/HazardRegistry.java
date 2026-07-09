package com.hbm.world.hazard;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class HazardRegistry {
    private static final Map<Item, EnumMap<HazardType, Double>> ITEM_HAZARDS = new IdentityHashMap<>();
    private static final Map<Block, EnumMap<HazardType, Double>> BLOCK_HAZARDS = new IdentityHashMap<>();

    public static void registerItem(Item item, HazardData hazard) {
        register(ITEM_HAZARDS, item, hazard);
    }

    public static void registerBlock(Block block, HazardData hazard) {
        register(BLOCK_HAZARDS, block, hazard);
    }

    public static double get(ItemStack stack, HazardType type) {
        if (stack.isEmpty()) {
            return 0D;
        }

        double itemHazard = get(ITEM_HAZARDS, stack.getItem(), type);
        double blockHazard = stack.getItem() instanceof BlockItem blockItem ? get(BLOCK_HAZARDS, blockItem.getBlock(), type) : 0D;
        return Math.max(itemHazard, blockHazard) * stack.getCount();
    }

    public static double get(BlockState state, HazardType type) {
        return get(BLOCK_HAZARDS, state.getBlock(), type);
    }

    public static double getRadiation(ItemStack stack) {
        return get(stack, HazardType.RADIATION);
    }

    public static double getRadiation(BlockState state) {
        return get(state, HazardType.RADIATION);
    }

    private static <T> void register(Map<T, EnumMap<HazardType, Double>> registry, T key, HazardData hazard) {
        registry.computeIfAbsent(key, ignored -> new EnumMap<>(HazardType.class)).put(hazard.type(), hazard.strength());
    }

    private static <T> double get(Map<T, EnumMap<HazardType, Double>> registry, T key, HazardType type) {
        EnumMap<HazardType, Double> hazards = registry.get(key);
        return hazards == null ? 0D : hazards.getOrDefault(type, 0D);
    }

    private HazardRegistry() {
    }
}
