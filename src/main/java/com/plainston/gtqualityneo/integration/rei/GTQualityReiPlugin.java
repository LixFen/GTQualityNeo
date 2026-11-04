package com.plainston.gtqualityneo.integration.rei;

import com.mojang.logging.LogUtils;
import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.integration.jei.machine.MachineRecipePreparation;
import com.plainston.gtqualityneo.integration.jei.machine.RecipeVariants;
import dev.architectury.fluid.FluidStack;
import gregapi.GT_API;
import gregapi.block.prefixblock.PrefixBlockItem;
import gregapi.data.CS.ModIDs;
import gregapi.data.MD;
import gregapi.fluid.FluidGT;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.ST;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

@REIPluginClient
public final class GTQualityReiPlugin implements REIClientPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<String, CategoryIdentifier<ReiMachineDisplay>> activeCategories = Map.of();
    private final Map<RecipeMap, CategoryIdentifier<ReiMachineDisplay>> categories = new LinkedHashMap<>();

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entry -> {
            if (entry.getType() == VanillaEntryTypes.FLUID)
                return entry.<FluidStack>castValue().getFluid() instanceof FluidGT.Source;
            if (entry.getType() != VanillaEntryTypes.ITEM) return false;
            ItemStack stack = entry.castValue();
            if (!ModIDs.isGregNamespace(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) return false;
            // REI inserts an item's base stack even when GT6 deliberately omits it from creative tabs.
            return ST.hidden(stack) || (stack.getItem() instanceof PrefixBlockItem && !stack.has(GT_API.SUBTYPE_TYPE));
        });
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        activeCategories = Map.of();
        categories.clear();
        var names = new HashSet<String>();
        for (var map : RecipeMap.RECIPE_MAP_LIST) {
            if (!map.mNEIAllowed) continue;
            try {
                CategoryIdentifier<ReiMachineDisplay> id = CategoryIdentifier.of(MD.GT.mID,
                    names.contains(map.mNameNEI) ? map.mNameInternal : map.mNameNEI);
                registry.add(new ReiMachineCategory(map, id));
                categories.put(map, id);
                names.add(map.mNameNEI);
                for (var machine : map.mRecipeMachineList) registry.addWorkstations(id, EntryStacks.of(machine));
            } catch (RuntimeException e) {
                LOGGER.error("Could not register REI category for {}", map.mNameInternal, e);
            }
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        var mode = new RecipeVariants.Mode(GTQualityNeo.JEI_FLUID_VARIANTS.get(), GTQualityNeo.JEI_OREDICT_VARIANTS.get());
        var names = new LinkedHashMap<String, CategoryIdentifier<ReiMachineDisplay>>();
        categories.forEach((map, id) -> {
            try {
                var displays = MachineRecipePreparation.prepare(map, mode).stream()
                    .map(recipe -> new ReiMachineDisplay(id, map, recipe)).toList();
                registry.registerDisplayGenerator(id, new ReiMachineLookup(id, displays));
                names.putIfAbsent(map.mNameNEI, id);
            } catch (Exception e) {
                LOGGER.error("Could not register REI recipes for {}", map.mNameInternal, e);
            }
        });
        ReiCraftingRecipes.register(registry);
        activeCategories = Map.copyOf(names);
    }

    public static boolean show(String name) {
        var category = name == null ? null : activeCategories.get(name);
        return category != null && ViewSearchBuilder.builder().addCategory(category).open();
    }
}
