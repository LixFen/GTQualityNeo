package com.plainston.gtqualityneo.integration.jade;

import gregapi.data.CS;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.util.WD;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Complements GT6's native Jade tool icons and tier with the WDMla held-tool semantics. */
public enum GT6HarvestProvider implements IBlockComponentProvider {
    INSTANCE;
    private static final Identifier UID = Identifier.fromNamespaceAndPath("gtqualityneo", "harvest");
    @Override public Identifier getUid() { return UID; }
    @Override public int getDefaultPriority() { return 8100; }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var block = accessor.getBlockState().getBlock();
        if (!MD.GT.owns(block) && !MD.GAPI.owns(block)) return;
        var pos = accessor.getPosition();
        int meta = WD.meta(accessor.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        String tool = WD.harvestTool(block, meta);
        if (tool != null && !tool.isEmpty()) {
            tooltip.add(Component.translatable("gtqualityneo.jade.effective_tool")
                .append(": " + LH.get(CS.TOOL_LOCALISER_PREFIX + tool, tool)));
        }
        var held = accessor.getPlayer().getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof MultiItemTool item) {
            boolean effective = item.getDigSpeed(held.copy(), block, meta) > 0;
            boolean harvestable = WD.getMaterial(block).isToolNotRequired() || effective;
            addResult(tooltip, "held_tool_effective", effective && harvestable);
            addResult(tooltip, "harvestable", harvestable);
        }
    }

    private static void addResult(ITooltip tooltip, String key, boolean success) {
        tooltip.add(Component.translatable("gtqualityneo.jade." + key).append(": ")
            .append(Component.literal(success ? "✔" : "✕").withColor(success ? 0x55FF55 : 0xFF5555)));
    }
}
