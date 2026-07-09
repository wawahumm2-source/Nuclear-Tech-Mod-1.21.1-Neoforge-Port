package com.hbm.item;

import com.hbm.world.radiation.RadiationEmitter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class RadioactiveBlockItem extends BlockItem implements RadiationEmitter {
    private final double radiationDose;

    public RadioactiveBlockItem(Block block, Properties properties, double radiationDose) {
        super(block, properties);
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
