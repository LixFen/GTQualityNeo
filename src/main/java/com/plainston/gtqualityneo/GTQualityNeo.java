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
        SAP_BAG_EXTRACTION, HEAT_HAZMAT_IMMUNITY, TOOL_BARS;
    public static final ModConfigSpec.DoubleValue SCAFFOLD_UP, SCAFFOLD_DOWN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        JADE_INTEGRATION = builder.comment("Register GTQualityNeo's Jade integrations. Restart required.")
            .define("jadeIntegration", true);
        CONFIG = builder.build();
        builder = new ModConfigSpec.Builder();
        OBSTRUCTED_INTERACTION = builder.define("allowObstructedInteraction", true);
        GUI_FLUID_INTERACTION = builder.define("guiFluidInteraction", true);
        SAP_BAG_EXTRACTION = builder.define("allowSapBagHopperExtraction", true);
        HEAT_HAZMAT_IMMUNITY = builder.define("heatHazmatFireImmunity", true);
        SCAFFOLD_UP = builder.defineInRange("scaffoldClimbUpSpeed",
            net.neoforged.fml.ModList.get().isLoaded("gaiablossom") ? 0.0 : 0.14, 0.0, 2.0);
        SCAFFOLD_DOWN = builder.defineInRange("scaffoldClimbDownSpeed", 0.15, 0.0, 2.0);
        SERVER_CONFIG = builder.build();
        builder = new ModConfigSpec.Builder();
        TOOL_BARS = builder.define("gtmToolBars", true);
        CLIENT_CONFIG = builder.build();
    }

    public GTQualityNeo(ModContainer container, IEventBus bus) {
        container.registerConfig(ModConfig.Type.COMMON, CONFIG);
        container.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG);
        bus.addListener(QolNetwork::register);
        CreativeTank.register(bus);
        NeoForge.EVENT_BUS.addListener(QolEvents::damage);
        NeoForge.EVENT_BUS.addListener(QolEvents::moldClick);
    }
}
