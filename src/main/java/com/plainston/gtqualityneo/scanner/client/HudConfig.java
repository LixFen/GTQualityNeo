package com.plainston.gtqualityneo.scanner.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HudConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue X, Y;
    public static final ModConfigSpec.BooleanValue LIST;
    static {
        var b = new ModConfigSpec.Builder();
        LIST = b.define("showChunkResources", true);
        X = b.defineInRange("x", 8, 0, 8192);
        Y = b.defineInRange("y", 20, 0, 8192);
        SPEC = b.build();
    }
    private HudConfig() {}
}
