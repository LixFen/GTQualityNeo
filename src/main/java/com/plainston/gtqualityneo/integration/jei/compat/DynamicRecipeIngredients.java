// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.compat;

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
