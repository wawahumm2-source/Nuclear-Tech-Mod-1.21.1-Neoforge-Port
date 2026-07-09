package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HbmNuclearTech.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCKS = CREATIVE_TABS.register("blocks", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabBlocks"))
            .icon(() -> HbmItems.URANIUM_ORE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(HbmItems.URANIUM_ORE.get());
                output.accept(HbmItems.DEEPSLATE_URANIUM_ORE.get());
                output.accept(HbmItems.LEAD_ORE.get());
                output.accept(HbmItems.RADIOACTIVE_WASTE_BARREL.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONSUMABLE = CREATIVE_TABS.register("consumable", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabConsumable"))
            .icon(() -> HbmItems.GEIGER_COUNTER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(HbmItems.GEIGER_COUNTER.get());
                output.accept(HbmItems.RADAWAY.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTROL = CREATIVE_TABS.register("control", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabControl"))
            .icon(() -> HbmItems.URANIUM_FUEL_PELLET.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(HbmItems.URANIUM_FUEL_PELLET.get()))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MACHINE = CREATIVE_TABS.register("machine", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabMachine"))
            .icon(() -> HbmItems.BURNER_PRESS.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(HbmItems.BURNER_PRESS.get());
                output.accept(HbmItems.PRESS_PREHEATER.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NUKE = CREATIVE_TABS.register("nuke", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabNuke"))
            .icon(() -> HbmItems.PROTOTYPE_NUKE.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(HbmItems.PROTOTYPE_NUKE.get()))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PARTS = CREATIVE_TABS.register("parts", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabParts"))
            .icon(() -> HbmItems.URANIUM_INGOT.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(HbmItems.URANIUM_INGOT.get());
                output.accept(HbmItems.LEAD_INGOT.get());
                output.accept(HbmItems.STEEL_INGOT.get());
                output.accept(HbmItems.GRAPHITE_INGOT.get());
                output.accept(HbmItems.PLATE_IRON.get());
                output.accept(HbmItems.PLATE_GOLD.get());
                output.accept(HbmItems.PLATE_STEEL.get());
                output.accept(HbmItems.PLATE_LEAD.get());
                output.accept(HbmItems.WIRE_GOLD.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TEMPLATE = CREATIVE_TABS.register("template", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tabTemplate"))
            .icon(() -> HbmItems.STAMP_PLATE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(HbmItems.STAMP_FLAT.get());
                output.accept(HbmItems.STAMP_PLATE.get());
                output.accept(HbmItems.STAMP_WIRE.get());
                output.accept(HbmItems.STAMP_CIRCUIT.get());
                output.accept(HbmItems.STAMP_357.get());
                output.accept(HbmItems.STAMP_44.get());
                output.accept(HbmItems.STAMP_50.get());
                output.accept(HbmItems.STAMP_9.get());
                output.accept(HbmItems.STAMP_PRINTING_1.get());
                output.accept(HbmItems.STAMP_PRINTING_2.get());
                output.accept(HbmItems.STAMP_PRINTING_3.get());
                output.accept(HbmItems.STAMP_PRINTING_4.get());
                output.accept(HbmItems.STAMP_PRINTING_5.get());
                output.accept(HbmItems.STAMP_PRINTING_6.get());
                output.accept(HbmItems.STAMP_PRINTING_7.get());
                output.accept(HbmItems.STAMP_PRINTING_8.get());
            })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }

    private HbmCreativeTabs() {
    }
}
