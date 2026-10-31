package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.integration.jei.MachineRecipeLookup;
import com.plainston.gtqualityneo.integration.jei.RecipeVariants;
import gregapi.data.FL;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Pseudo
@Mixin(targets = "gregapi.jei.GT6_JEI_Plugin$MapLookup", remap = false)
public abstract class JeiMachineLookupMixin {
    @Shadow @Final private RecipeMap mMap;
    @Unique private MachineRecipeLookup gtqualityneo$lookup;

    @Unique private MachineRecipeLookup gtqualityneo$lookup() {
        if (gtqualityneo$lookup == null)
            gtqualityneo$lookup = new MachineRecipeLookup(mMap, () -> new RecipeVariants.Mode(
                GTQualityNeo.JEI_FLUID_VARIANTS.get(), GTQualityNeo.JEI_OREDICT_VARIANTS.get()));
        return gtqualityneo$lookup;
    }

    @Unique private static ItemStack gtqualityneo$stack(ITypedIngredient<?> ingredient) {
        if (ingredient == null) return null;
        if (ingredient.getIngredient() instanceof FluidStack fluid) return FL.display(fluid, false, false);
        return ingredient.getItemStack().orElse(null);
    }

    @Inject(method = {"isHandledInput", "isHandledOutput"}, at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$acceptFluid(ITypedIngredient<?> ingredient, CallbackInfoReturnable<Boolean> cir) {
        var stack = gtqualityneo$stack(ingredient);
        cir.setReturnValue(stack != null && !stack.isEmpty());
    }

    @Inject(method = "getRecipesForInput", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$inputs(ITypedIngredient<?> ingredient, CallbackInfoReturnable<List<Recipe>> cir) {
        cir.setReturnValue(gtqualityneo$lookup().find(gtqualityneo$stack(ingredient), true));
    }

    @Inject(method = "getRecipesForOutput", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$outputs(ITypedIngredient<?> ingredient, CallbackInfoReturnable<List<Recipe>> cir) {
        cir.setReturnValue(gtqualityneo$lookup().find(gtqualityneo$stack(ingredient), false));
    }

    @Inject(method = "getAllRecipes", at = @At("HEAD"), cancellable = true)
    private void gtqualityneo$all(CallbackInfoReturnable<List<Recipe>> cir) {
        cir.setReturnValue(gtqualityneo$lookup().lazyAll());
    }
}
