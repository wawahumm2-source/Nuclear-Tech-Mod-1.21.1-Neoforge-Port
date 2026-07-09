package com.hbm.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class HbmCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                HbmBlockEntities.BURNER_PRESS.get(),
                (burnerPress, side) -> burnerPress.getAutomationItems(side)
        );
    }

    private HbmCapabilities() {
    }
}
