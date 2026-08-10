package com.hbm.client.screen;

import com.hbm.client.gui.BurnerPressGuiLayout;
import com.hbm.menu.BurnerPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BurnerPressScreen extends AbstractContainerScreen<BurnerPressMenu> {
    private static final int INVENTORY_SOURCE_Y = 108;
    private static final int INVENTORY_TARGET_Y = 90;
    private static final int COMPACTED_HEIGHT = 184;
    private static final int MAX_SPEED = 400;

    public BurnerPressScreen(BurnerPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BurnerPressGuiLayout.WIDTH;
        this.imageHeight = COMPACTED_HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = INVENTORY_TARGET_Y;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderMachineTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        drawCompactBackground(guiGraphics, left, top);
        drawFuelFlame(guiGraphics, left + 27, top + 36 + BurnerPressGuiLayout.MACHINE_OFFSET_Y);
        drawSpeedNeedle(guiGraphics, left + 34, top + 25 + BurnerPressGuiLayout.MACHINE_OFFSET_Y);

        int pressHeight = this.menu.getScaledProgress(16);
        if (pressHeight > 0) {
            guiGraphics.blit(BurnerPressGuiLayout.TEXTURE, left + 79, top + 35 + BurnerPressGuiLayout.MACHINE_OFFSET_Y, 14, 202, 18, pressHeight);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleX = (this.imageWidth - this.font.width(this.title)) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    private void drawCompactBackground(GuiGraphics guiGraphics, int left, int top) {
        BurnerPressGuiLayout.drawMachinePanel(guiGraphics, left, top);
        guiGraphics.fill(left, top + BurnerPressGuiLayout.MACHINE_PANEL_HEIGHT, left + this.imageWidth, top + INVENTORY_TARGET_Y, 0xFFC6C6C6);
        // Preserve the source GUI's side frame through the compacted spacer.
        guiGraphics.blit(BurnerPressGuiLayout.TEXTURE, left, top + BurnerPressGuiLayout.MACHINE_PANEL_HEIGHT, 0, BurnerPressGuiLayout.MACHINE_PANEL_HEIGHT, 3, INVENTORY_TARGET_Y - BurnerPressGuiLayout.MACHINE_PANEL_HEIGHT);
        guiGraphics.blit(BurnerPressGuiLayout.TEXTURE, left + this.imageWidth - 5, top + BurnerPressGuiLayout.MACHINE_PANEL_HEIGHT, this.imageWidth - 5, BurnerPressGuiLayout.MACHINE_PANEL_HEIGHT, 5, INVENTORY_TARGET_Y - BurnerPressGuiLayout.MACHINE_PANEL_HEIGHT);
        guiGraphics.blit(BurnerPressGuiLayout.TEXTURE, left, top + INVENTORY_TARGET_Y, 0, INVENTORY_SOURCE_Y, this.imageWidth, this.imageHeight - INVENTORY_TARGET_Y);
    }

    private void drawFuelFlame(GuiGraphics guiGraphics, int x, int y) {
        BurnerPressGuiLayout.drawFuelFlame(guiGraphics, x, y, this.menu.getScaledFuelReserve(14));
    }

    private void drawSpeedNeedle(GuiGraphics guiGraphics, int centerX, int centerY) {
        int speed = this.menu.getSpeed();
        BurnerPressGuiLayout.drawGaugeNeedle(guiGraphics, centerX, centerY, speed / (double) MAX_SPEED);
    }

    private void renderMachineTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        if (isInside(localX, localY, 25, 16 + BurnerPressGuiLayout.MACHINE_OFFSET_Y, 18, 18)) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getSpeedPercent() + "%"), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
