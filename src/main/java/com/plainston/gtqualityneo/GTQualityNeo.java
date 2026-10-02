package com.plainston.gtqualityneo;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

@Mod(GTQualityNeo.MOD_ID)
public final class GTQualityNeo {
    public static final String MOD_ID = "gtqualityneo";
    public static final ModConfigSpec CONFIG;
    public static final ModConfigSpec.BooleanValue JADE_INTEGRATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        JADE_INTEGRATION = builder.comment("Register GTQualityNeo's Jade integrations. Restart required.")
            .define("jadeIntegration", true);
        CONFIG = builder.build();
    }

    public GTQualityNeo(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CONFIG);
    }
}
