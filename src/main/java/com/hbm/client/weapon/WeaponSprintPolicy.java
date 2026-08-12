package com.hbm.client.weapon;

/** Pure client movement-intent rules shared by weapon transition tests. */
public final class WeaponSprintPolicy {
    public static boolean shouldUseSprintFireTransition(boolean wasSprinting,
                                                        boolean adsActive) {
        return wasSprinting && !adsActive;
    }

    public static boolean shouldRestartAfterAds(boolean adsReleased,
                                                boolean inputAllowed,
                                                boolean sprintKeyHeld,
                                                boolean movingForward,
                                                boolean actionPlaying,
                                                boolean sprintFireActive) {
        return adsReleased
                && inputAllowed
                && sprintKeyHeld
                && movingForward
                && !actionPlaying
                && !sprintFireActive;
    }

    private WeaponSprintPolicy() {
    }
}
