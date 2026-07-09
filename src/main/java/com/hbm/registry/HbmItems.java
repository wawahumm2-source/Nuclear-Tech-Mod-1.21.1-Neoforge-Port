package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.item.GeigerCounterItem;
import com.hbm.item.RadAwayItem;
import com.hbm.item.RadioactiveBlockItem;
import com.hbm.item.RadioactiveItem;
import com.hbm.item.StampItem;
import com.hbm.item.StampType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HbmNuclearTech.MOD_ID);

    public static final DeferredItem<BlockItem> URANIUM_ORE = ITEMS.register("uranium_ore",
            () -> new RadioactiveBlockItem(HbmBlocks.URANIUM_ORE.get(), new Item.Properties(), 0.08D));
    public static final DeferredItem<BlockItem> DEEPSLATE_URANIUM_ORE = ITEMS.register("deepslate_uranium_ore",
            () -> new RadioactiveBlockItem(HbmBlocks.DEEPSLATE_URANIUM_ORE.get(), new Item.Properties(), 0.12D));
    public static final DeferredItem<BlockItem> LEAD_ORE = ITEMS.registerSimpleBlockItem("lead_ore", HbmBlocks.LEAD_ORE);
    public static final DeferredItem<BlockItem> RADIOACTIVE_WASTE_BARREL = ITEMS.register("radioactive_waste_barrel",
            () -> new RadioactiveBlockItem(HbmBlocks.RADIOACTIVE_WASTE_BARREL.get(), new Item.Properties(), 0.20D));
    public static final DeferredItem<BlockItem> BURNER_PRESS = ITEMS.registerSimpleBlockItem("burner_press", HbmBlocks.BURNER_PRESS);
    public static final DeferredItem<BlockItem> PRESS_PREHEATER = ITEMS.registerSimpleBlockItem("press_preheater", HbmBlocks.PRESS_PREHEATER);
    public static final DeferredItem<BlockItem> PROTOTYPE_NUKE = ITEMS.register("prototype_nuke",
            () -> new RadioactiveBlockItem(HbmBlocks.PROTOTYPE_NUKE.get(), new Item.Properties(), 0.35D));

    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.register("uranium_ingot",
            () -> new RadioactiveItem(new Item.Properties(), 0.05D));
    public static final DeferredItem<Item> URANIUM_FUEL_PELLET = ITEMS.register("uranium_fuel_pellet",
            () -> new RadioactiveItem(new Item.Properties(), 0.18D));
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> GRAPHITE_INGOT = ITEMS.registerSimpleItem("graphite_ingot");
    public static final DeferredItem<Item> PLATE_IRON = ITEMS.registerSimpleItem("plate_iron");
    public static final DeferredItem<Item> PLATE_GOLD = ITEMS.registerSimpleItem("plate_gold");
    public static final DeferredItem<Item> PLATE_STEEL = ITEMS.registerSimpleItem("plate_steel");
    public static final DeferredItem<Item> PLATE_LEAD = ITEMS.registerSimpleItem("plate_lead");
    public static final DeferredItem<Item> WIRE_GOLD = ITEMS.registerSimpleItem("wire_gold");
    public static final DeferredItem<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> RADAWAY = ITEMS.register("radaway",
            () -> new RadAwayItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> STAMP_FLAT = registerStamp("stamp_flat", StampType.FLAT);
    public static final DeferredItem<Item> STAMP_PLATE = registerStamp("stamp_plate", StampType.PLATE);
    public static final DeferredItem<Item> STAMP_WIRE = registerStamp("stamp_wire", StampType.WIRE);
    public static final DeferredItem<Item> STAMP_CIRCUIT = registerStamp("stamp_circuit", StampType.CIRCUIT);
    public static final DeferredItem<Item> STAMP_357 = registerStamp("stamp_357", StampType.C357);
    public static final DeferredItem<Item> STAMP_44 = registerStamp("stamp_44", StampType.C44);
    public static final DeferredItem<Item> STAMP_50 = registerStamp("stamp_50", StampType.C50);
    public static final DeferredItem<Item> STAMP_9 = registerStamp("stamp_9", StampType.C9);
    public static final DeferredItem<Item> STAMP_PRINTING_1 = registerStamp("stamp_printing_1", StampType.PRINTING1);
    public static final DeferredItem<Item> STAMP_PRINTING_2 = registerStamp("stamp_printing_2", StampType.PRINTING2);
    public static final DeferredItem<Item> STAMP_PRINTING_3 = registerStamp("stamp_printing_3", StampType.PRINTING3);
    public static final DeferredItem<Item> STAMP_PRINTING_4 = registerStamp("stamp_printing_4", StampType.PRINTING4);
    public static final DeferredItem<Item> STAMP_PRINTING_5 = registerStamp("stamp_printing_5", StampType.PRINTING5);
    public static final DeferredItem<Item> STAMP_PRINTING_6 = registerStamp("stamp_printing_6", StampType.PRINTING6);
    public static final DeferredItem<Item> STAMP_PRINTING_7 = registerStamp("stamp_printing_7", StampType.PRINTING7);
    public static final DeferredItem<Item> STAMP_PRINTING_8 = registerStamp("stamp_printing_8", StampType.PRINTING8);

    private static DeferredItem<Item> registerStamp(String name, StampType stampType) {
        return ITEMS.register(name, () -> new StampItem(new Item.Properties().stacksTo(1).durability(100), stampType));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private HbmItems() {
    }
}
