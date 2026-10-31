package com.plainston.gtqualityneo.integration.jei;

import gregapi.jei.GT6_JEI_CraftingCategory;
import gregapi.recipes.ICraftingRecipeGT;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IRecipeLookup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class MergedCraftingRecipesTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void wrapsEachGt6RecipeInOneStableHolderAndPassesTheFocusThrough() {
        var first = recipe();
        var second = recipe();
        var asked = new ArrayList<Object>();
        var lookup = new MergedCraftingRecipes.Lookup(() -> runtime(List.of(first, second), asked, false));
        var all = lookup.getAllRecipes();
        assertEquals(2, all.size());
        assertSame(first, ((MergedCraftingRecipes.View) all.get(0).value()).recipe);
        assertNotEquals(all.get(0).id(), all.get(1).id());
        assertEquals(List.of("hidden", "stream"), asked);

        asked.clear();
        var focused = lookup.getRecipesForOutput(item(new ItemStack(Items.STICK)));
        assertSame(all.get(0), focused.get(0));
        assertSame(all.get(1), focused.get(1));
        assertEquals(List.of("hidden", RecipeIngredientRole.OUTPUT, "stream"), asked);
    }

    @Test void answersNothingBeforeJeiStartsOrWhenGt6HasNoCraftingCategory() {
        var stick = item(new ItemStack(Items.STICK));
        assertEquals(List.of(), new MergedCraftingRecipes.Lookup(() -> null).getRecipesForInput(stick));
        var failing = new MergedCraftingRecipes.Lookup(() -> runtime(List.of(recipe()), new ArrayList<>(), true));
        assertEquals(List.of(), failing.getAllRecipes());
        assertEquals(List.of(), failing.getRecipesForInput(stick));
        assertFalse(failing.isHandledInput(item(ItemStack.EMPTY)));
        assertTrue(failing.isHandledOutput(stick));
    }

    private static ICraftingRecipeGT recipe() {
        return proxy(ICraftingRecipeGT.class, (method, args) -> { throw new AssertionError(method); });
    }

    private static ITypedIngredient<?> item(ItemStack stack) {
        return proxy(ITypedIngredient.class, (method, args) -> switch (method) {
            case "getItemStack" -> Optional.of(stack);
            case "getIngredient" -> stack;
            default -> throw new AssertionError(method);
        });
    }

    private static IJeiRuntime runtime(List<ICraftingRecipeGT> recipes, List<Object> asked, boolean fail) {
        IRecipeLookup<?>[] lookup = new IRecipeLookup<?>[1];
        lookup[0] = proxy(IRecipeLookup.class, (method, args) -> switch (method) {
            case "includeHidden" -> { asked.add("hidden"); yield lookup[0]; }
            case "limitFocus" -> { asked.add(((IFocus<?>) ((List<?>) args[0]).get(0)).getRole()); yield lookup[0]; }
            case "get" -> { asked.add("stream"); yield recipes.stream(); }
            default -> throw new AssertionError(method);
        });
        IRecipeManager manager = proxy(IRecipeManager.class, (method, args) -> {
            if (!method.equals("createRecipeLookup")) throw new AssertionError(method);
            assertEquals(GT6_JEI_CraftingCategory.TYPE, args[0]);
            if (fail) throw new IllegalStateException("no such category");
            return lookup[0];
        });
        IFocusFactory focuses = proxy(IFocusFactory.class, (method, args) -> {
            if (!method.equals("createFocus")) throw new AssertionError(method);
            return proxy(IFocus.class, (focusMethod, focusArgs) -> {
                if (!focusMethod.equals("getRole")) throw new AssertionError(focusMethod);
                return args[0];
            });
        });
        IJeiHelpers helpers = proxy(IJeiHelpers.class, (method, args) -> {
            if (!method.equals("getFocusFactory")) throw new AssertionError(method);
            return focuses;
        });
        return proxy(IJeiRuntime.class, (method, args) -> switch (method) {
            case "getRecipeManager" -> manager;
            case "getJeiHelpers" -> helpers;
            default -> throw new AssertionError(method);
        });
    }

    private interface Handler { Object call(String method, Object[] args) throws Throwable; }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<?> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(self);
            case "equals" -> self == args[0];
            case "toString" -> type.getSimpleName();
            default -> handler.call(method.getName(), args);
        });
    }
}
