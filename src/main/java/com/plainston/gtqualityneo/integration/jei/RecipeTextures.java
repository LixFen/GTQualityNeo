package com.plainston.gtqualityneo.integration.jei;

/** Source pixels of the slot borders in GT6's original per-machine GUI atlas. */
public final class RecipeTextures {
    public record Source(int x, int y) {}

    private RecipeTextures() {}

    public static Source fluid(boolean output, int index) {
        return new Source(output ? 106 + index % 3 * 18 : 52 - index % 3 * 18, 62 - index / 3 * 18);
    }

    public static Source item(boolean output, int count, int fluids, int index) {
        int x, y;
        if (count <= 3) {
            x = output ? 107 + index * 18 : 71 - count * 18 + index * 18;
            y = fluids > 6 ? 7 : 25;
        } else if (count == 4) {
            x = (output ? 107 : 35) + index % 2 * 18;
            y = (fluids > 3 ? 7 : 16) + index / 2 * 18;
        } else if (count <= 6) {
            x = (output ? 107 : count == 5 && index >= 3 ? 35 : 17) + index % 3 * 18;
            y = (fluids > 3 ? 7 : 16) + index / 3 * 18;
        } else {
            int inset = count >= 10 && count < 12 && index >= 9 ? (12 - count) * 18 : 0;
            x = (output ? 107 : 17) + inset + index % 3 * 18;
            y = 7 + index / 3 * 18;
        }
        return new Source(x - 1, y - 1);
    }
}
