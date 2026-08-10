package com.hbm.world.radiation;

import com.hbm.config.HbmConfig;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Source-shaped resistance registry. One resistance point reduces intake to one tenth. */
public final class RadiationProtectionRegistry {
    private static final Map<Item, Double> RESISTANCE = new IdentityHashMap<>();

    public static void bootstrap() {
        if (!RESISTANCE.isEmpty()) {
            return;
        }

        // Tier 1 HazmatRegistry: iron has 0.0225 total resistance, split by armor slot.
        register(Items.IRON_HELMET, 0.0225D * 0.2D);
        register(Items.IRON_CHESTPLATE, 0.0225D * 0.4D);
        register(Items.IRON_LEGGINGS, 0.0225D * 0.3D);
        register(Items.IRON_BOOTS, 0.0225D * 0.1D);

        register(Items.GOLDEN_HELMET, 0.0225D * 0.2D);
        register(Items.GOLDEN_CHESTPLATE, 0.0225D * 0.4D);
        register(Items.GOLDEN_LEGGINGS, 0.0225D * 0.3D);
        register(Items.GOLDEN_BOOTS, 0.0225D * 0.1D);
    }

    public static void register(Item item, double resistance) {
        RESISTANCE.put(item, Math.max(0D, resistance));
    }

    public static double getResistance(Player player, RadiationPlayerState state) {
        bootstrap();
        double resistance = state.hasRadX() ? HbmConfig.RADIATION.radXResistance.get() : 0D;
        for (ItemStack stack : player.getArmorSlots()) {
            resistance += getResistance(stack);
        }
        return resistance;
    }

    public static double getResistance(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0D;
        }
        double taggedResistance = stack.is(HbmTags.Items.RADIATION_SHIELDING)
                ? HbmConfig.RADIATION.tagShieldingResistance.get()
                : 0D;
        return Math.max(taggedResistance, RESISTANCE.getOrDefault(stack.getItem(), 0D));
    }

    private RadiationProtectionRegistry() {
    }
}
