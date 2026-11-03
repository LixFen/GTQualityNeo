// Compatibility: GT6w alpha.17 CR.live is vanilla-prefix + BUFFER; CR.remove owns its original get/remove loop.
package com.plainston.gtqualityneo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.plainston.gtqualityneo.integration.loading.CraftingRecipeList;
import gregapi.GT_API;
import gregapi.code.ArrayListNoNulls;
import gregapi.recipes.ICraftingRecipeGT;
import gregapi.util.CR;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;

public final class CraftingRemovalMixin {
    @Mixin(value = CR.class, remap = false)
    public abstract static class Registry {
        @WrapOperation(method = "<clinit>", at = @At(value = "NEW", target = "(I)Lgregapi/code/ArrayListNoNulls;"))
        private static ArrayListNoNulls<ICraftingRecipeGT> gtqualityneo$craftingList(int capacity,
            Operation<ArrayListNoNulls<ICraftingRecipeGT>> original) {
            return new CraftingRecipeList(capacity);
        }

        @WrapOperation(method = "remove([Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
        private static int gtqualityneo$nextRemovalCandidate(List<ICraftingRecipeGT> recipes, Operation<Integer> original,
            @Local LocalIntRef position, @Local CraftingInput grid) {
            int end = original.call(recipes);
            if (GT_API.sDeferredItemInitRunning && CR.BUFFER instanceof CraftingRecipeList buffer) {
                int prefix = end - buffer.size();
                if (position.get() >= prefix && position.get() < end) {
                    int previous = position.get();
                    int next = prefix + buffer.nextCandidate(previous - prefix, grid);
                    position.set(next);
                    // The left operand of i < size() is already on the JVM stack.
                    if (next == end) return previous;
                }
            }
            return end;
        }
    }

    @Mixin(value = GT_API.class, remap = false)
    public abstract static class Lifecycle {
        @WrapMethod(method = "runDeferredItemInit")
        private static void gtqualityneo$releaseCraftingIndex(HolderLookup.Provider registries, Operation<Void> original) {
            try { original.call(registries); }
            finally { if (CR.BUFFER instanceof CraftingRecipeList buffer) buffer.releaseIndex(); }
        }
    }
}
