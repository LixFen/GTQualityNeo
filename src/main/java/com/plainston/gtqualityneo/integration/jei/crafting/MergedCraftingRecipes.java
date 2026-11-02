// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.crafting;


import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.jei.GT6_JEI_CraftingCategory;
import gregapi.recipes.GT6CraftingDispatcher;
import gregapi.recipes.ICraftingRecipeGT;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Shows GT6 workbench recipes as cards of JEI's vanilla crafting category instead of GT6's separate one. */
public final class MergedCraftingRecipes {
    private MergedCraftingRecipes() {}

    /** Display-only stand-in: the workbench still crafts through GT6's dispatcher, never through this. */
    public static final class View extends CustomRecipe {
        final ICraftingRecipeGT recipe;
        View(ICraftingRecipeGT recipe) { this.recipe = recipe; }
        @Override public boolean matches(CraftingInput input, Level level) { return false; }
        @Override public ItemStack assemble(CraftingInput input) { return ItemStack.EMPTY; }
        @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return GT6CraftingDispatcher.SERIALIZER; }
    }

    /** Lays the card out with GT6's own category code, on the same 116x54 grid as the vanilla category. */
    public static final class Extension implements ICraftingCategoryExtension<View> {
        private final GT6_JEI_CraftingCategory layout;
        public Extension(IGuiHelper guiHelper) { layout = new GT6_JEI_CraftingCategory(guiHelper); }
        @Override public List<SlotDisplay> getIngredients(RecipeHolder<View> holder) { return List.of(); }
        @Override public void setRecipe(RecipeHolder<View> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
            layout.setRecipe(builder, holder.value().recipe, focuses);
        }
    }

    /** Answers the vanilla crafting category from GT6's own crafting lookup, which stays registered but hidden. */
    public static final class Lookup implements ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> {
        private final Supplier<IJeiRuntime> runtime;
        private final Map<ICraftingRecipeGT, RecipeHolder<CraftingRecipe>> holders = new IdentityHashMap<>();
        public Lookup(Supplier<IJeiRuntime> runtime) { this.runtime = runtime; }

        private static boolean item(ITypedIngredient<?> ingredient) {
            return ingredient != null && ingredient.getItemStack().filter(stack -> !stack.isEmpty()).isPresent();
        }
        @Override public boolean isHandledInput(ITypedIngredient<?> input) { return item(input); }
        @Override public boolean isHandledOutput(ITypedIngredient<?> output) { return item(output); }
        @Override public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) { return find(input, RecipeIngredientRole.INPUT); }
        @Override public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) { return find(output, RecipeIngredientRole.OUTPUT); }
        @Override public List<RecipeHolder<CraftingRecipe>> getAllRecipes() { return find(null, null); }

        private List<RecipeHolder<CraftingRecipe>> find(ITypedIngredient<?> ingredient, RecipeIngredientRole role) {
            IJeiRuntime jei = runtime.get();
            if (jei == null || ingredient != null && !item(ingredient)) return List.of();
            try {
                var lookup = jei.getRecipeManager().createRecipeLookup(GT6_JEI_CraftingCategory.TYPE).includeHidden();
                if (ingredient != null) lookup = lookup.limitFocus(List.of(jei.getJeiHelpers().getFocusFactory().createFocus(role, ingredient)));
                return lookup.get().map(this::holder).toList();
            } catch (RuntimeException e) {
                // GT6 registers no crafting category when it has nothing to show; JEI would disable a plugin that throws.
                return List.of();
            }
        }

        /** One holder per recipe for the whole JEI session: JEI caches extensions and hidden recipes by holder. */
        private synchronized RecipeHolder<CraftingRecipe> holder(ICraftingRecipeGT recipe) {
            return holders.computeIfAbsent(recipe, r -> new RecipeHolder<>(ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(GTQualityNeo.MOD_ID, "gt6_crafting/" + holders.size())), new View(r)));
        }
    }
}
