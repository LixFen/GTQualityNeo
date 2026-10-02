package com.plainston.gtqualityneo.integration.jade;

import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

/** Client-only presentation of the server snapshot. */
public enum GT6MachineComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final String DATA = "gtqualityneo.machine";

    @Override
    public Identifier getUid() {
        return GT6MachineProvider.INSTANCE.getUid();
    }

    @Override
    public int getDefaultPriority() {
        // Replace the generic inventory section only after Jade has appended it.
        return 1100;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(DATA)) return;
        CompoundTag data = accessor.getServerData().getCompoundOrEmpty(DATA);
        tooltip.remove(JadeIds.UNIVERSAL_ITEM_STORAGE);
        if (data.getBooleanOr("through_part", false)) tooltip.add(tr("controller"));
        String state = data.getStringOr("state", "idle");
        int color = switch (state) {
            case "running" -> 0x55FF55;
            case "stopped" -> 0xFF5555;
            default -> 0xAAAAAA;
        };
        tooltip.add(tr("state").append(": ").append(tr("state." + state).withColor(color)));

        boolean active = data.getBooleanOr("recipe_active", false);
        boolean completed = data.getBooleanOr("recipe_completed", false);
        if (active || completed) {
            long max = Math.max(1, data.getLongOr("max_progress", 0));
            long current = completed && !active ? max : data.getLongOr("progress", 0);
            float ratio = progressRatio(current, max);
            Component text = tr("progress").append(": " + String.format(Locale.ROOT, "%.0f%%", ratio * 100));
            ProgressView view = new ProgressView(ProgressView.Part.of(ratio), text,
                JadeUI.progressStyle(), BoxStyle.nestedBox());
            tooltip.add(JadeUI.progress(view));
            appendItems(tooltip, accessor, data.getListOrEmpty("item_outputs"), "recipe_item_outputs");
            ListTag fluids = data.getListOrEmpty("fluid_outputs");
            if (!fluids.isEmpty()) tooltip.add(tr("recipe_fluid_outputs"));
            for (int i = 0; i < fluids.size(); i++) {
                FluidStack fluid = accessor.decodeFromNbt(FluidStack.STREAM_CODEC, fluids.get(i)).orElse(FluidStack.EMPTY);
                if (fluid.isEmpty()) continue;
                JadeFluidObject object = JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
                tooltip.add(JadeUI.fluid(object));
                tooltip.append(Component.literal(fluid.getAmount() + " mB ").append(object.getDisplayName()));
            }
        }
        appendItems(tooltip, accessor, data.getListOrEmpty("inventory"), "machine_items");
    }

    static float progressRatio(long current, long max) {
        return (float) Math.max(0, Math.min(1, (double) current / Math.max(1, max)));
    }

    private static void appendItems(ITooltip tooltip, BlockAccessor accessor, ListTag items, String title) {
        boolean titled = false;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = accessor.decodeFromNbt(ItemStack.OPTIONAL_STREAM_CODEC, items.get(i)).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            if (!titled) {
                tooltip.add(tr(title));
                titled = true;
            }
            tooltip.add(JadeUI.smallItem(stack));
            tooltip.append(Component.literal(stack.getCount() + " × ").append(stack.getHoverName()));
        }
    }

    private static net.minecraft.network.chat.MutableComponent tr(String key) {
        return Component.translatable("gtqualityneo.jade." + key);
    }
}
