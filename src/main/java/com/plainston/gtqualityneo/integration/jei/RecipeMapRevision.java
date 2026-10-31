package com.plainston.gtqualityneo.integration.jei;

/** Implemented by the client RecipeMap mixin; no JEI classes are needed on a server. */
public interface RecipeMapRevision {
    long gtqualityneo$recipeRevision();
    RegisteredRecipeVariants gtqualityneo$recipeVariants();
}
