package com.plainston.gtqualityneo.integration.jei;

import gregapi.recipes.Recipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
class RegisteredRecipeVariantsTest {
    @Test void addingAndRemovingRecipesUpdatesOnlyTheirGroup() {
        var registry = new RegisteredRecipeVariants();
        var water = recipe(100);
        var lava = MachineRecipeLookupTest.recipe(new FluidStack(Fluids.LAVA, 100));
        var separate = recipe(200);
        registry.add(water);
        registry.add(separate);
        var unchangedPage = registry.pageFor(separate);
        registry.add(lava);
        registry.add(lava);
        assertEquals(2, registry.groupCount());
        assertEquals(2, registry.pages().size());
        assertEquals(2, RecipeVariants.inputOptions(registry.pageFor(water), 0).size());
        assertSame(unchangedPage, registry.pageFor(separate));
        registry.remove(water);
        assertSame(lava, registry.pageFor(lava));
        registry.remove(lava);
        assertEquals(List.of(separate), registry.pages());
        assertSame(unchangedPage, registry.pageFor(separate));
    }

    @Test void removalUsesOriginalKeyAndRebuildReclassifiesEditedRecipes() {
        var registry = new RegisteredRecipeVariants();
        var water = recipe(100);
        var lava = MachineRecipeLookupTest.recipe(new FluidStack(Fluids.LAVA, 100));
        registry.add(water);
        registry.add(lava);
        water.mDuration++;
        registry.remove(water);
        assertEquals(1, registry.groupCount());
        assertSame(lava, registry.pages().getFirst());
        registry.rebuild(List.of(water, lava));
        assertEquals(2, registry.groupCount());
        water.mDuration = lava.mDuration;
        registry.rebuild(List.of(water, lava));
        assertEquals(1, registry.groupCount());
    }

    @Test void hiddenDisabledAndVirtualRecipesDoNotPolluteRegistrationGroups() {
        var registry = new RegisteredRecipeVariants();
        var hidden = recipe(100);
        hidden.mHidden = true;
        var disabled = recipe(100);
        disabled.mEnabled = false;
        registry.add(hidden);
        registry.add(disabled);
        assertEquals(0, registry.groupCount());
        var virtual = new Recipe(false, false, new ItemStack[]{new ItemStack(Items.DIAMOND)},
            null, null, null, null, null, 20, 4, 0);
        assertEquals(List.of(virtual), registry.select(List.of(virtual)));
        assertEquals(0, registry.groupCount());
        assertTrue(registry.pages().isEmpty());
    }

    private static Recipe recipe(int amount) {
        return MachineRecipeLookupTest.recipe(new FluidStack(Fluids.WATER, amount));
    }
}
