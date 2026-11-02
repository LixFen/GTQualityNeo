// 兼容说明：基于 GT6w alpha.17；未来 GT6w 更新 JEI 注册、配方结构或生成规则后，本文件可能过时，需复核。
package com.plainston.gtqualityneo.integration.jei.cache;

import gregapi.config.ModConfigSpec;
import java.util.LinkedHashSet;
import java.util.Set;

/** Only batches writes made by the current JEI query, never unrelated server work. */
public final class RecipeConfigBatch implements AutoCloseable {
    private static final ThreadLocal<RecipeConfigBatch> CURRENT = new ThreadLocal<>();
    private final Set<ModConfigSpec> pending = new LinkedHashSet<>();
    private final RecipeConfigBatch parent;

    private RecipeConfigBatch() {
        parent = CURRENT.get();
        CURRENT.set(this);
    }

    public static RecipeConfigBatch open() { return new RecipeConfigBatch(); }

    public static boolean defer(ModConfigSpec config) {
        var batch = CURRENT.get();
        if (batch == null) return false;
        batch.pending.add(config);
        return true;
    }

    @Override public void close() {
        if (parent == null) CURRENT.remove();
        else CURRENT.set(parent);
        if (parent != null) parent.pending.addAll(pending);
        else for (var config : pending) config.save();
    }
}
