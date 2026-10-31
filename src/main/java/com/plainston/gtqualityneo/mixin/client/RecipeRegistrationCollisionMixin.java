package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.RegistrationCollisionIndex;
import gregapi.code.ItemStackMap;
import gregapi.random.IHasWorldAndCoords;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RecipeMap.class, remap = false)
public abstract class RecipeRegistrationCollisionMixin {
    @Redirect(method = "addRecipe(Lgregapi/recipes/Recipe;ZZZZ)Lgregapi/recipes/Recipe;",
        at = @At(value = "INVOKE", target = "Lgregapi/recipes/Recipe$RecipeMap;findRecipeInternal(Lgregapi/random/IHasWorldAndCoords;Lgregapi/recipes/Recipe;ZZJLnet/minecraft/world/item/ItemStack;[Lnet/neoforged/neoforge/fluids/FluidStack;[Lnet/minecraft/world/item/ItemStack;)Lgregapi/recipes/Recipe;"))
    private Recipe gtqualityneo$registrationCandidates(RecipeMap map, IHasWorldAndCoords tile, Recipe previous,
            boolean loop, boolean unificate, long size, ItemStack special, FluidStack[] fluids, ItemStack[] inputs) {
        try (var scope = RegistrationCollisionIndex.open(map, inputs)) {
            return map.findRecipeInternal(tile, previous, loop, unificate, size, special, fluids, inputs);
        }
    }

    @Redirect(method = "findRecipeInternal", at = @At(value = "INVOKE",
        target = "Lgregapi/code/ItemStackMap;get(Lnet/minecraft/world/item/ItemStack;)Ljava/lang/Object;"))
    private Object gtqualityneo$exact(ItemStackMap<?, ?> index, ItemStack input) {
        return RegistrationCollisionIndex.filter(index, index.get(input));
    }

    @Redirect(method = "findRecipeInternal", at = @At(value = "INVOKE",
        target = "Lgregapi/code/ItemStackMap;get(Lnet/minecraft/world/item/ItemStack;J)Ljava/lang/Object;"))
    private Object gtqualityneo$wildcard(ItemStackMap<?, ?> index, ItemStack input, long meta) {
        return RegistrationCollisionIndex.filter(index, index.get(input, meta));
    }
}
