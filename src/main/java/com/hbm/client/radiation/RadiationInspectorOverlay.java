package com.hbm.client.radiation;

import com.hbm.registry.HbmItems;
import com.hbm.world.radiation.RadiationDiagnostics;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class RadiationInspectorOverlay {
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.getMainHandItem().is(HbmItems.RADIATION_INSPECTOR.get())) {
            return;
        }

        RadiationDiagnostics diagnostics = RadiationClientState.diagnostics();
        List<Component> lines = List.of(
                Component.literal("HBM Radiation Inspector"),
                Component.literal(String.format("Dose: %.2f RAD", diagnostics.accumulatedRadiation())),
                Component.literal(String.format("Rate: %.3f RAD/s", diagnostics.totalRate())),
                Component.literal(String.format("Inventory %.3f  Block %.3f", diagnostics.inventoryRate(), diagnostics.blockRate())),
                Component.literal(String.format("Fallout %.3f  Explosion %.3f", diagnostics.falloutRate(), diagnostics.explosionRate())),
                Component.literal(String.format("Last explosion %.2f RAD", diagnostics.lastExplosionDose())),
                Component.literal(String.format("Resistance %.3f  Intake %.1f%%", diagnostics.resistance(), diagnostics.protectionMultiplier() * 100D))
        );

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 6;
        int y = 6;
        int width = 176;
        int height = lines.size() * 10 + 8;
        graphics.fill(x - 3, y - 3, x + width, y + height, 0xC0101010);
        for (int index = 0; index < lines.size(); index++) {
            int color = index == 0 ? 0xFFFFFF55 : 0xFFE0E0E0;
            graphics.drawString(minecraft.font, lines.get(index), x, y + index * 10, color, false);
        }
    }

    private RadiationInspectorOverlay() {
    }
}
