package com.plainston.gtqualityneo.tools;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.TD;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.util.ST;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

public final class DrillMining {
    private static final ThreadLocal<Boolean> HARVESTING = ThreadLocal.withInitial(() -> false);
    public record Context(ItemStack stack, int side, int depth, float hardness) {}
    private DrillMining() {}

    public static Context capture(ServerPlayer player, BlockPos pos) {
        if (HARVESTING.get() || !GTQualityNeo.DRILL_MODES.get() || player.isShiftKeyDown()) return null;
        var stack = player.getMainHandItem();
        int depth = DrillModes.depth(DrillInteraction.tier(stack), DrillInteraction.mode(stack));
        if (depth == 0 || (ST.meta_(stack) & 1) != 0) return null;
        var state = player.level().getBlockState(pos);
        var tool = (MultiItemTool) stack.getItem();
        if (!tool.getToolStats(stack).isMinableBlock(state.getBlock(), WD.meta(state))
            || tool.getDigSpeed(stack, state.getBlock(), WD.meta(state)) <= 0) return null;
        var eyes = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eyes,
            eyes.add(player.getLookAngle().scale(player.blockInteractionRange())), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) return null;
        float hardness = state.getDestroySpeed(player.level(), pos);
        return hardness < 0 ? null : new Context(stack, hit.getDirection().get3DDataValue(), depth, hardness);
    }
    public static void harvest(ServerPlayer player, BlockPos origin, Context context) {
        if (context == null) return;
        HARVESTING.set(true);
        try {
            var stack = context.stack();
            var tool = (MultiItemTool) stack.getItem();
            for (int[] offset : DrillModes.offsets(context.side(), context.depth())) {
                if (player.getMainHandItem() != stack || stack.isEmpty() || (ST.meta_(stack) & 1) != 0
                    || MultiItemTool.getToolDamage(stack) >= MultiItemTool.getToolMaxDamage(stack)) break;
                var pos = origin.offset(offset[0], offset[1], offset[2]);
                if (!player.level().hasChunkAt(pos) || !player.level().mayInteract(player, pos)) continue;
                var state = player.level().getBlockState(pos);
                float hardness = state.getDestroySpeed(player.level(), pos);
                if (state.isAir() || !state.getFluidState().isEmpty() || hardness < 0 || hardness > context.hardness() + 8
                    || !tool.getToolStats(stack).isMinableBlock(state.getBlock(), WD.meta(state))
                    || tool.getDigSpeed(stack, state.getBlock(), WD.meta(state)) <= 0) continue;
                long cost = (long) Math.ceil(tool.getToolStats(stack).getToolDamagePerBlockBreak() * hardness);
                if (!player.isCreative() && !tool.useEnergy(TD.Energy.EU, stack, cost, player, null, player.level(),
                    pos.getX(), pos.getY(), pos.getZ(), false)) break;
                // The normal server path enforces break events, protection, drops and tool wear for every block.
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            HARVESTING.remove();
        }
    }
}
