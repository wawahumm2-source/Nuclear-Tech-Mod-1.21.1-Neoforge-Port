package com.hbm.blockentity.machine;

public final class BurnerPressTiming {
    public static final int MAX_SPEED = 400;
    public static final int PROGRESS_AT_MAX_SPEED = 25;
    public static final int BURN_RATE_PER_TICK = 4;
    public static final int COOL_RATE = 1;
    public static final int HEAT_TICK_INTERVAL = 4;
    public static final int MINIMUM_WORKING_SPEED = MAX_SPEED / PROGRESS_AT_MAX_SPEED;

    private BurnerPressTiming() {
    }

    public static boolean canRunRecipe(boolean fuelBurning, int speed) {
        return fuelBurning || speed >= MINIMUM_WORKING_SPEED;
    }

    public static int advanceSpeed(int speed, boolean fuelBurning, boolean preheated, long gameTime) {
        if (!fuelBurning) {
            return Math.max(0, speed - COOL_RATE);
        }
        if (gameTime % HEAT_TICK_INTERVAL != 0) {
            return speed;
        }
        return Math.min(MAX_SPEED, speed + (preheated ? 4 : 1));
    }

    public static int burnFuel(int burnTime, boolean fuelBurning) {
        return fuelBurning ? Math.max(0, burnTime - BURN_RATE_PER_TICK) : burnTime;
    }

    public static int getStampSpeed(int speed) {
        return speed * PROGRESS_AT_MAX_SPEED / MAX_SPEED;
    }

    public static int getRetractionSpeed(int speed) {
        return Math.max(1, getStampSpeed(speed));
    }
}
