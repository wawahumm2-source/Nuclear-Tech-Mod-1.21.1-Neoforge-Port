package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.menu.BurnerPressMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, HbmNuclearTech.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BurnerPressMenu>> BURNER_PRESS =
            MENUS.register("burner_press", () -> new MenuType<>(BurnerPressMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private HbmMenus() {
    }
}
