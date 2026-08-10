package com.hbm.client.gui;

import com.hbm.HbmNuclearTech;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class BurnerPressGuiLayout {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, "textures/gui/gui_press.png");
    public static final int WIDTH = 176;
    public static final int MACHINE_PANEL_HEIGHT = 83;
    public static final int MACHINE_OFFSET_Y = 4;
    public static final int FUEL_X = 26;
    public static final int FUEL_Y = 53 + MACHINE_OFFSET_Y;
    public static final int STAMP_X = 80;
    public static final int STAMP_Y = 17 + MACHINE_OFFSET_Y;
    public static final int INPUT_X = 80;
    public static final int INPUT_Y = 53 + MACHINE_OFFSET_Y;
    public static final int OUTPUT_X = 140;
    public static final int OUTPUT_Y = 35 + MACHINE_OFFSET_Y;

    private static final int BACKGROUND_COLOR = 0xFFC6C6C6;

    private BurnerPressGuiLayout() {
    }

    public static void drawMachinePanel(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.blit(TEXTURE, left, top, 0, 0, WIDTH, MACHINE_PANEL_HEIGHT);
        moveArtworkRegion(guiGraphics, left, top, 23, 15, 22, 60);
        moveArtworkRegion(guiGraphics, left, top, 76, 15, 25, 60);

        // The arrow and output frame are separate source elements. Moving them as one crop corrupts their edges.
        guiGraphics.fill(left + 100, top + 28, left + 163, top + 62, BACKGROUND_COLOR);
        copyArtworkRegion(guiGraphics, left, top, 101, 35, 30, 21);
        copyArtworkRegion(guiGraphics, left, top, 134, 30, 27, 26);
    }

    public static void drawFuelFlame(GuiGraphics guiGraphics, int x, int y, int flameHeight) {
        if (flameHeight <= 0) {
            return;
        }
        int clampedHeight = Math.min(14, flameHeight);
        int topCrop = 14 - clampedHeight;
        guiGraphics.blit(TEXTURE, x, y + topCrop, 0, 202 + topCrop, 14, clampedHeight);
    }

    public static void drawGaugeNeedle(GuiGraphics guiGraphics, int centerX, int centerY, double progress) {
        double clampedProgress = Math.min(1.0D, Math.max(0.0D, progress));
        double angle = Math.toRadians(135D + clampedProgress * 270D);
        int endX = centerX + (int) Math.round(Math.cos(angle) * 5D);
        int endY = centerY + (int) Math.round(Math.sin(angle) * 5D);
        drawLine(guiGraphics, centerX, centerY + 1, endX, endY + 1, 0xFF1F1F1F);
        drawLine(guiGraphics, centerX, centerY, endX, endY, 0xFF7F0000);
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0xFF2B2B2B);
    }

    public static void clearArrowLane(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.fill(left + 100, top + 35 + MACHINE_OFFSET_Y, left + 134, top + 56 + MACHINE_OFFSET_Y, BACKGROUND_COLOR);
    }

    private static void moveArtworkRegion(GuiGraphics guiGraphics, int left, int top, int sourceX, int sourceY, int width, int height) {
        guiGraphics.fill(left + sourceX, top + sourceY, left + sourceX + width, top + sourceY + height, BACKGROUND_COLOR);
        copyArtworkRegion(guiGraphics, left, top, sourceX, sourceY, width, height);
    }

    private static void copyArtworkRegion(GuiGraphics guiGraphics, int left, int top, int sourceX, int sourceY, int width, int height) {
        guiGraphics.blit(TEXTURE, left + sourceX, top + sourceY + MACHINE_OFFSET_Y, sourceX, sourceY, width, height);
    }

    private static void drawLine(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY, int color) {
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

}
