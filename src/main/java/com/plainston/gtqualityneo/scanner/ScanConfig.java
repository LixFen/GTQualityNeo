package com.plainston.gtqualityneo.scanner;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ScanConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue BUDGET_MS, CONSUMPTION;
    public static final ModConfigSpec.BooleanValue LOAD_CHUNKS;
    public static final java.util.List<ModConfigSpec.IntValue> RANGES;
    static {
        var b = new ModConfigSpec.Builder();
        BUDGET_MS = b.comment("Global server tick budget, shared by all scans.").defineInRange("tickBudgetMs", 5, 1, 20);
        CONSUMPTION = b.comment("EU cost = this multiplier times tier voltage.").defineInRange("consumptionMultiplier", 50, 0, 8000);
        LOAD_CHUNKS = b.comment("Allow scans to load/generate chunks. Can exceed tick budget during generation.",
                "Default: scan loaded chunks only; unavailable chunks are visibly marked.").define("loadChunks", false);
        var ranges = new java.util.ArrayList<ModConfigSpec.IntValue>();
        for (int tier = 2; tier <= 5; tier++) ranges.add(b.defineInRange("rangeTier" + tier, tier * 2 + 5, 1, 31));
        RANGES = java.util.List.copyOf(ranges);
        SPEC = b.build();
    }
    public static int range(int tier) { return RANGES.get(tier - 2).get(); }
    private ScanConfig() {}
}
