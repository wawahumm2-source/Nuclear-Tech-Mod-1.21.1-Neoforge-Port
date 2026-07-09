package com.hbm.client.screen;

import com.hbm.HbmNuclearTech;
import com.hbm.menu.BurnerPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BurnerPressScreen extends AbstractContainerScreen<BurnerPressMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "textures/gui/gui_press.png");

    public BurnerPressScreen(BurnerPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 108;
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
        guiGraphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.fill(left + 8, top + 84, left + 170, top + 104, 0xFFC6C6C6);
        if (this.menu.getBurnTime() >= 20) {
            guiGraphics.blit(TEXTURE, left + 27, top + 36, 0, 202, 14, 14);
        }
        drawSpeedNeedle(guiGraphics, left + 34, top + 25);

        int pressHeight = this.menu.getScaledProgress(16);
        if (pressHeight > 0) {
            guiGraphics.blit(TEXTURE, left + 79, top + 35, 14, 202, 18, pressHeight);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleX = (this.imageWidth - this.font.width(this.title)) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    private void drawSpeedNeedle(GuiGraphics guiGraphics, int centerX, int centerY) {
        int speed = this.menu.getScaledSpeed(10);
        if (speed <= 0) {
            return;
        }

        double angle = Math.toRadians(225D + speed * 18D);
        int endX = centerX + (int) Math.round(Math.cos(angle) * 5D);
        int endY = centerY + (int) Math.round(Math.sin(angle) * 5D);
        drawLine(guiGraphics, centerX, centerY, endX, endY, 0xFF7F0000);
    }

    private void drawLine(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY, int color) {
        int deltaX = Math.abs(endX - startX);
        int deltaY = Math.abs(endY - startY);
        int stepX = startX < endX ? 1 : -1;
        int stepY = startY < endY ? 1 : -1;
        int error = deltaX - deltaY;
        int x = startX;
        int y = startY;

        while (true) {
            guiGraphics.fill(x, y, x + 1, y + 1, color);
            if (x == endX && y == endY) {
                return;
            }
            int doubledError = error * 2;
            if (doubledError > -deltaY) {
                error -= deltaY;
                x += stepX;
            }
            if (doubledError < deltaX) {
                error += deltaX;
                y += stepY;
            }
        }
    }

    private void renderMachineTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        if (isInside(localX, localY, 25, 16, 18, 18)) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getSpeedPercent() + "%"), mouseX, mouseY);
        } else if (isInside(localX, localY, 25, 34, 18, 18)) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getStoredOperations() + " operations left"), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
