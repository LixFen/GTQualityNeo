package com.plainston.gtqualityneo.tools;

import java.util.ArrayList;
import java.util.List;

public final class DrillModes {

    private DrillModes() {}

    public static int normalize(int tier, int mode) {
        return tier >= 1 && tier <= 3 && mode >= 0 && mode <= (tier == 1 ? 1 : 2) ? mode : 0;
    }

    public static int next(int tier, int mode) {
        return normalize(tier, normalize(tier, mode) + 1);
    }

    public static int depth(int tier, int mode) {
        int valid = normalize(tier, mode);
        return valid == 0 ? 0 : valid == 1 ? 1 : tier;
    }

    /** Offsets relative to the targeted block; the targeted block itself is excluded. */
    public static List<int[]> offsets(int side, int depth) {
        List<int[]> offsets = new ArrayList<>();
        if (side < 0 || side > 5) return offsets;
        int axis = side / 2;
        // Minecraft side order: down/up, north/south, west/east.
        int normal = axis == 0 ? 1 : axis == 1 ? 2 : 0;
        int first = (normal + 1) % 3, second = (normal + 2) % 3;
        int inward = side % 2 == 0 ? 1 : -1;
        for (int layer = 0; layer < depth; layer++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (layer == 0 && a == 0 && b == 0) continue;
                    int[] offset = new int[3];
                    offset[normal] = layer * inward;
                    offset[first] = a;
                    offset[second] = b;
                    offsets.add(offset);
                }
            }
        }
        return offsets;
    }
}

