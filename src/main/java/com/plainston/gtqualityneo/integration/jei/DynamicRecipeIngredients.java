package com.plainston.gtqualityneo.integration.jei;

import gregapi.recipes.Recipe.RecipeMap;
import java.util.List;

public final class DynamicRecipeIngredients {
    private static volatile long resourceVersion;
    private DynamicRecipeIngredients() {}
    public static long resourceVersion() { return resourceVersion; }
    public static void invalidateResources() { resourceVersion++; }

    public interface Category { RecipeMap gtqualityneo$recipeMap(); }
    public interface Cache { void gtqualityneo$cacheIngredients(Object recipe, List<?> links); }
}
