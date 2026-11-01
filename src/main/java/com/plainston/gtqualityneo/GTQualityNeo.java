package com.plainston.gtqualityneo;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import com.plainston.gtqualityneo.qol.QolEvents;
import com.plainston.gtqualityneo.qol.QolNetwork;
import com.plainston.gtqualityneo.fluid.CreativeTank;

@Mod(GTQualityNeo.MOD_ID)
public final class GTQualityNeo {
    public static final String MOD_ID = "gtqualityneo";
    public static final ModConfigSpec CONFIG;
    public static final ModConfigSpec.BooleanValue JADE_INTEGRATION;
    public static final ModConfigSpec SERVER_CONFIG, CLIENT_CONFIG;
    public static final ModConfigSpec.BooleanValue OBSTRUCTED_INTERACTION, GUI_FLUID_INTERACTION,
        SAP_BAG_EXTRACTION, HEAT_HAZMAT_IMMUNITY, TOOL_BARS, CIRCUIT_SELECTOR, UNIVERSAL_HAZMAT, JEI_WORLDGEN, DRILL_MODES, ONE_WAY_PIPES, JEI_MULTIBLOCKS, MODIFIED_TOOLTIPS, JEI_FLUID_VARIANTS, JEI_OREDICT_VARIANTS, JEI_MERGE_CRAFTING;
    public static final ModConfigSpec.BooleanValue MOLD_SELECTOR, STORAGE_FORM, CREATIVE_TANK, FILTER_GHOST,
        SCAFFOLD_COLLISION, HOPPER_SHAPES, JEI_MACHINE_LOOKUP;
    public static final ModConfigSpec.DoubleValue SCAFFOLD_UP, SCAFFOLD_DOWN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        JADE_INTEGRATION = builder.comment("Register GTQualityNeo's Jade integrations. Restart required.")
            .define("jadeIntegration", true);
        UNIVERSAL_HAZMAT = builder.comment("Enhance the universal hazard suit. Restart required; use the same value on both sides.")
            .define("universalHazmatEnhancement", true);
        CONFIG = builder.build();
        builder = new ModConfigSpec.Builder();
        DRILL_MODES = builder.define("drillRangeModes", true);
        ONE_WAY_PIPES = builder.define("oneWayFluidPipes", true);
        OBSTRUCTED_INTERACTION = builder.define("allowObstructedInteraction", true);
        GUI_FLUID_INTERACTION = builder.define("guiFluidInteraction", true);
        SAP_BAG_EXTRACTION = builder.define("allowSapBagHopperExtraction", true);
        HEAT_HAZMAT_IMMUNITY = builder.define("heatHazmatFireImmunity", true);
        CIRCUIT_SELECTOR = builder.define("circuitSelectorGui", true);
        MOLD_SELECTOR = builder.define("moldShapeSelector", true);
        STORAGE_FORM = builder.define("massStorageFormSwitching", true);
        CREATIVE_TANK = builder.comment("Enable the creative fluid tank interaction and fluid output. The registry entries remain available for existing worlds.")
            .define("creativeTank", true);
        SCAFFOLD_COLLISION = builder.define("scaffoldCollisionChanges", true);
        HOPPER_SHAPES = builder.define("hopperShapeChanges", true);
        SCAFFOLD_UP = builder.defineInRange("scaffoldClimbUpSpeed",
            net.neoforged.fml.ModList.get().isLoaded("gaiablossom") ? 0.0 : 0.14, 0.0, 2.0);
        SCAFFOLD_DOWN = builder.defineInRange("scaffoldClimbDownSpeed", 0.15, 0.0, 2.0);
        SERVER_CONFIG = builder.build();
        builder = new ModConfigSpec.Builder();
        MODIFIED_TOOLTIPS = builder.comment("Show animated rainbow tooltips on items with targeted GTQuality changes.")
            .define("modifiedItemTooltips", true);
        JEI_MULTIBLOCKS = builder.define("jeiMultiblockPreview", true);
        TOOL_BARS = builder.comment("Show tool durability/charge bars and fluid-colored battery charge bars in GUIs.")
            .define("gtmToolBars", true);
        JEI_WORLDGEN = builder.comment("Display configured GT6 resource generation rules in JEI. Restart required.")
            .define("jeiWorldgenDisplay", true);
        JEI_FLUID_VARIANTS = builder.comment("Combine machine recipes differing only in one fluid input with the same amount, outputs, duration and power.")
            .define("jeiFluidVariants", true);
        JEI_OREDICT_VARIANTS = builder.comment("Combine machine recipes whose GT6 material inputs differ only within one \"Any\" material family (e.g. AnySteel) and give the same outputs, duration and power.")
            .define("jeiOreDictVariants", true);
        JEI_MERGE_CRAFTING = builder.comment("Show GT6 workbench recipes in JEI's vanilla Crafting category instead of a separate GT6 one. Applies when JEI reloads.")
            .define("jeiMergeCraftingTable", true);
        JEI_MACHINE_LOOKUP = builder.define("jeiMachineRecipeLookup", true);
        FILTER_GHOST = builder.define("jeiFilterGhostIngredients", true);
        CLIENT_CONFIG = builder.build();
    }

    public GTQualityNeo(ModContainer container, IEventBus bus) {
        container.registerConfig(ModConfig.Type.COMMON, CONFIG);
        container.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG);
        bus.addListener(QolNetwork::register);
        bus.addListener(com.plainston.gtqualityneo.qol.UniversalHazmat::components);
        bus.addListener(com.plainston.gtqualityneo.qol.ObstructionConfig::configChanged);
        CreativeTank.register(bus);
        com.plainston.gtqualityneo.scanner.ScannerIntegration.register(bus, container);
        if (net.neoforged.fml.ModList.get().isLoaded("ae2"))
            com.plainston.gtqualityneo.integration.ae2.AE2Integration.register(bus);
        NeoForge.EVENT_BUS.addListener(QolEvents::damage);
        NeoForge.EVENT_BUS.addListener(com.plainston.gtqualityneo.tools.DrillInteraction::rightClick);
        NeoForge.EVENT_BUS.addListener(QolEvents::moldClick);
        NeoForge.EVENT_BUS.addListener(com.plainston.gtqualityneo.qol.CircuitInteraction::rightClickItem);
        NeoForge.EVENT_BUS.addListener(com.plainston.gtqualityneo.qol.CircuitInteraction::rightClickBlock);
        NeoForge.EVENT_BUS.addListener(com.plainston.gtqualityneo.qol.UniversalHazmat::tick);
        NeoForge.EVENT_BUS.addListener(com.plainston.gtqualityneo.qol.UniversalHazmat::equipmentChanged);
        NeoForge.EVENT_BUS.addListener(com.plainston.gtqualityneo.qol.ObstructionConfig::serverStarted);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) ->
            com.plainston.gtqualityneo.qol.ObstructionConfig.restore());
    }
}
