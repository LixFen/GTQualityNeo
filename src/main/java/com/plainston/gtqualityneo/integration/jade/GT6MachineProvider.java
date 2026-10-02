package com.plainston.gtqualityneo.integration.jade;

import gregapi.data.IL;
import gregapi.tileentity.machines.ITileEntityRunningActively;
import gregapi.tileentity.machines.ITileEntityRunningPassively;
import gregapi.tileentity.machines.ITileEntitySwitchableOnOff;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** Server-only snapshot collection; keep client rendering types out of this class. */
public enum GT6MachineProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final Identifier UID = Identifier.fromNamespaceAndPath("gtqualityneo", "machine_details");
    private static final String DATA = "gtqualityneo.machine";

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        return accessor.getBlockEntity() instanceof MultiTileEntityBasicMachine
            || accessor.getBlockEntity() instanceof MultiTileEntityMultiBlockPart;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity tile = accessor.getBlockEntity();
        boolean throughPart = tile instanceof MultiTileEntityMultiBlockPart;
        if (tile instanceof MultiTileEntityMultiBlockPart part) {
            // Resolve only on the server: the controller can be outside the client's loaded chunks.
            Object controller = part.getTarget(true);
            if (!(controller instanceof BlockEntity target)) return;
            tile = target;
        }
        if (!(tile instanceof MultiTileEntityBasicMachine machine)) return;

        CompoundTag snapshot = new CompoundTag();
        snapshot.putBoolean("through_part", throughPart);
        snapshot.putLong("progress", machine.mProgress);
        snapshot.putLong("max_progress", machine.mMaxProgress);
        boolean active = machine.mMaxProgress > 0;
        boolean completed = machine.mSuccessful && machine.mCurrentRecipe != null;
        snapshot.putBoolean("recipe_active", active);
        snapshot.putBoolean("recipe_completed", completed);
        boolean on = !(tile instanceof ITileEntitySwitchableOnOff switchable) || switchable.getStateOnOff();
        boolean passive = !(tile instanceof ITileEntityRunningPassively running) || running.getStateRunningPassively();
        boolean running = tile instanceof ITileEntityRunningActively working && working.getStateRunningActively();
        String state = !on || !passive ? "stopped"
            : running && (active || completed) ? "running" : active ? "stopped" : "idle";
        snapshot.putString("state", state);

        if (active || completed) {
            // During processing these buffers contain the actual rolled outputs, not recipe previews.
            ItemStack[] items = active ? machine.mOutputItems : machine.mCurrentRecipe.mOutputs;
            FluidStack[] fluids = active ? machine.mOutputFluids : machine.mCurrentRecipe.mFluidOutputs;
            snapshot.put("item_outputs", saveItems(accessor, items));
            ListTag fluidOutputs = new ListTag();
            if (fluids != null) {
                for (FluidStack fluid : fluids) {
                    if (fluid != null && !fluid.isEmpty()) fluidOutputs.add(accessor.encodeAsNbt(FluidStack.STREAM_CODEC, fluid));
                }
            }
            snapshot.put("fluid_outputs", fluidOutputs);
        }

        ListTag inventory = new ListTag();
        for (int slot = 0; slot < machine.getContainerSize(); slot++) {
            ItemStack stack = machine.getItem(slot);
            if (stack == null || stack.isEmpty() || IL.Display_Fluid.equal(stack, true, true)) continue;
            inventory.add(accessor.encodeAsNbt(ItemStack.OPTIONAL_STREAM_CODEC, stack));
        }
        snapshot.put("inventory", inventory);
        data.put(DATA, snapshot);
    }

    private static ListTag saveItems(BlockAccessor accessor, ItemStack[] stacks) {
        ListTag result = new ListTag();
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                if (stack != null && !stack.isEmpty()) result.add(accessor.encodeAsNbt(ItemStack.OPTIONAL_STREAM_CODEC, stack));
            }
        }
        return result;
    }

}
