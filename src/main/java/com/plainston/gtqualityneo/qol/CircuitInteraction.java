package com.plainston.gtqualityneo.qol;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.item.ItemIntegratedCircuit;
import gregapi.util.ST;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CircuitInteraction {
    private CircuitInteraction() {}
    public static boolean isCircuit(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemIntegratedCircuit;
    }
    private static boolean open(PlayerInteractEvent event) {
        var player = event.getEntity();
        if (!GTQualityNeo.CIRCUIT_SELECTOR.get() || event.getHand() != InteractionHand.MAIN_HAND
            || !player.isShiftKeyDown() || !isCircuit(player.getMainHandItem())) return false;
        if (player instanceof ServerPlayer serverPlayer)
            PacketDistributor.sendToPlayer(serverPlayer, new QolNetwork.OpenCircuit(
                player.getInventory().getSelectedSlot(), ST.meta_(player.getMainHandItem())));
        return true;
    }
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (open(event)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (open(event)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
    public static boolean select(ServerPlayer player, int slot, int original, int number) {
        if (!GTQualityNeo.CIRCUIT_SELECTOR.get() || !player.isAlive() || slot < 0 || slot > 8
            || player.getInventory().getSelectedSlot() != slot || number < 0 || number > 24) return false;
        var stack = player.getMainHandItem();
        if (!isCircuit(stack) || ST.meta_(stack) != original) return false;
        ST.meta_(stack, (original & ~255) | number);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }
}
