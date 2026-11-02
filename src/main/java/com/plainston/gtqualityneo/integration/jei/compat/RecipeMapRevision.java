// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.compat;

import com.plainston.gtqualityneo.integration.jei.machine.RegisteredRecipeVariants;

/** Implemented by the client RecipeMap mixin; no JEI classes are needed on a server. */
public interface RecipeMapRevision {
    long gtqualityneo$recipeRevision();
    default long gtqualityneo$recipeResetVersion() { return gtqualityneo$recipeRevision(); }
    RegisteredRecipeVariants gtqualityneo$recipeVariants();
}
