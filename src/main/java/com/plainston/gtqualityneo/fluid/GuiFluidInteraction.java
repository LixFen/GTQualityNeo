package com.plainston.gtqualityneo.fluid;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.FL;
import gregapi.fluid.FluidTankGT;
import gregapi.gui.ContainerCommonBasicMachine;
import gregapi.gui.Slot_Render;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.util.ST;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class GuiFluidInteraction {
    private GuiFluidInteraction() {}

    public static boolean click(ContainerCommonBasicMachine menu, int index, int button, ContainerInput type,
                                net.minecraft.world.entity.player.Player player) {
        if (!GTQualityNeo.GUI_FLUID_INTERACTION.get() || button < 0 || button > 1
            || type != ContainerInput.PICKUP && type != ContainerInput.QUICK_MOVE
            || index < 0 || index >= menu.slots.size() || !(menu.getSlot(index) instanceof Slot_Render slot)
            || !(menu.mTileEntity instanceof MultiTileEntityBasicMachine machine)) return false;
        int fluidIndex = slot.getSlotIndex() - machine.mRecipes.mInputItemsCount - machine.mRecipes.mOutputItemsCount - 1;
        if (slot.container != machine || fluidIndex < 0) return false;
        boolean input = fluidIndex < machine.mTanksInput.length;
        int outputIndex = fluidIndex - machine.mTanksInput.length;
        FluidTankGT tank = input ? machine.mTanksInput[fluidIndex]
            : outputIndex < machine.mTanksOutput.length ? machine.mTanksOutput[outputIndex] : null;
        if (tank == null) return false;
        if (!(player instanceof ServerPlayer) || !menu.stillValid(player)) return true;
        boolean batch = type == ContainerInput.QUICK_MOVE;
        int attempts = batch ? menu.getCarried().getCount() : 1;
        for (int i = 0; i < attempts && !menu.getCarried().isEmpty(); i++) {
            ItemStack held = menu.getCarried();
            ItemStack result = transfer(tank, held.copyWithCount(1), input);
            if (result == null) break;
            if (!batch && held.getCount() == 1) menu.setCarried(result);
            else {
                held.shrink(1);
                if (!result.isEmpty() && !player.getInventory().add(result)) player.drop(result, false);
            }
            machine.markDirtyGUI();
        }
        menu.broadcastChanges();
        return true;
    }

    /** The item handler works on a copy. Change GT6's long tank only after the item transfer succeeds. */
    public static ItemStack transfer(FluidTankGT tank, ItemStack one, boolean input) {
        var temporary = new net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler(1);
        temporary.set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(one), 1);
        ItemAccess access = ItemAccess.forHandlerIndexStrict(temporary, 0);
        var handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler != null) {
            if (input) {
                for (int i = 0; i < handler.size(); i++) {
                    var resource = handler.getResource(i);
                    if (resource.isEmpty()) continue;
                    try (var transaction = Transaction.openRoot()) {
                        int accepted = tank.fill(resource.toStack(handler.getAmountAsInt(i)), FluidAction.SIMULATE);
                        if (accepted > 0 && handler.extract(i, resource, accepted, transaction) == accepted) {
                            tank.fill(resource.toStack(accepted), FluidAction.EXECUTE);
                            transaction.commit();
                            return access.getResource().toStack(access.getAmount());
                        }
                    }
                }
            }
            if (tank.has()) {
                var resource = FluidResource.of(tank.getFluid());
                try (var transaction = Transaction.openRoot()) {
                    int moved = handler.insert(resource, (int) Math.min(Integer.MAX_VALUE, tank.amount()), transaction);
                    if (moved > 0 && tank.drain(moved, FluidAction.SIMULATE).getAmount() == moved) {
                        tank.drain(moved, FluidAction.EXECUTE);
                        transaction.commit();
                        return access.getResource().toStack(access.getAmount());
                    }
                }
            }
            return null;
        }
        // GT6's registered fixed full/empty pairs need not expose an item capability.
        var fluid = FL.getFluid(one.copy(), false);
        if (input && fluid != null && !fluid.isEmpty() && tank.fill(fluid, FluidAction.SIMULATE) == fluid.getAmount()) {
            var result = ST.nn(FL.getEmpty(one.copy(), false));
            tank.fill(fluid, FluidAction.EXECUTE);
            return result;
        }
        var available = tank.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
        if (available == null || available.isEmpty()) return null;
        int before = available.getAmount();
        var result = FL.fill(available, one.copy(), true, false, true, true);
        int moved = before - available.getAmount();
        if (ST.invalid(result) || moved <= 0) return null;
        tank.drain(moved, FluidAction.EXECUTE);
        return result;
    }
}
