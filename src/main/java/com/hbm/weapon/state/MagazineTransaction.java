package com.hbm.weapon.state;

import com.hbm.weapon.data.GunDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Pure magazine mutation used after an ammo source has completed an extraction. Keeping this logic
 * separate makes capacity, mixed-ammunition, and chamber rules independently testable.
 */
public final class MagazineTransaction {
    public static Result load(GunState state, GunDefinition definition, ResourceLocation ammoId, int extractedRounds) {
        if (extractedRounds < 0) {
            throw new IllegalArgumentException("Extracted ammunition cannot be negative");
        }
        if (!definition.getSupportedAmmo().contains(ammoId)) {
            throw new IllegalArgumentException("Unsupported ammunition " + ammoId + " for " + definition.getId());
        }
        if (state.ammoCount() > 0 && !state.loadedAmmoId().equals(ammoId)) {
            throw new IllegalArgumentException("Cannot mix " + ammoId + " with loaded " + state.loadedAmmoId());
        }

        int room = Math.max(0, definition.getMagazine().getCapacity() - state.ammoCount());
        int accepted = Math.min(room, extractedRounds);
        if (accepted == 0) {
            return new Result(state, 0);
        }
        int count = state.ammoCount() + accepted;
        return new Result(
                state.withMagazine(ammoId, count,
                        definition.getMagazine().getUsesChamber() && count > 0),
                accepted
        );
    }

    public record Result(GunState state, int acceptedRounds) {
    }

    private MagazineTransaction() {
    }
}
