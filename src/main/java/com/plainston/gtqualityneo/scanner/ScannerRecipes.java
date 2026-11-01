package com.plainston.gtqualityneo.scanner;

import gregapi.data.*;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.CR;
import net.minecraft.world.item.ItemStack;

/** Same tier materials and components as the original scanner; GT6 resolves ore dictionary ingredients. */
final class ScannerRecipes {
    static void register() {
        OreDictMaterial[] metals = {MT.SteelGalvanized, MT.Aluminium, MT.Titanium, MT.TungstenSteel};
        String[] sticks = {"gt:usbstick2", "gt:usbstick3", "gt:usbstick4", "gt:usbstick4"};
        for (int i = 0; i < 4; i++) {
            int tier = i + 2;
            CR.shaped(new ItemStack(ScannerIntegration.SCANNERS.get(i).get()), new Object[]{
                    "FCs", "MPM", "MDM",
                    'M', OP.plate.mat(metals[i], 1), 'C', CS.OD_CIRCUITS[tier],
                    'P', OP.foil.mat(MT.Plastic, 1), 'D', sticks[i], 'F', IL.SENSORS[tier].get(1)
            });
        }
    }
    private ScannerRecipes() {}
}
