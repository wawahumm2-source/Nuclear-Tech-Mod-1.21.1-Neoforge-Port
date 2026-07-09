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
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;

    public BurnerPressScreen(BurnerPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 90;
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
        drawMachinePanel(guiGraphics, left, top);
        drawFuelFlame(guiGraphics, left + 27, top + 36);
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

    private void drawCompactBackground(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
        guiGraphics.fill(left, top, left + this.imageWidth, top + 1, PANEL_LIGHT);
        guiGraphics.fill(left, top, left + 1, top + this.imageHeight, PANEL_LIGHT);
        guiGraphics.fill(left + this.imageWidth - 1, top, left + this.imageWidth, top + this.imageHeight, PANEL_DARK);
        guiGraphics.fill(left, top + this.imageHeight - 1, left + this.imageWidth, top + this.imageHeight, PANEL_DARK);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiGraphics, left + 7 + column * 18, top + 101 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(guiGraphics, left + 7 + column * 18, top + 159);
        }
    }

    private void drawMachinePanel(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.blit(TEXTURE, left + 24, top + 15, 24, 15, 21, 21);
        drawSlot(guiGraphics, left + 25, top + 52);
        guiGraphics.blit(TEXTURE, left + 27, top + 36, 27, 36, 14, 14);

        guiGraphics.blit(TEXTURE, left + 78, top + 16, 78, 16, 22, 58);
        drawSlot(guiGraphics, left + 79, top + 16);
        drawSlot(guiGraphics, left + 79, top + 52);

        guiGraphics.blit(TEXTURE, left + 106, top + 37, 106, 37, 31, 17);
        drawSlot(guiGraphics, left + 139, top + 34);
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, PANEL_DARK);
        guiGraphics.fill(x + 1, y + 1, x + 18, y + 2, PANEL_LIGHT);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + 18, PANEL_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + 17, y + 17, 0xFF8B8B8B);
    }

    private void drawFuelFlame(GuiGraphics guiGraphics, int x, int y) {
        int flameHeight = this.menu.getScaledBurnTime(14);
        if (flameHeight <= 0) {
            return;
        }
        int topCrop = 14 - flameHeight;
        guiGraphics.blit(TEXTURE, x, y + topCrop, 0, 202 + topCrop, 14, flameHeight);
    }

    private void drawSpeedNeedle(GuiGraphics guiGraphics, int centerX, int centerY) {
        int speed = this.menu.getSpeed();
        if (speed <= 0) {
            return;
        }

        double progress = Math.min(1.0D, speed / 400.0D);
        double angle = Math.toRadians(135D + progress * 270D);
        int endX = centerX + (int) Math.round(Math.cos(angle) * 5D);
        int endY = centerY + (int) Math.round(Math.sin(angle) * 5D);
        drawLine(guiGraphics, centerX, centerY + 1, endX, endY + 1, 0xFF1F1F1F);
        drawLine(guiGraphics, centerX, centerY, endX, endY, 0xFF7F0000);
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0xFF2B2B2B);
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
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
