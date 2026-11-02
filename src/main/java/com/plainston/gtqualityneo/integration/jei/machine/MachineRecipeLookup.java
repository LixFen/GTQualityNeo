// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.machine;

import com.plainston.gtqualityneo.integration.jei.cache.RecipeConfigBatch;
import com.plainston.gtqualityneo.integration.jei.compat.RecipeMapRevision;

import gregapi.data.CS;
import gregapi.jei.GT6_JEI_StartMaps;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.function.Consumer;

/** Per-JEI-lifecycle caches; focused lookups retain GT6's redirects and on-demand handlers. */
public final class MachineRecipeLookup {
    private final RecipeMap map;
    private final RegisteredRecipeVariants variants;
    private final Supplier<RecipeVariants.Mode> mode;
    private final boolean standardQueries;
    private Stamp stamp;
    private List<Recipe> all;
    private final Map<Focus, CachedFocus> focused = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Focus, CachedFocus> entry) { return size() > 64; }
    };

    public MachineRecipeLookup(RecipeMap map, Supplier<RecipeVariants.Mode> mode) {
        this.map = map;
        this.variants = ((RecipeMapRevision) map).gtqualityneo$recipeVariants();
        this.mode = mode;
        this.standardQueries = standardQueries(map);
    }

    private static boolean standardQueries(RecipeMap map) {
        try {
            return map.getClass().getMethod("getNEIRecipesFor", ItemStack.class).getDeclaringClass() == RecipeMap.class
                && map.getClass().getMethod("getNEIUsagesFor", ItemStack.class).getDeclaringClass() == RecipeMap.class
                && map.getClass().getMethod("getNEIRecipes", ItemStack[].class).getDeclaringClass() == RecipeMap.class
                && map.getClass().getMethod("getNEIUsages", ItemStack[].class).getDeclaringClass() == RecipeMap.class;
        } catch (ReflectiveOperationException e) { return false; }
    }

    private Stamp current() {
        return new Stamp(map instanceof RecipeMapRevision revision ? revision.gtqualityneo$recipeRevision() : 0,
            ((RecipeMapRevision) map).gtqualityneo$recipeResetVersion(),
            map.mRecipeList.size(), map.mRecipeMapHandlers.size(), mode.get());
    }

    private void refresh() {
        var current = current();
        if (!current.equals(stamp)) {
            if (stamp == null || current.reset() != stamp.reset() || current.handlers() != stamp.handlers()
                || !current.mode().equals(stamp.mode())) focused.clear();
            stamp = current;
            all = null;
        }
    }

    private List<Recipe> pages(List<Recipe> recipes) {
        var current = mode.get();
        return current.any() ? variants.select(recipes, current) : List.copyOf(recipes);
    }

    public synchronized List<Recipe> find(ItemStack stack, boolean input) {
        if (stack == null || stack.isEmpty()) return List.of();
        synchronized (map) {
            refresh();
            var key = new Focus(stack.getItem(), stack.getCount(), stack.getComponentsPatch(), input);
            var cached = focused.get(key);
            if (cached != null) {
                if (cached.stamp().equals(stamp)) return cached.pages();
                if (standardQueries && !cached.queries().isEmpty()
                    && cached.queries().stream().allMatch(RecipeQueryIndex.QueryStamp::valid)) {
                    // New recipes may extend a merged process group without changing this focus's matches.
                    var result = pages(cached.sources());
                    focused.put(key, new CachedFocus(cached.sources(), result, cached.queries(), stamp));
                    return result;
                }
            }
            try (var batch = RecipeConfigBatch.open(); var capture = RecipeQueryIndex.capture()) {
                // GT6 answers the query (overridden by hammer/crucible maps); the RecipeMap mixin only narrows its scan.
                var sources = List.copyOf(input ? map.getNEIUsagesFor(stack) : map.getNEIRecipesFor(stack));
                var result = pages(sources);
                // Generation may have changed this and previously cached results, including all-recipes pages.
                refresh();
                focused.put(key, new CachedFocus(sources, result, capture.queries(), stamp));
                return result;
            } catch (Exception e) {
                e.printStackTrace(CS.ERR);
                return List.of();
            }
        }
    }

    public synchronized List<Recipe> all() {
        synchronized (map) {
            refresh();
            if (all != null) return all;
            boolean saveOnEdit = map.mConfigFile == null || map.mConfigFile.mSaveOnEdit;
            try (var batch = RecipeConfigBatch.open()) {
                // A snapshot visits every handler once; removing from an indexed loop skips its next neighbour.
                for (var handler : new ArrayList<>(map.mRecipeMapHandlers)) {
                    handler.addAllRecipes(map);
                    if (handler.isDone()) map.mRecipeMapHandlers.remove(handler);
                }
                List<Recipe> result;
                if (map.getClass().getMethod("getNEIAllRecipes").getDeclaringClass() != RecipeMap.class) {
                    // Hammer/crucible maps append virtual recipes outside the registration API.
                    result = pages(map.getNEIAllRecipes());
                } else if (mode.get().any()) {
                    result = variants.pages(mode.get());
                } else {
                    result = map.mRecipeList.stream().filter(recipe -> recipe.mEnabled && !recipe.mHidden).toList();
                }
                refresh();
                all = result;
                return result;
            } catch (Exception e) {
                e.printStackTrace(CS.ERR);
                return List.of();
            } finally {
                if (map.mConfigFile != null) map.mConfigFile.mSaveOnEdit = saveOnEdit;
            }
        }
    }

    /** Preserve GT6's cheap startup emptiness check rather than generating every machine at JEI startup. */
    public List<Recipe> lazyAll() { return new DeferredRecipes(); }

    private record Stamp(long revision, long reset, int recipes, int handlers, RecipeVariants.Mode mode) {}
    private record CachedFocus(List<Recipe> sources, List<Recipe> pages, List<RecipeQueryIndex.QueryStamp> queries, Stamp stamp) {}
    private record Focus(Item item, int count, DataComponentPatch components, boolean input) {}

    private final class DeferredRecipes extends AbstractList<Recipe> {
        private List<Recipe> resolved;
        private List<Recipe> resolve() {
            if (resolved == null) resolved = all();
            return resolved;
        }
        @Override public Recipe get(int index) { return resolve().get(index); }
        @Override public int size() { return resolve().size(); }
        @Override public Spliterator<Recipe> spliterator() {
            return new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
                private Recipe source;
                private RecipeVariants.Grouped early;
                private int index;
                private boolean started;

                @Override public Spliterator<Recipe> trySplit() { return null; }

                @Override public boolean tryAdvance(Consumer<? super Recipe> action) {
                    if (!started) {
                        started = true;
                        if (resolved == null && (source = GT6_JEI_StartMaps.firstReady(map)) != null) {
                            early = new RecipeVariants.Grouped(List.of(source));
                            if (mode.get().any()) early.completeFrom(variants.pageFor(source, mode.get()));
                            action.accept(early);
                            return true;
                        }
                    }
                    var recipes = resolve();
                    while (index < recipes.size()) {
                        var recipe = recipes.get(index++);
                        if (early != null && (mode.get().any() ? variants.represents(recipe, source, mode.get()) : recipe == source)) {
                            early.completeFrom(recipe);
                            continue;
                        }
                        action.accept(recipe);
                        return true;
                    }
                    return false;
                }
            };
        }
    }
}
