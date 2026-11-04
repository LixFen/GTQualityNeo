package com.plainston.gtqualityneo.integration.rei;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Supplies a machine category's cards and ingredient lookups for one REI reload. */
final class ReiMachineLookup implements DynamicDisplayGenerator<ReiMachineDisplay> {
    private static final Hash.Strategy<EntryStack<?>> FUZZY = new Hash.Strategy<>() {
        @Override public int hashCode(EntryStack<?> stack) {
            return stack == null ? 0 : Long.hashCode(EntryStacks.hashFuzzy(stack));
        }

        @Override public boolean equals(EntryStack<?> left, EntryStack<?> right) {
            return left == right || left != null && right != null && EntryStacks.equalsFuzzy(left, right);
        }
    };

    private final CategoryIdentifier<ReiMachineDisplay> category;
    private final List<ReiMachineDisplay> displays;
    private final Map<EntryStack<?>, List<ReiMachineDisplay>> inputs = new Object2ObjectOpenCustomHashMap<>(FUZZY);
    private final Map<EntryStack<?>, List<ReiMachineDisplay>> outputs = new Object2ObjectOpenCustomHashMap<>(FUZZY);

    ReiMachineLookup(CategoryIdentifier<ReiMachineDisplay> category, List<ReiMachineDisplay> displays) {
        this.category = category;
        this.displays = displays;
        for (var display : displays) {
            index(inputs, display.getInputEntries(), display);
            index(outputs, display.getOutputEntries(), display);
        }
    }

    private static void index(Map<EntryStack<?>, List<ReiMachineDisplay>> index, List<EntryIngredient> ingredients,
                              ReiMachineDisplay display) {
        for (var ingredient : ingredients) {
            for (var stack : ingredient) {
                if (stack.isEmpty()) continue;
                var matches = index.computeIfAbsent(stack, key -> new ArrayList<>());
                if (matches.isEmpty() || matches.getLast() != display) matches.add(display);
            }
        }
    }

    @Override public Optional<List<ReiMachineDisplay>> getRecipeFor(EntryStack<?> entry) {
        return find(outputs, entry);
    }

    @Override public Optional<List<ReiMachineDisplay>> getUsageFor(EntryStack<?> entry) {
        var workstations = CategoryRegistry.getInstance().get(category).getWorkstations();
        var wildcard = entry.wildcard();
        for (var workstation : workstations) {
            if (EntryIngredients.testFuzzy(workstation, entry) || EntryIngredients.testFuzzy(workstation, wildcard))
                return Optional.of(displays);
        }
        return find(inputs, entry);
    }

    @Override public Optional<List<ReiMachineDisplay>> generate(ViewSearchBuilder builder) {
        return builder.getCategories().contains(category) ? Optional.of(displays) : Optional.empty();
    }

    private static Optional<List<ReiMachineDisplay>> find(Map<EntryStack<?>, List<ReiMachineDisplay>> index,
                                                         EntryStack<?> entry) {
        var matches = index.get(entry);
        return Optional.ofNullable(matches != null ? matches : index.get(entry.wildcard()));
    }
}
