package com.plainston.gtqualityneo.integration.jei;

import gregapi.config.ModConfigSpec;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class RecipeConfigBatchTest {
    @Test void nestedQueriesFlushEachFileOnceAndKeepOtherThreadsIndependent() throws InterruptedException {
        var config = new CountingConfig();
        var otherThreadDeferred = new AtomicBoolean(true);
        try (var outer = RecipeConfigBatch.open()) {
            assertTrue(RecipeConfigBatch.defer(config));
            try (var inner = RecipeConfigBatch.open()) {
                assertTrue(RecipeConfigBatch.defer(config));
                assertTrue(RecipeConfigBatch.defer(config));
            }
            assertEquals(0, config.writes);
            var thread = Thread.startVirtualThread(() -> otherThreadDeferred.set(RecipeConfigBatch.defer(config)));
            thread.join();
            assertFalse(otherThreadDeferred.get());
        }
        assertEquals(1, config.writes);
        assertFalse(RecipeConfigBatch.defer(config));
    }

    @Test void failedQueryStillFlushesAndRestoresImmediateSaving() {
        var config = new CountingConfig();
        assertThrows(IllegalStateException.class, () -> {
            try (var batch = RecipeConfigBatch.open()) {
                assertTrue(RecipeConfigBatch.defer(config));
                throw new IllegalStateException("generation failed");
            }
        });
        assertEquals(1, config.writes);
        assertFalse(RecipeConfigBatch.defer(config));
    }

    private static final class CountingConfig extends ModConfigSpec {
        private int writes;
        CountingConfig() { super(null); }
        @Override public void save() { writes++; }
    }
}
