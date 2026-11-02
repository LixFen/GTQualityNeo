// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

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
    private RecipeVariants.Mode pagesMode;

    public synchronized void add(Recipe recipe) {
        if (!recipe.mEnabled || recipe.mHidden || membership.containsKey(recipe)) return;
        Object signature = RecipeVariants.key(recipe);
        Object key = signature == null ? recipe : signature;
        var group = groups.computeIfAbsent(key, Group::new);
        group.members.add(recipe);
        group.invalidate();
        membership.put(recipe, group);
        pages = null;
    }

    public synchronized void remove(Recipe recipe) {
        // Use the registration-time key even if an addon edited the recipe before removing it.
        var group = membership.remove(recipe);
        if (group == null) return;
        group.members.remove(recipe);
        group.invalidate();
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

    /** Materializing a display copy does not reclassify or hash any recipe. Unchanged groups reuse their copies. */
    public synchronized List<Recipe> pages(RecipeVariants.Mode mode) {
        if (pages == null || !mode.equals(pagesMode)) {
            pages = groups.values().stream().flatMap(group -> group.pages(mode).stream()).toList();
            pagesMode = mode;
        }
        return pages;
    }

    public synchronized Recipe pageFor(Recipe source, RecipeVariants.Mode mode) {
        var group = membership.get(source);
        return group == null ? source : group.pageFor(source, mode);
    }

    public synchronized boolean represents(Recipe page, Recipe source, RecipeVariants.Mode mode) {
        return page == pageFor(source, mode);
    }

    public synchronized List<Recipe> select(List<Recipe> sources, RecipeVariants.Mode mode) {
        var result = new LinkedHashSet<Recipe>();
        var transientRecipes = new ArrayList<Recipe>();
        for (var recipe : sources) {
            var group = membership.get(recipe);
            if (group == null) transientRecipes.add(recipe);
            else result.add(group.pageFor(recipe, mode));
        }
        // Some RecipeMap subclasses return virtual recipes that never pass through GT6's add API.
        if (!transientRecipes.isEmpty()) result.addAll(RecipeVariants.group(transientRecipes, mode));
        return List.copyOf(result);
    }

    private static final class Group {
        private final Object key;
        private final LinkedHashSet<Recipe> members = new LinkedHashSet<>();
        private RecipeVariants.Mode mode;
        private List<Recipe> pages;
        private Map<Recipe, Recipe> pageOf;

        private Group(Object key) { this.key = key; }

        private void invalidate() { pages = null; pageOf = null; mode = null; }

        /** The mode decides which members share a card; the split is cached until the group or the mode changes. */
        private void split(RecipeVariants.Mode current) {
            if (pages != null && current.equals(mode)) return;
            var buckets = new LinkedHashMap<Object, List<Recipe>>();
            boolean grouped = !(key instanceof Recipe);
            for (var recipe : members) {
                Object part = grouped ? RecipeVariants.partition(recipe, current) : null;
                buckets.computeIfAbsent(part == null ? recipe : part, unused -> new ArrayList<>()).add(recipe);
            }
            pageOf = new IdentityHashMap<>();
            var result = new ArrayList<Recipe>();
            for (var bucket : buckets.values()) for (var page : RecipeVariants.pages(bucket)) {
                result.add(page);
                if (page instanceof RecipeVariants.Grouped card) for (var recipe : card.members()) pageOf.put(recipe, page);
                else pageOf.put(page, page);
            }
            pages = List.copyOf(result);
            mode = current;
        }

        private List<Recipe> pages(RecipeVariants.Mode current) { split(current); return pages; }
        private Recipe pageFor(Recipe source, RecipeVariants.Mode current) { split(current); return pageOf.get(source); }
    }
}
