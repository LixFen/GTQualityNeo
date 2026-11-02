// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.crafting;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.jei.GT6_JEI_CraftingCategory;
import gregapi.recipes.GT6CraftingDispatcher;
import gregapi.recipes.ICraftingRecipeGT;
import gregapi.recipes.ShapedOreRecipe;
import gregapi.recipes.ShapelessOreRecipe;
import gregapi.recipes.AdvancedCrafting1ToY;
import gregapi.recipes.AdvancedCraftingXToY;
import gregapi.util.CR;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
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
import java.util.ArrayList;
import java.util.List;

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

    public static void register(IRecipeRegistration registration) {
        registration.addRecipes(RecipeTypes.CRAFTING, prepare(CR.list()));
    }

    /** Match GT6's display filtering, then let JEI index the vanilla-category cards once per load. */
    static List<RecipeHolder<CraftingRecipe>> prepare(List<ICraftingRecipeGT> recipes) {
        var holders = new ArrayList<RecipeHolder<CraftingRecipe>>();
        for (var recipe : recipes) {
            if (!(recipe instanceof ShapedOreRecipe || recipe instanceof ShapelessOreRecipe
                || recipe instanceof AdvancedCrafting1ToY || recipe instanceof AdvancedCraftingXToY)) continue;
            if (!GT6_JEI_CraftingCategory.showable(recipe)) continue;
            holders.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(GTQualityNeo.MOD_ID, "gt6_crafting/" + holders.size())), new View(recipe)));
        }
        return List.copyOf(holders);
    }
}
