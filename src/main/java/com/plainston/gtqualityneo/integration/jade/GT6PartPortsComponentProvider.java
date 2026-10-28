package com.plainston.gtqualityneo.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum GT6PartPortsComponentProvider implements IBlockComponentProvider {
    INSTANCE;
    @Override public Identifier getUid() { return GT6PartPortsProvider.UID; }
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.showDetails() || !accessor.getServerData().contains(GT6PartPortsProvider.DATA)
            || accessor.getHitResult() == null) return;
        append(tooltip, accessor.getServerData().getCompoundOrEmpty(GT6PartPortsProvider.DATA),
            accessor.getHitResult().getDirection().get3DDataValue());
    }
    static void append(ITooltip tooltip, CompoundTag data, int side) {
        if (side < 0 || side > 5) return;
        int bit = 1 << side;
        for (String direction : new String[] {"input", "output"}) {
            var types = data.getCompoundOrEmpty("gtquality.part.energy_" + direction + "_types").getListOrEmpty(Integer.toString(side));
            if (types.isEmpty()) continue;
            Component names = Component.empty();
            for (int i = 0; i < types.size(); i++) {
                if (i > 0) names.getSiblings().add(Component.literal(", "));
                names.getSiblings().add(Component.translatable(types.getStringOr(i, "")));
            }
            tooltip.add(Component.translatable("gtqualityneo.jade.part_energy_" + direction, names));
            if (direction.equals("output") && (data.getIntOr("gtquality.part.energy_auto_output_sides", 0) & bit) != 0)
                tooltip.add(Component.translatable("gtqualityneo.jade.part_energy_active_output", names));
        }
        for (String resource : new String[] {"item", "fluid"}) {
            String prefix = "gtquality.part." + resource;
            if ((data.getIntOr(prefix + "_input_sides", 0) & bit) != 0)
                tooltip.add(Component.translatable("gtqualityneo.jade.part_" + resource + "_input"));
            if ((data.getIntOr(prefix + "_output_sides", 0) & bit) == 0) continue;
            tooltip.add(Component.translatable("gtqualityneo.jade.part_" + resource + "_output"));
            if ((data.getIntOr(prefix + "_auto_output_sides", 0) & bit) != 0)
                tooltip.add(Component.translatable("gtqualityneo.jade.part_" + resource + "_active_output"));
            else if ((data.getIntOr(prefix + "_auto_output_disabled_sides", 0) & bit) != 0)
                tooltip.add(Component.translatable("gtqualityneo.jade.part_" + resource + "_auto_output_disabled"));
        }
    }
}
