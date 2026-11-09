package com.plainston.gtqualityneo.integration;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.CS;
import gregapi.data.FL;
import gregapi.data.MD;
import gregapi.oredict.OreDictMaterial;
import gregapi.recipes.Recipe;
import gregapi.util.OM;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Browser-only visibility; never removes recipes from GT6's processing maps. */
public final class RecipeMaterialVisibility {
    private RecipeMaterialVisibility() {}

    public static boolean hidden(OreDictMaterial material) {
        if (GTQualityNeo.CLIENT_CONFIG.isLoaded() && !GTQualityNeo.HIDE_INVISIBLE_MATERIALS.get()) return false;
        return !CS.SHOW_HIDDEN_MATERIALS && material != null && material.mHidden
            && material.mOriginalMod != null && material.mOriginalMod != MD.GAPI
            && !material.mOriginalMod.mLoaded;
    }

    public static boolean hidden(ItemStack stack) {
        if (stack == null) return false;
        var data = OM.anydata_(stack);
        return data != null && data.mMaterial != null && hidden(data.mMaterial.mMaterial);
    }

    public static boolean hidden(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var material = OreDictMaterial.FLUID_MAP.get(FL.regName(stack.getFluid()));
        return material != null && hidden(material.mMaterial);
    }

    public static boolean visible(Recipe recipe) {
        if (!recipe.mEnabled || recipe.mHidden) return false;
        for (var stack : recipe.mInputs) if (hidden(stack)) return false;
        for (var stack : recipe.mOutputs) if (hidden(stack)) return false;
        for (var stack : recipe.mFluidInputs) if (hidden(stack)) return false;
        for (var stack : recipe.mFluidOutputs) if (hidden(stack)) return false;
        return !(recipe.mSpecialItems instanceof ItemStack stack) || !hidden(stack);
    }
}
