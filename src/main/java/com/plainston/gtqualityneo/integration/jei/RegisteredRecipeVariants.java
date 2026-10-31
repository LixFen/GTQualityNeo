package com.plainston.gtqualityneo.integration.jei;

import gregapi.recipes.Recipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** One RecipeMap's process groups, maintained as GT6 accepts/removes recipes, independently of JEI lookups. */
public final class RegisteredRecipeVariants {
    private final Map<Object, Group> groups = new LinkedHashMap<>();
    private final Map<Recipe, Group> membership = new IdentityHashMap<>();
    private List<Recipe> pages;

    public synchronized void add(Recipe recipe) {
        if (!recipe.mEnabled || recipe.mHidden || membership.containsKey(recipe)) return;
        Object signature = RecipeVariants.key(recipe);
        Object key = signature == null ? recipe : signature;
        var group = groups.computeIfAbsent(key, Group::new);
        group.members.add(recipe);
        group.page = null;
        membership.put(recipe, group);
        pages = null;
    }

    public synchronized void remove(Recipe recipe) {
        // Use the registration-time key even if an addon edited the recipe before removing it.
        var group = membership.remove(recipe);
        if (group == null) return;
        group.members.remove(recipe);
        group.page = null;
        if (group.members.isEmpty()) groups.remove(group.key);
        pages = null;
    }

    public synchronized void rebuild(Collection<Recipe> recipes) {
        groups.clear();
        membership.clear();
        pages = null;
        for (var recipe : recipes) add(recipe);
    }

    public synchronized int groupCount() { return groups.size(); }

    /** Materializing a display copy does not reclassify or hash any recipe. Unchanged groups reuse their copy. */
    public synchronized List<Recipe> pages() {
        if (pages == null) pages = groups.values().stream().map(Group::page).toList();
        return pages;
    }

    public synchronized Recipe pageFor(Recipe source) {
        var group = membership.get(source);
        return group == null ? source : group.page();
    }

    public synchronized boolean represents(Recipe page, Recipe source) {
        return page == pageFor(source);
    }

    public synchronized List<Recipe> select(List<Recipe> sources) {
        var result = new LinkedHashSet<Recipe>();
        var transientRecipes = new ArrayList<Recipe>();
        for (var recipe : sources) {
            var group = membership.get(recipe);
            if (group == null) transientRecipes.add(recipe);
            else result.add(group.page());
        }
        // Some RecipeMap subclasses return virtual recipes that never pass through GT6's add API.
        if (!transientRecipes.isEmpty()) result.addAll(RecipeVariants.group(transientRecipes));
        return List.copyOf(result);
    }

    private static final class Group {
        private final Object key;
        private final LinkedHashSet<Recipe> members = new LinkedHashSet<>();
        private Recipe page;

        private Group(Object key) { this.key = key; }

        private Recipe page() {
            if (page == null) page = members.size() == 1 ? members.getFirst()
                : new RecipeVariants.Grouped(List.copyOf(members));
            return page;
        }
    }
}
