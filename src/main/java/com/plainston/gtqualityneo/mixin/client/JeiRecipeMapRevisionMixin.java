// 兼容说明：依赖 GT6w alpha.17 / 当前 JEI 的接口；未来上游更新后本适配可能过时，需复核。
package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.compat.RecipeMapRevision;
import com.plainston.gtqualityneo.integration.jei.machine.RegisteredRecipeVariants;
import com.plainston.gtqualityneo.integration.jei.compat.RegistrationCollisionIndex;
import com.plainston.gtqualityneo.integration.jei.machine.RecipeQueryIndex;
import gregapi.recipes.IRecipeMapHandler;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.objectweb.asm.Opcodes;

@Mixin(value = RecipeMap.class, remap = false)
public abstract class JeiRecipeMapRevisionMixin implements RecipeMapRevision, RegistrationCollisionIndex.Holder, RecipeQueryIndex.Holder {
    @Unique private volatile long gtqualityneo$revision;
    @Unique private RegisteredRecipeVariants gtqualityneo$variants;
    @Unique private RegistrationCollisionIndex gtqualityneo$collisions;
    @Unique private RecipeQueryIndex gtqualityneo$queryIndex;
    @Unique private volatile long gtqualityneo$resetVersion;
    @Override public synchronized RecipeQueryIndex gtqualityneo$queryIndex() {
        if (gtqualityneo$queryIndex == null) {
            gtqualityneo$queryIndex = new RecipeQueryIndex();
            gtqualityneo$queryIndex.rebuild(((RecipeMap) (Object) this).mRecipeList);
        }
        return gtqualityneo$queryIndex;
    }
    @Override public long gtqualityneo$recipeResetVersion() { return gtqualityneo$resetVersion; }

    @Override public synchronized RegistrationCollisionIndex gtqualityneo$collisionIndex() {
        if (gtqualityneo$collisions == null) gtqualityneo$collisions = new RegistrationCollisionIndex();
        return gtqualityneo$collisions;
    }

    @Override public long gtqualityneo$recipeRevision() { return gtqualityneo$revision; }

    @Override public synchronized RegisteredRecipeVariants gtqualityneo$recipeVariants() {
        if (gtqualityneo$variants == null) gtqualityneo$variants = new RegisteredRecipeVariants();
        return gtqualityneo$variants;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtqualityneo$initialRecipes(CallbackInfo ci) {
        var recipes = ((RecipeMap) (Object) this).mRecipeList;
        if (gtqualityneo$variants == null && !recipes.isEmpty()) gtqualityneo$recipeVariants().rebuild(recipes);
        if (gtqualityneo$collisions == null && !recipes.isEmpty()) gtqualityneo$collisionIndex().rebuild(recipes);
    }

    // This field is incremented only after GT6 accepts the recipe. RecipeCapture's early return must not register it.
    @Inject(method = "add(Lgregapi/recipes/Recipe;ZZ)Lgregapi/recipes/Recipe;",
        at = @At(value = "FIELD", target = "Lgregapi/recipes/Recipe$RecipeMap;mRecipeListSize:I",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void gtqualityneo$added(Recipe recipe, boolean logErrors, boolean addConfig, CallbackInfoReturnable<Recipe> cir) {
        gtqualityneo$recipeVariants().add(recipe);
        gtqualityneo$collisionIndex().add(recipe);
        if (gtqualityneo$queryIndex != null) gtqualityneo$queryIndex.add(recipe);
        gtqualityneo$revision++;
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void gtqualityneo$removed(Recipe recipe, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            gtqualityneo$recipeVariants().remove(recipe);
            gtqualityneo$collisionIndex().remove(recipe);
            if (gtqualityneo$queryIndex != null) gtqualityneo$queryIndex.remove(recipe);
            gtqualityneo$resetVersion++;
            gtqualityneo$revision++;
        }
    }

    @Inject(method = "add(Lgregapi/recipes/IRecipeMapHandler;)Z", at = @At("RETURN"))
    private void gtqualityneo$handlerAdded(IRecipeMapHandler handler, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) { gtqualityneo$resetVersion++; gtqualityneo$revision++; }
    }

    @Inject(method = "removeSameHandler", at = @At("RETURN"))
    private void gtqualityneo$handlersRemoved(IRecipeMapHandler template, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() > 0) { gtqualityneo$resetVersion++; gtqualityneo$revision++; }
    }

    @Inject(method = "reInit()V", at = @At("RETURN"))
    private void gtqualityneo$rebuilt(CallbackInfo ci) {
        gtqualityneo$recipeVariants().rebuild(((RecipeMap) (Object) this).mRecipeList);
        gtqualityneo$collisionIndex().rebuild(((RecipeMap) (Object) this).mRecipeList);
        if (gtqualityneo$queryIndex != null) gtqualityneo$queryIndex.rebuild(((RecipeMap) (Object) this).mRecipeList);
        gtqualityneo$resetVersion++;
        gtqualityneo$revision++;
    }
}
