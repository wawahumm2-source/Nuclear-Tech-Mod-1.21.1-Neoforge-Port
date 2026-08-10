package com.hbm.world.explosion;

/** Pure geometry used by the bounded nuclear water-vaporization job. */
final class HbmWaterVaporizationMath {
    private HbmWaterVaporizationMath() {
    }

    static int surveyRadius(int fullRadius, int sentinelRingBlocks) {
        return Math.max(0, fullRadius) + Math.max(1, sentinelRingBlocks);
    }

    static boolean shouldEvaporateComponent(boolean touchesBoundaryOrPersistent, int insideVolume,
            int maximumContainedVolume) {
        return !touchesBoundaryOrPersistent
                && insideVolume > 0
                && insideVolume <= Math.max(0, maximumContainedVolume);
    }

    static boolean canRefillFromOpening(boolean insideCrater, boolean horizontalAirOpening,
            boolean airPathReachesCrater) {
        return insideCrater && horizontalAirOpening && airPathReachesCrater;
    }

}
