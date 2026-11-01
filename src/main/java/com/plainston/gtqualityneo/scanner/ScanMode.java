package com.plainston.gtqualityneo.scanner;

public enum ScanMode {
    LARGE, SMALL, DENSE_AND_NORMAL, BEDROCK, FLUID_BEDROCK, ROCK, FLUID, OREDICT;

    public String translationKey() { return "gtqualityneo.scanner.mode." + name().toLowerCase(java.util.Locale.ROOT); }
    public boolean fluid() { return this == FLUID || this == FLUID_BEDROCK; }
    public enum Source { ORE_INDEX, BLOCK_ENTITIES, BLOCK_SECTIONS }
    public Source source() {
        return switch (this) {
            case LARGE, SMALL, BEDROCK -> Source.ORE_INDEX;
            case ROCK, FLUID_BEDROCK -> Source.BLOCK_ENTITIES;
            case DENSE_AND_NORMAL, FLUID, OREDICT -> Source.BLOCK_SECTIONS;
        };
    }
}
