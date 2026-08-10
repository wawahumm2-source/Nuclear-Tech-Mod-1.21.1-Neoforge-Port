package com.hbm.world.radiation;

/** Pure source values for the six public Sellafite tiers. */
public final class SellafieldMath {
    private static final double[] ITEM_RADIATION = {0.5D, 1D, 2.5D, 4D, 5D, 10D};

    private SellafieldMath() {
    }

    public static double itemRadiation(int level) {
        return ITEM_RADIATION[Math.clamp(level, 0, ITEM_RADIATION.length - 1)];
    }

    /** Exact BlockSellafieldSlaked brightness curve for metadata stages 0 through 9. */
    public static int slakedColor(int stage) {
        int channel = Math.clamp(Math.round(255F * (1F - Math.clamp(stage, 0, 9) / 15F)), 0, 255);
        return channel << 16 | channel << 8 | channel;
    }
}
