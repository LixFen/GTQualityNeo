package com.plainston.gtqualityneo.integration.jei.machine;

import gregapi.config.ModConfigSpec;
import java.util.LinkedHashSet;
import java.util.Set;

/** Coalesce GT6 handler saves during one map's generation; config values remain available immediately. */
public final class RecipeConfigBatch implements AutoCloseable {
    private static final ThreadLocal<RecipeConfigBatch> ACTIVE = new ThreadLocal<>();
    private final RecipeConfigBatch parent;
    private final Set<ModConfigSpec> pending = new LinkedHashSet<>();

    private RecipeConfigBatch() {
        parent = ACTIVE.get();
        ACTIVE.set(this);
    }

    public static RecipeConfigBatch begin() { return new RecipeConfigBatch(); }

    /** Called by the save mixin. Other threads and calls outside a generation batch save normally. */
    public static boolean defer(ModConfigSpec config) {
        var batch = ACTIVE.get();
        if (batch == null) return false;
        batch.pending.add(config);
        return true;
    }

    @Override public void close() {
        if (parent != null) {
            ACTIVE.set(parent);
            parent.pending.addAll(pending);
        } else {
            ACTIVE.remove();
            pending.forEach(ModConfigSpec::save);
        }
    }
}
