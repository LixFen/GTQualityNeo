package com.plainston.gtqualityneo.tools;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.CS;
import gregapi.item.IItemGT;
import gregapi.util.ST;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class DrillInteraction {
    private static final String MODE = "gtqualityneo.drill_mode";
    private DrillInteraction() {}

    public static int tier(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != CS.ToolsGT.sMetaTool) return 0;
        int id = ST.meta_(stack) & ~1;
        if (id == CS.ToolsGT.MININGDRILL_LV) return 1;
        if (id == CS.ToolsGT.MININGDRILL_MV) return 2;
        return id == CS.ToolsGT.MININGDRILL_HV ? 3 : 0;
    }
    public static int mode(ItemStack stack) {
        return DrillModes.normalize(tier(stack), stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag().getIntOr(MODE, 0));
    }
    public static Component label(ItemStack stack) {
        int depth = DrillModes.depth(tier(stack), mode(stack));
        return depth == 0 ? Component.translatable("gtqualityneo.drill.single") : Component.literal("3×3×" + depth);
    }
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var stack = player.getMainHandItem();
        if (!GTQualityNeo.DRILL_MODES.get() || event.getHand() != InteractionHand.MAIN_HAND
            || !player.isShiftKeyDown() || tier(stack) == 0) return;
        // Match GTQuality and GT6's Behavior_Switch_Metadata with mCheckTarget=true:
        // GT item blocks allow switching even with a block entity; other block entities retain their interaction.
        if (!(player.level().getBlockState(event.getPos()).getBlock() instanceof IItemGT)
            && player.level().getBlockEntity(event.getPos()) != null) return;
        if (!player.level().isClientSide()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE, DrillModes.next(tier(stack), mode(stack))));
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.sendOverlayMessage(Component.translatable("gtqualityneo.drill.mode", label(stack)));
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
