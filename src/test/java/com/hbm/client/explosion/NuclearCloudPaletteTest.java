package com.hbm.client.explosion;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NuclearCloudPaletteTest {
    @Test
    void cloudStartsYellowRatherThanGray() {
        NuclearCloudPalette.Rgb color = NuclearCloudPalette.colorAt(0, 1350, 12D, false);

        assertTrue(color.red() > color.green());
        assertTrue(color.green() > color.blue());
        assertTrue(color.red() > 1F);
    }

    @Test
    void cloudCoolsOnSourceTimelineRatherThanPersistenceTimeline() {
        NuclearCloudPalette.Rgb hot = NuclearCloudPalette.colorAt(100, 1350, 12D, false);
        NuclearCloudPalette.Rgb cooled = NuclearCloudPalette.colorAt(1350, 1350, 12D, false);

        assertTrue(hot.red() > cooled.red());
        assertTrue(Math.abs(cooled.red() - cooled.green()) < 0.0001F);
        assertTrue(Math.abs(cooled.green() - cooled.blue()) < 0.0001F);
    }

    @Test
    void structuralCollarIsBrighterThanTheSurroundingCloud() {
        NuclearCloudPalette.Rgb cloud = NuclearCloudPalette.colorAt(300, 1350, 12D, false);
        NuclearCloudPalette.Rgb collar = NuclearCloudPalette.colorAt(300, 1350, 12D, true);

        assertTrue(collar.red() + collar.green() + collar.blue()
                > cloud.red() + cloud.green() + cloud.blue());
    }
}
