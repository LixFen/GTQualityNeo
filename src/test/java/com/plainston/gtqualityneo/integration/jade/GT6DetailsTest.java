package com.plainston.gtqualityneo.integration.jade;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import gregapi.fluid.FluidTankGT;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregtech.tileentity.multiblocks.MultiTileEntityCrucible;
import gregtech.tileentity.tanks.MultiTileEntityBarrelMetal;
import gregtech.tileentity.tools.MultiTileEntityMold;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import snownee.jade.api.BlockAccessor;

@ExtendWith(EphemeralTestServerProvider.class)
class GT6DetailsTest {
    private static MinecraftServer server;

    @BeforeAll static void initializeComponents(MinecraftServer testServer) {
        server = testServer;
    }

    @Test void moldSnapshotPreservesTemperatureDirectionsAndRedstoneMode() {
        var mold = new MultiTileEntityMold();
        mold.setLevel(server.overworld());
        CompoundTag saved = new CompoundTag();
        saved.putLong("gt.temperature", 70000);
        saved.putInt("gt.mold", 1);
        saved.putByte("gt.connection", (byte) 12);
        saved.putBoolean("gt.mode", true);
        mold.readFromNBT2(saved);
        CompoundTag data = new CompoundTag();
        GT6DetailsProvider.INSTANCE.appendServerData(data, accessor(mold));
        CompoundTag snapshot = data.getCompoundOrEmpty(GT6DetailsProvider.DATA);
        assertEquals("MultiTileEntityMold", snapshot.getStringOr("kind", ""));
        assertEquals(70000, snapshot.getLongOr("gt.temperature", 0L));
        assertEquals(12, snapshot.getByteOr("gt.connection", (byte) 0));
        assertTrue(snapshot.getBooleanOr("gt.mode", false));
        assertFalse(snapshot.getStringOr("mold_prefix", "").isEmpty());
        assertFalse(snapshot.contains("id"));
    }

    @Test void multiblockSnapshotDoesNotReadControllerThroughClientWorld() {
        var controller = new MultiTileEntityCrucible();
        controller.setLevel(server.overworld());
        var part = new MultiTileEntityMultiBlockPart() {
            @Override public ITileEntityMultiBlockController getTarget(boolean check) { return controller; }
        };
        CompoundTag data = new CompoundTag();
        GT6DetailsProvider.INSTANCE.appendServerData(data, accessor(part));
        CompoundTag snapshot = data.getCompoundOrEmpty(GT6DetailsProvider.DATA);
        assertEquals("MultiTileEntityCrucible", snapshot.getStringOr("kind", ""));
        assertTrue(snapshot.getBooleanOr("through_part", false));
    }

    @Test void unboundMultiblockPartDoesNotSendStaleDetails() {
        var part = new MultiTileEntityMultiBlockPart();
        CompoundTag data = new CompoundTag();
        GT6DetailsProvider.INSTANCE.appendServerData(data, accessor(part));
        assertFalse(data.contains(GT6DetailsProvider.DATA));
    }

    @Test void jadeFluidViewKeepsQuantitiesBeyondIntegerRange() {
        long amount = (long) Integer.MAX_VALUE + 5000;
        long capacity = amount * 2;
        var barrel = new MultiTileEntityBarrelMetal();
        barrel.mTank = new FluidTankGT(new FluidStack(Fluids.WATER, 1), amount, capacity);
        var groups = GT6FluidStorageProvider.INSTANCE.getGroups(accessor(barrel));
        assertNotNull(groups);
        var view = groups.getFirst().views.getFirst();
        assertEquals(amount, view.fluids().getFirst().getAmount());
        assertEquals(capacity, view.capacity());
    }

    private static BlockAccessor accessor(BlockEntity tile) {
        return (BlockAccessor) Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(), new Class<?>[] { BlockAccessor.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "getBlockEntity", "getTarget" -> tile;
                default -> throw new AssertionError("Unexpected accessor call: " + method.getName());
            });
    }
}
