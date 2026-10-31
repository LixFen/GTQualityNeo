package com.plainston.gtqualityneo.integration.jei;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import com.plainston.gtqualityneo.client.MachineLayout;
import org.junit.jupiter.api.Test;

class RecipeLayoutTest {
    @Test void generatedSlotsStayInsideTheRecipeAndClearTheArrowSpecialSlotAndInfo() {
        int[] counts = {0, 1, 2, 3, 4, 6, 9, 12, 16};
        for (int in : counts) for (int out : counts) for (int fi : counts) for (int fo : counts) {
            var layout = new RecipeLayout(in, out, fi, fo);
            var slots = new ArrayList<RecipeLayout.Position>();
            for (boolean output : new boolean[]{false, true}) for (boolean fluid : new boolean[]{false, true}) {
                int count = output ? (fluid ? fo : out) : (fluid ? fi : in);
                for (int i = 0; i < count; i++) slots.add(layout.slot(output, fluid, i));
            }
            slots.add(layout.special());
            for (int i = 0; i < slots.size(); i++) {
                var slot = slots.get(i);
                assertTrue(slot.x() >= 0 && slot.x() + 18 <= RecipeLayout.WIDTH);
                assertTrue(slot.y() >= RecipeLayout.TOP && slot.y() + 18 < layout.infoY());
                assertFalse(overlap(slot, 18, new RecipeLayout.Position(RecipeLayout.ARROW_X, layout.arrowY()), 20));
                for (int j = 0; j < i; j++) assertFalse(overlap(slot, 18, slots.get(j), 18));
            }
            assertEquals(RecipeLayout.INFO_HEIGHT, layout.height() - layout.infoY());
        }
    }

    private static boolean overlap(RecipeLayout.Position a, int width, RecipeLayout.Position b, int otherWidth) {
        return a.x() < b.x() + otherWidth && a.x() + width > b.x() && a.y() < b.y() + 18 && a.y() + 18 > b.y();
    }

    @Test void usesTheExistingMachineCenteringRatherThanTextureCoordinates() {
        var recipe = new RecipeLayout(5, 1, 2, 6);
        var machine = new MachineLayout(5, 1, 2, 6, true);
        for (int i = 0; i < 5; i++) {
            var expected = machine.position(i);
            assertEquals(new RecipeLayout.Position(expected.x(), expected.y() - MachineLayout.TOP + RecipeLayout.TOP),
                recipe.slot(false, false, i));
        }
        var expected = machine.position(5 + 1 + 1 + 2);
        assertEquals(new RecipeLayout.Position(expected.x(), expected.y() - MachineLayout.TOP + RecipeLayout.TOP),
            recipe.slot(true, true, 0));
    }
}
