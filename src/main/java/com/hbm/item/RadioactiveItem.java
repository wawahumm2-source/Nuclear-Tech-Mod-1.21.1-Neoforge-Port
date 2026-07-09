package com.hbm.item;

import com.hbm.world.radiation.RadiationEmitter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class RadioactiveItem extends Item implements RadiationEmitter {
    private final double radiationDose;

    public RadioactiveItem(Properties properties, double radiationDose) {
        super(properties);
        this.radiationDose = radiationDose;
    }

    @Override
    public double hbm$getRadiationDose() {
        return this.radiationDose;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.radioactive.tooltip", this.radiationDose).withStyle(ChatFormatting.YELLOW));
    }
}
