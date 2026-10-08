package com.plainston.gtqualityneo.qol;

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

    public static int shape(MultiTileEntityMold mold) {
        return mold.writeItemNBT2(new net.minecraft.nbt.CompoundTag()).getIntOr("gt.mold", 0);
    }

    public static boolean canEdit(MultiTileEntityMold mold) {
        var saved = new net.minecraft.nbt.CompoundTag();
        mold.writeToNBT2(saved);
        return gregapi.oredict.OreDictMaterialStack.load(gregapi.data.CS.NBT_MATERIALS, saved).mAmount <= 0 && !mold.slotHas(0);
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
        setShape(mold, selected);
        mold.setChanged();
        mold.updateClientData();
        var data = player.getPersistentData();
        var persisted = data.getCompoundOrEmpty(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        persisted.putInt("gtqualityneo.lastMoldShape", selected);
        data.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
    }

    public static void setShape(MultiTileEntityMold mold, int selected) {
        // Read and write the complete public persistence state so temperature, materials and controls survive.
        var saved = new net.minecraft.nbt.CompoundTag();
        mold.writeToNBT2(saved);
        saved.putInt("gt.mold", selected);
        mold.readFromNBT2(saved);
    }
}
