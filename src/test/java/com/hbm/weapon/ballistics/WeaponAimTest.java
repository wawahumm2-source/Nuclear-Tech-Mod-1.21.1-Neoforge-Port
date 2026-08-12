package com.hbm.weapon.ballistics;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponAimTest {
    @Test
    void positiveIronSightZeroMovesAuthoritativeRayBelowCameraCenter() {
        Vec3 centered = WeaponAim.direction(0.0F, 0.0F, 0.0D);
        Vec3 zeroed = WeaponAim.direction(0.0F, 0.0F, 0.65D);

        assertEquals(0.0D, centered.x, 1.0E-12D);
        assertEquals(0.0D, centered.y, 1.0E-12D);
        assertEquals(1.0D, centered.z, 1.0E-12D);
        assertTrue(zeroed.y < 0.0D);
        assertTrue(zeroed.z > 0.99D);
        assertEquals(1.0D, zeroed.length(), 1.0E-9D);
    }

    @Test
    void hudProjectionTracksTheSameAngularZero() {
        assertEquals(0, WeaponAim.screenOffsetY(0.0D, 57.4D, 540));
        int offset = WeaponAim.screenOffsetY(0.65D, 57.4D, 540);
        assertTrue(offset >= 5 && offset <= 6);
    }

    @Test
    void ballisticSolverCrossesTheSightLineAtFiftyBlocks() {
        Vec3 launch = WeaponAim.zeroedDirection(
                0.0F, 0.0F, 0.65D, 50.0D, 16.0D, 0.0245D, 0.001D);
        Vec3 sight = WeaponAim.direction(0.0F, 0.0F, 0.65D);

        Vec3 position = Vec3.ZERO;
        Vec3 velocity = launch.scale(16.0D);
        double targetHorizontal = Math.hypot(sight.x, sight.z) * 50.0D;
        double previousHorizontal = 0.0D;
        Vec3 previous = position;
        while (Math.hypot(position.x, position.z) < targetHorizontal) {
            previous = position;
            previousHorizontal = Math.hypot(position.x, position.z);
            position = position.add(velocity);
            velocity = velocity.scale(0.999D).add(0.0D, -0.0245D, 0.0D);
        }
        double currentHorizontal = Math.hypot(position.x, position.z);
        double blend = (targetHorizontal - previousHorizontal)
                / (currentHorizontal - previousHorizontal);
        double actualY = Mth.lerp(blend, previous.y, position.y);
        double expectedY = sight.y * 50.0D;

        assertEquals(expectedY, actualY, 1.0E-6D);
        assertTrue(launch.y > sight.y, "zeroing must compensate upward for gravity");
    }
}
