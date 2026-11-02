// 兼容说明：注入 GT6w alpha.17 RecipeMap.getNEIRecipes/getNEIUsages 的配方遍历与 handler 调用；上游修改这两个方法后需复核。
package com.plainston.gtqualityneo.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.integration.jei.compat.RecipeMapRevision;
import com.plainston.gtqualityneo.integration.jei.machine.RecipeQueryIndex;
import gregapi.oredict.OreDictItemData;
import gregapi.recipes.IRecipeMapHandler;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.recipes.handlers.RecipeMapHandlerMaterial;
import gregapi.recipes.handlers.RecipeMapHandlerPrefix;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * GT6 keeps the whole NEI query (focus expansion, handler generation, matching); this only narrows the recipe scan
 * to indexed candidates and skips repeated standard-template generation.
 */
@Mixin(value = RecipeMap.class, remap = false)
public abstract class JeiRecipeMapQueryMixin {
    @Unique private final Map<IRecipeMapHandler, Set<Object>> gtqualityneo$generated = new HashMap<>();
    @Unique private long gtqualityneo$generatedReset = Long.MIN_VALUE;

    @WrapOperation(method = "getNEIRecipes", at = @At(value = "FIELD",
        target = "Lgregapi/recipes/Recipe$RecipeMap;mRecipeList:Ljava/util/Collection;"))
    private Collection<Recipe> gtqualityneo$producingCandidates(RecipeMap map, Operation<Collection<Recipe>> original,
                                                               @Local(argsOnly = true) ItemStack[] outputs) {
        if (!GTQualityNeo.JEI_MACHINE_LOOKUP.get()) return original.call(map);
        return ((RecipeQueryIndex.Holder) map).gtqualityneo$queryIndex().candidates(outputs, false);
    }

    @WrapOperation(method = "getNEIUsages", at = @At(value = "FIELD",
        target = "Lgregapi/recipes/Recipe$RecipeMap;mRecipeList:Ljava/util/Collection;"))
    private Collection<Recipe> gtqualityneo$usingCandidates(RecipeMap map, Operation<Collection<Recipe>> original,
                                                           @Local(argsOnly = true) ItemStack[] inputs) {
        if (!GTQualityNeo.JEI_MACHINE_LOOKUP.get()) return original.call(map);
        return ((RecipeQueryIndex.Holder) map).gtqualityneo$queryIndex().candidates(inputs, true);
    }

    @WrapOperation(method = "getNEIRecipes", at = @At(value = "INVOKE", target =
        "Lgregapi/recipes/IRecipeMapHandler;addRecipesProducing(Lgregapi/recipes/Recipe$RecipeMap;ZLnet/minecraft/world/item/ItemStack;Lgregapi/oredict/OreDictItemData;)Z"))
    private boolean gtqualityneo$produceOnce(IRecipeMapHandler handler, RecipeMap map, boolean nei, ItemStack stack,
                                             OreDictItemData data, Operation<Boolean> original) {
        return gtqualityneo$generateOnce(handler, data, () -> original.call(handler, map, nei, stack, data));
    }

    @WrapOperation(method = "getNEIUsages", at = @At(value = "INVOKE", target =
        "Lgregapi/recipes/IRecipeMapHandler;addRecipesUsing(Lgregapi/recipes/Recipe$RecipeMap;ZLnet/minecraft/world/item/ItemStack;Lgregapi/oredict/OreDictItemData;)Z"))
    private boolean gtqualityneo$useOnce(IRecipeMapHandler handler, RecipeMap map, boolean nei, ItemStack stack,
                                         OreDictItemData data, Operation<Boolean> original) {
        return gtqualityneo$generateOnce(handler, data, () -> original.call(handler, map, nei, stack, data));
    }

    /** Only successful standard-template generation is remembered; failed or custom handlers can be stateful. */
    @Unique private boolean gtqualityneo$generateOnce(IRecipeMapHandler handler, OreDictItemData data,
                                                     java.util.function.BooleanSupplier generate) {
        Object key = GTQualityNeo.JEI_MACHINE_LOOKUP.get() ? gtqualityneo$generationKey(handler, data) : null;
        if (key == null) return generate.getAsBoolean();
        synchronized (gtqualityneo$generated) {
            long reset = ((RecipeMapRevision) (Object) this).gtqualityneo$recipeResetVersion();
            if (gtqualityneo$generatedReset != reset) { gtqualityneo$generated.clear(); gtqualityneo$generatedReset = reset; }
            if (gtqualityneo$generated.getOrDefault(handler, Set.of()).contains(key)) return false;
        }
        boolean added = generate.getAsBoolean();
        if (added) synchronized (gtqualityneo$generated) {
            gtqualityneo$generated.computeIfAbsent(handler, unused -> new HashSet<>()).add(key);
        }
        return added;
    }

    @Unique private static Object gtqualityneo$generationKey(IRecipeMapHandler handler, OreDictItemData data) {
        if (data == null || !data.validData()) return null;
        if (handler.getClass() == RecipeMapHandlerPrefix.class) return data.mMaterial.mMaterial;
        if (handler.getClass() == RecipeMapHandlerMaterial.class) return data.mPrefix;
        return null;
    }
}
