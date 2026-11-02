package com.plainston.gtqualityneo.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.plainston.gtqualityneo.client.loading.GtLoadingProgress;
import org.spongepowered.asm.mixin.Mixin;

/** Name the substantial loaders inside GT6's opaque deferred Runnable batches. */
@Mixin(targets = {
    "gregtech.loaders.b.Loader_MultiTileEntities", "gregtech.loaders.b.Loader_Books",
    "gregtech.loaders.b.Loader_OreProcessing", "gregtech.loaders.b.Loader_Worldgen", "gregtech.loaders.b.Loader_ItemIterator",
    "gregtech.loaders.c.Loader_Recipes_Furnace", "gregtech.loaders.c.Loader_Recipes_Woods",
    "gregtech.loaders.c.Loader_Recipes_Vanilla", "gregtech.loaders.c.Loader_Recipes_Temporary",
    "gregtech.loaders.c.Loader_Recipes_Chem", "gregtech.loaders.c.Loader_Recipes_Crops",
    "gregtech.loaders.c.Loader_Recipes_Potions", "gregtech.loaders.c.Loader_Recipes_Food",
    "gregtech.loaders.c.Loader_Recipes_Ores", "gregtech.loaders.c.Loader_Recipes_Alloys",
    "gregtech.loaders.c.Loader_Recipes_Other", "gregtech.loaders.c.Loader_Recipes_Extruder",
    "gregtech.loaders.c.Loader_Recipes_Hints", "gregtech.loaders.c.Loader_Recipes_Replace",
    "gregtech.loaders.c.Loader_Recipes_Foreign", "gregtech.loaders.c.Loader_Recipes_Decomp",
    "gregtech.loaders.c.Loader_Recipes_Handlers"
}, remap = false)
public abstract class GtLoadingTasksMixin {
    @WrapMethod(method = "run")
    private void gtqualityneo$task(Operation<Void> original) {
        if (!GtLoadingProgress.active()) { original.call(); return; }
        try (var progress = GtLoadingProgress.begin(getClass().getSimpleName(), 0)) { original.call(); }
    }
}
