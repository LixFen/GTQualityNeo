package com.plainston.gtqualityneo.integration.jei;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecipeIngredientCacheTest {
    @Test void extractsOncePerRecipeAndInvalidatesTheWholeVersionOnMutationOrReload() {
        var cache = new RecipeIngredientCache<Object, List<String>>();
        var first = new Object();
        var second = new Object();
        var links = List.of("input", "output");
        cache.put(first, links, 1, 1);
        cache.put(second, links, 1, 1);
        assertSame(links, cache.get(first, 1, 1));
        assertSame(links, cache.get(second, 1, 1));
        assertNull(cache.get(first, 2, 1));
        assertNull(cache.get(second, 2, 1));
        cache.put(first, links, 2, 1);
        assertNull(cache.get(first, 2, 2));
        cache.put(first, links, 2, 2);
        assertSame(links, cache.get(first, 2, 2));
    }
}
