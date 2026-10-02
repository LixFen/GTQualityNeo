package com.plainston.gtqualityneo.qol;

import com.plainston.gtqualityneo.mixin.MoldAccess;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.oredict.OreDictPrefix;
import gregtech.items.tools.early.GT_Tool_Chisel;
import gregtech.tileentity.tools.MultiTileEntityMold;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.TreeMap;

public final class MoldInteraction {
    private MoldInteraction() {}

    public static boolean isChisel(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MultiItemTool tool
            && tool.getToolStats(stack) instanceof GT_Tool_Chisel;
    }

    public static int shape(MultiTileEntityMold mold) { return ((MoldAccess) mold).gtquality$getShape(); }

    public static boolean canEdit(MultiTileEntityMold mold) {
        return ((MoldAccess) mold).gtquality$getContent() == null && !mold.slotHas(0);
    }

    public static Map<Integer, OreDictPrefix> choices() {
        Map<Integer, OreDictPrefix> result = new TreeMap<>();
        new TreeMap<>(MultiTileEntityMold.MOLD_RECIPES).forEach((id, prefix) -> {
            if (!result.containsValue(prefix)) result.put(id, prefix);
        });
        return result;
    }

    public static void select(ServerPlayer player, BlockPos pos, int selected) {
        if (!isChisel(player.getMainHandItem())
            || !player.isWithinBlockInteractionRange(pos, 0) || !player.level().hasChunkAt(pos)
            || !player.level().mayInteract(player, pos)) return;
        var tile = player.level().getBlockEntity(pos);
        if (tile == null || tile.getClass() != MultiTileEntityMold.class) return;
        var mold = (MultiTileEntityMold) tile;
        if (!canEdit(mold) || selected != 0 && !choices().containsKey(selected)) return;
        ((MoldAccess) mold).gtquality$setShape(selected);
        mold.setChanged();
        mold.updateClientData();
        var data = player.getPersistentData();
        var persisted = data.getCompoundOrEmpty(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        persisted.putInt("gtqualityneo.lastMoldShape", selected);
        data.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
    }
}
