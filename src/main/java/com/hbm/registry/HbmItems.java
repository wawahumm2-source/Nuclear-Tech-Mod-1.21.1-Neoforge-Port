package com.hbm.registry;

import com.hbm.HbmNuclearTech;
import com.hbm.item.GeigerCounterItem;
import com.hbm.item.FalloutInjectorItem;
import com.hbm.item.HbmAmmoItem;
import com.hbm.item.HbmGunItem;
import com.hbm.item.RadAwayItem;
import com.hbm.item.RadXItem;
import com.hbm.item.RadioactiveBlockItem;
import com.hbm.item.RadioactiveItem;
import com.hbm.item.RadiationInspectorItem;
import com.hbm.item.RadiationPulseItem;
import com.hbm.item.RadiationResetItem;
import com.hbm.item.StampItem;
import com.hbm.item.StampType;
import com.hbm.block.SellafieldBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HbmNuclearTech.MOD_ID);

    public static final DeferredItem<BlockItem> URANIUM_ORE = ITEMS.register("uranium_ore",
            () -> new RadioactiveBlockItem(HbmBlocks.URANIUM_ORE.get(), new Item.Properties(), 0.35D));
    public static final DeferredItem<BlockItem> DEEPSLATE_URANIUM_ORE = ITEMS.register("deepslate_uranium_ore",
            () -> new RadioactiveBlockItem(HbmBlocks.DEEPSLATE_URANIUM_ORE.get(), new Item.Properties(), 0.35D));
    public static final DeferredItem<BlockItem> LEAD_ORE = ITEMS.registerSimpleBlockItem("lead_ore", HbmBlocks.LEAD_ORE);
    public static final DeferredItem<BlockItem> RADIOACTIVE_WASTE_BARREL = ITEMS.register("radioactive_waste_barrel",
            () -> new RadioactiveBlockItem(HbmBlocks.RADIOACTIVE_WASTE_BARREL.get(), new Item.Properties(), 5D));
    public static final DeferredItem<BlockItem> BURNER_PRESS = ITEMS.registerSimpleBlockItem("burner_press", HbmBlocks.BURNER_PRESS);
    public static final DeferredItem<BlockItem> PRESS_PREHEATER = ITEMS.registerSimpleBlockItem("press_preheater", HbmBlocks.PRESS_PREHEATER);
    public static final DeferredItem<BlockItem> PROTOTYPE_NUKE = ITEMS.register("prototype_nuke",
            () -> new RadioactiveBlockItem(HbmBlocks.PROTOTYPE_NUKE.get(), new Item.Properties(), 0.35D));
    public static final DeferredItem<BlockItem> NUKE_BOY = ITEMS.registerSimpleBlockItem("nuke_boy", HbmBlocks.NUKE_BOY);
    public static final DeferredItem<BlockItem> TNT = ITEMS.registerSimpleBlockItem("tnt", HbmBlocks.TNT);
    public static final DeferredItem<BlockItem> SELLAFIELD_SLAKED = ITEMS.registerSimpleBlockItem("sellafield_slaked", HbmBlocks.SELLAFIELD_SLAKED);
    public static final DeferredItem<BlockItem> SELLAFIELD = ITEMS.register("sellafield",
            () -> new SellafieldBlockItem(HbmBlocks.SELLAFIELD.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SELLAFIELD_BEDROCK = ITEMS.registerSimpleBlockItem(
            "sellafield_bedrock", HbmBlocks.SELLAFIELD_BEDROCK);
    public static final DeferredItem<BlockItem> ORE_SELLAFIELD_DIAMOND = ITEMS.registerSimpleBlockItem(
            "ore_sellafield_diamond", HbmBlocks.ORE_SELLAFIELD_DIAMOND);
    public static final DeferredItem<BlockItem> ORE_SELLAFIELD_EMERALD = ITEMS.registerSimpleBlockItem(
            "ore_sellafield_emerald", HbmBlocks.ORE_SELLAFIELD_EMERALD);
    public static final DeferredItem<BlockItem> ORE_SELLAFIELD_URANIUM_SCORCHED = ITEMS.registerSimpleBlockItem(
            "ore_sellafield_uranium_scorched", HbmBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED);
    public static final DeferredItem<BlockItem> ORE_SELLAFIELD_SCHRABIDIUM = ITEMS.registerSimpleBlockItem(
            "ore_sellafield_schrabidium", HbmBlocks.ORE_SELLAFIELD_SCHRABIDIUM);
    public static final DeferredItem<BlockItem> ORE_SELLAFIELD_RADGEM = ITEMS.register(
            "ore_sellafield_radgem",
            () -> new RadioactiveBlockItem(HbmBlocks.ORE_SELLAFIELD_RADGEM.get(), new Item.Properties(), 25D));
    public static final DeferredItem<BlockItem> WASTE_EARTH = ITEMS.registerSimpleBlockItem("waste_earth", HbmBlocks.WASTE_EARTH);
    public static final DeferredItem<BlockItem> GAS_RADON = ITEMS.registerSimpleBlockItem("gas_radon", HbmBlocks.GAS_RADON);
    public static final DeferredItem<BlockItem> GAS_RADON_DENSE = ITEMS.registerSimpleBlockItem("gas_radon_dense", HbmBlocks.GAS_RADON_DENSE);
    public static final DeferredItem<BlockItem> GAS_RADON_TOMB = ITEMS.registerSimpleBlockItem("gas_radon_tomb", HbmBlocks.GAS_RADON_TOMB);
    public static final DeferredItem<BlockItem> WASTE_LOG = ITEMS.registerSimpleBlockItem("waste_log", HbmBlocks.WASTE_LOG);
    public static final DeferredItem<BlockItem> WASTE_PLANKS = ITEMS.registerSimpleBlockItem("waste_planks", HbmBlocks.WASTE_PLANKS);
    public static final DeferredItem<BlockItem> WASTE_LEAVES = ITEMS.registerSimpleBlockItem("waste_leaves", HbmBlocks.WASTE_LEAVES);
    public static final DeferredItem<BlockItem> WASTE_MYCELIUM = ITEMS.registerSimpleBlockItem("waste_mycelium", HbmBlocks.WASTE_MYCELIUM);
    public static final DeferredItem<BlockItem> WASTE_TRINITITE = ITEMS.registerSimpleBlockItem("waste_trinitite", HbmBlocks.WASTE_TRINITITE);
    public static final DeferredItem<BlockItem> WASTE_TRINITITE_RED = ITEMS.registerSimpleBlockItem("waste_trinitite_red", HbmBlocks.WASTE_TRINITITE_RED);
    public static final DeferredItem<BlockItem> FALLOUT = ITEMS.registerSimpleBlockItem("fallout", HbmBlocks.FALLOUT);
    public static final DeferredItem<BucketItem> CONTAMINATED_WATER_BUCKET = ITEMS.register(
            "contaminated_water_bucket",
            () -> new BucketItem(HbmFluids.CONTAMINATED_WATER.get(), new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1))
    );

    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.register("uranium_ingot",
            () -> new RadioactiveItem(new Item.Properties(), 0.35D));
    public static final DeferredItem<Item> URANIUM_FUEL_PELLET = ITEMS.register("uranium_fuel_pellet",
            () -> new RadioactiveItem(new Item.Properties(), 0.05D));
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> GRAPHITE_INGOT = ITEMS.registerSimpleItem("graphite_ingot");
    public static final DeferredItem<Item> PLATE_IRON = ITEMS.registerSimpleItem("plate_iron");
    public static final DeferredItem<Item> PLATE_GOLD = ITEMS.registerSimpleItem("plate_gold");
    public static final DeferredItem<Item> PLATE_STEEL = ITEMS.registerSimpleItem("plate_steel");
    public static final DeferredItem<Item> PLATE_LEAD = ITEMS.registerSimpleItem("plate_lead");
    public static final DeferredItem<Item> WIRE_GOLD = ITEMS.registerSimpleItem("wire_gold");
    public static final DeferredItem<Item> GEM_RAD = ITEMS.register("gem_rad",
            () -> new RadioactiveItem(new Item.Properties(), 25D));
    public static final DeferredItem<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> RADAWAY = ITEMS.register("radaway",
            () -> new RadAwayItem(new Item.Properties().stacksTo(16), RadAwayItem.Treatment.NORMAL));
    public static final DeferredItem<Item> RADAWAY_STRONG = ITEMS.register("radaway_strong",
            () -> new RadAwayItem(new Item.Properties().stacksTo(16), RadAwayItem.Treatment.STRONG));
    public static final DeferredItem<Item> RADAWAY_FLUSH = ITEMS.register("radaway_flush",
            () -> new RadAwayItem(new Item.Properties().stacksTo(16), RadAwayItem.Treatment.FLUSH));
    public static final DeferredItem<Item> RADX = ITEMS.register("radx",
            () -> new RadXItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> RADIATION_INSPECTOR = ITEMS.register("dev_radiation_inspector",
            () -> new RadiationInspectorItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> RADIATION_RESET = ITEMS.register("dev_radiation_reset",
            () -> new RadiationResetItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FALLOUT_INJECTOR = ITEMS.register("dev_fallout_injector",
            () -> new FalloutInjectorItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> RADIATION_PULSE = ITEMS.register("dev_radiation_pulse",
            () -> new RadiationPulseItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CASING_SMALL = ITEMS.registerSimpleItem("casing_small");
    public static final DeferredItem<Item> CASING_RIFLE = ITEMS.registerSimpleItem("casing_rifle");
    public static final DeferredItem<Item> CASING_SHOTSHELL = ITEMS.registerSimpleItem("casing_shotshell");
    public static final DeferredItem<Item> CASING_40MM = ITEMS.registerSimpleItem("casing_40mm");
    public static final DeferredItem<Item> PROJECTILE_LEAD_SMALL = ITEMS.registerSimpleItem("projectile_lead_small");
    public static final DeferredItem<Item> PROJECTILE_STEEL_SMALL = ITEMS.registerSimpleItem("projectile_steel_small");
    public static final DeferredItem<Item> PROJECTILE_LEAD_RIFLE = ITEMS.registerSimpleItem("projectile_lead_rifle");
    public static final DeferredItem<Item> PROJECTILE_STEEL_RIFLE = ITEMS.registerSimpleItem("projectile_steel_rifle");
    public static final DeferredItem<Item> PELLETS_LEAD = ITEMS.registerSimpleItem("pellets_lead");
    public static final DeferredItem<Item> SLUG_LEAD = ITEMS.registerSimpleItem("slug_lead");
    public static final DeferredItem<Item> PROJECTILE_40MM_HE = ITEMS.registerSimpleItem("projectile_40mm_he");
    public static final DeferredItem<Item> PROJECTILE_40MM_HEAT = ITEMS.registerSimpleItem("projectile_40mm_heat");

    // The four pilot guns intentionally share one implementation; behavior is definition-driven.
    public static final DeferredItem<HbmGunItem> GUN_STAR_F = registerGun("gun_star_f", "star_f", "star_f");
    public static final DeferredItem<HbmGunItem> GUN_STG77 = registerGun("gun_stg77", "stg77", "stg77");
    public static final DeferredItem<HbmGunItem> GUN_SPAS12 = registerGun("gun_spas12", "spas-12", "spas12");
    public static final DeferredItem<HbmGunItem> GUN_CONGOLAKE = registerGun("gun_congolake", "congolake", "congolake");

    public static final DeferredItem<HbmAmmoItem> P22_FMJ = registerAmmo("p22_fmj", 64);
    public static final DeferredItem<HbmAmmoItem> P22_AP = registerAmmo("p22_ap", 64);
    public static final DeferredItem<HbmAmmoItem> R556_FMJ = registerAmmo("r556_fmj", 64);
    public static final DeferredItem<HbmAmmoItem> R556_AP = registerAmmo("r556_ap", 64);
    public static final DeferredItem<HbmAmmoItem> G12_BUCKSHOT = registerAmmo("g12_buckshot", 32);
    public static final DeferredItem<HbmAmmoItem> G12_SLUG = registerAmmo("g12_slug", 32);
    public static final DeferredItem<HbmAmmoItem> G40_HE = registerAmmo("g40_he", 16);
    public static final DeferredItem<HbmAmmoItem> G40_HEAT = registerAmmo("g40_heat", 16);
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

    private static DeferredItem<HbmGunItem> registerGun(String name, String modelName, String animationName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, name);
        ResourceLocation model = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID,
                "models/weapons/" + modelName + ".obj");
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID,
                "textures/models/weapons/" + modelName + ".png");
        ResourceLocation animation = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID,
                "animations/weapon/" + animationName + ".animation.json");
        return ITEMS.register(name, () -> new HbmGunItem(
                new Item.Properties().stacksTo(1), id, model, texture, animation));
    }

    private static DeferredItem<HbmAmmoItem> registerAmmo(String name, int stackSize) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HbmNuclearTech.MOD_ID, name);
        return ITEMS.register(name, () -> new HbmAmmoItem(new Item.Properties().stacksTo(stackSize), id));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private HbmItems() {
    }
}
