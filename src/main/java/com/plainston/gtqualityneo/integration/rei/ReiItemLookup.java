package com.plainston.gtqualityneo.integration.rei;

import dev.architectury.event.EventResult;
import gregapi.data.CS.ModIDs;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.view.Views;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.List;

final class ReiItemLookup {
    private ReiItemLookup() {}

    static EntryStack<?> wildcard(EntryStack<?> entry) {
        return isGtItem(entry) ? entry : entry.wildcard();
    }

    static EventResult filterDisplay(DisplayCategory<?> category, Display display) {
        var query = Views.getInstance().getContext();
        if (query == null || query.getCategories().contains(category.getCategoryIdentifier()))
            return EventResult.pass();

        var recipes = query.getRecipesFor();
        var usages = query.getUsagesFor();
        if (recipes.stream().noneMatch(ReiItemLookup::isGtItem) && usages.stream().noneMatch(ReiItemLookup::isGtItem))
            return EventResult.pass();

        var outputs = display.getOutputEntries();
        var inputs = display.getInputEntries();
        var workstations = usages.isEmpty() ? List.<EntryIngredient>of()
            : CategoryRegistry.getInstance().get(category.getCategoryIdentifier()).getWorkstations();
        // REI also searches with a component-free copy, which aliases GT6 subtype zero.
        boolean wrongSubtype = matchesFallback(recipes, outputs) || matchesFallback(usages, inputs)
            || matchesFallback(usages, workstations);
        if (!wrongSubtype || matches(recipes, outputs) || matches(usages, inputs) || matches(usages, workstations))
            return EventResult.pass();
        return EventResult.interruptFalse();
    }

    private static boolean matchesFallback(List<EntryStack<?>> query, Collection<EntryIngredient> ingredients) {
        for (var entry : query) {
            if (!isGtItem(entry)) continue;
            var wildcard = entry.wildcard();
            if (!EntryStacks.equalsFuzzy(entry, wildcard) && contains(ingredients, wildcard)) return true;
        }
        return false;
    }

    private static boolean matches(List<EntryStack<?>> query, Collection<EntryIngredient> ingredients) {
        for (var entry : query) {
            if (contains(ingredients, entry) || !isGtItem(entry) && contains(ingredients, entry.wildcard()))
                return true;
        }
        return false;
    }

    private static boolean contains(Collection<EntryIngredient> ingredients, EntryStack<?> entry) {
        return ingredients.stream().anyMatch(ingredient -> EntryIngredients.testFuzzy(ingredient, entry));
    }

    private static boolean isGtItem(EntryStack<?> entry) {
        if (entry.getType() != VanillaEntryTypes.ITEM) return false;
        ItemStack stack = entry.castValue();
        return ModIDs.isGregNamespace(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }
}
