// Compatibility: GT6w alpha.17 world-load pipeline and deferred-item queue.
package com.plainston.gtqualityneo.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.plainston.gtqualityneo.client.loading.GtLoadingProgress;
import gregapi.GT_API;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GT_API.class, remap = false)
public abstract class GtLoadingLifecycleMixin {
    @WrapMethod(method = "onLevelLoadEarlyItemInit")
    private void gtqualityneo$world(LevelEvent.Load event, Operation<Void> original) {
        boolean loadsContent = event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
            || event.getLevel() instanceof Level clientLevel && clientLevel.isClientSide() && !GT_API.DEFERRED_ITEM_INIT.isEmpty();
        if (!loadsContent) { original.call(event); return; }
        try (var progress = GtLoadingProgress.begin("world", 0)) { original.call(event); }
    }

    @WrapMethod(method = "runDeferredItemInit")
    private static void gtqualityneo$content(HolderLookup.Provider registries, Operation<Void> original) {
        try (var progress = GtLoadingProgress.begin("content", GT_API.DEFERRED_ITEM_INIT.size())) { original.call(registries); }
    }

    @WrapOperation(method = "runDeferredItemInit", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private static void gtqualityneo$queuedTask(Runnable task, Operation<Void> original) {
        GtLoadingProgress.queuedTask(GT_API.DEFERRED_ITEM_INIT.size());
        original.call(task);
    }

    @Inject(method = "buildCraftingBuffer", at = @At("HEAD"))
    private static void gtqualityneo$crafting(CallbackInfo ci) { GtLoadingProgress.describe("crafting"); }
    @Inject(method = "applyDatapackRecipeSuppression", at = @At("HEAD"))
    private static void gtqualityneo$replacements(CallbackInfo ci) { GtLoadingProgress.describe("replacements"); }
    @Inject(method = "onLevelLoadEarlyItemInit", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/item/crafting/RecipeManager;finalizeRecipeLoading(Lnet/minecraft/world/flag/FeatureFlagSet;)V"))
    private void gtqualityneo$recipeData(CallbackInfo ci) { GtLoadingProgress.describe("recipe_data"); }
    @Inject(method = "onLevelLoadEarlyItemInit", at = @At(value = "INVOKE", target =
        "Lgt6mirror/net/minecraftforge/common/ChestGenHooks;injectAll(Lnet/minecraft/server/MinecraftServer;)V"))
    private void gtqualityneo$loot(CallbackInfo ci) { GtLoadingProgress.describe("loot"); }
}
