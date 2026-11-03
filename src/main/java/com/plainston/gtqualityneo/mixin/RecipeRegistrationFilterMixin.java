// Compatibility: GT6w alpha.17 bulk registration reuses findRecipeInternal for collision detection.
package com.plainston.gtqualityneo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.plainston.gtqualityneo.integration.jei.machine.RecipeRegistrationFilter;
import gregapi.random.IHasWorldAndCoords;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RecipeMap.class, remap = false)
public abstract class RecipeRegistrationFilterMixin {
    @WrapMethod(method = "findRecipeInternal")
    private Recipe gtqualityneo$registrationInputs(IHasWorldAndCoords tile, Recipe previous, boolean loop,
        boolean unificate, long power, ItemStack special, FluidStack[] fluids, ItemStack[] items, Operation<Recipe> original) {
        try (var query = RecipeRegistrationFilter.query((RecipeMap) (Object) this, !loop && !unificate, items)) {
            return original.call(tile, previous, loop, unificate, power, special, fluids, items);
        }
    }

    @WrapOperation(method = "findRecipeInternal", at = @At(value = "INVOKE", target =
        "Lgregapi/recipes/Recipe;isRecipeInputEqual(ZZ[Lnet/neoforged/neoforge/fluids/FluidStack;[Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean gtqualityneo$rejectImpossibleInput(Recipe recipe, boolean consume, boolean ignoreAmounts,
        FluidStack[] fluids, ItemStack[] items, Operation<Boolean> original) {
        return RecipeRegistrationFilter.mayMatch((RecipeMap) (Object) this, recipe)
            && original.call(recipe, consume, ignoreAmounts, fluids, items);
    }
}
