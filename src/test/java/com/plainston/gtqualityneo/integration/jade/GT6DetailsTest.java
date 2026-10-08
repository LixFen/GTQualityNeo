package com.plainston.gtqualityneo.integration.jade;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import gregapi.data.MT;
import gregapi.fluid.FluidTankGT;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregtech.tileentity.multiblocks.MultiTileEntityCrucible;
import gregtech.tileentity.tanks.MultiTileEntityBarrelMetal;
import gregtech.tileentity.tools.MultiTileEntityMold;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;

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

    @Test void controllerHeadingIsHiddenOnBothControllerAndParts() {
        for (boolean throughPart : List.of(false, true)) {
            var snapshot = new CompoundTag();
            snapshot.putBoolean("through_part", throughPart);
            snapshot.putString("kind", "MultiTileEntityMultiBlockPart");
            var data = new CompoundTag();
            data.put(GT6DetailsProvider.DATA, snapshot);
            data.put("gtqualityneo.machine", snapshot);
            var client = (BlockAccessor) Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(), new Class<?>[]{BlockAccessor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getServerData")) return data;
                    throw new AssertionError(method.getName());
                });
            for (snownee.jade.api.IBlockComponentProvider provider : List.of(GT6MachineComponentProvider.INSTANCE, GT6DetailsComponentProvider.INSTANCE)) {
                var lines = new ArrayList<Component>();
                var tooltip = (ITooltip) Proxy.newProxyInstance(ITooltip.class.getClassLoader(), new Class<?>[]{ITooltip.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("add") && args[0] instanceof Component line) lines.add(line);
                        return method.getReturnType() == boolean.class ? false : null;
                    });
                provider.appendTooltip(tooltip, client, null);
                long headings = lines.stream().filter(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    && text.getKey().equals("gtqualityneo.jade.controller")).count();
                assertEquals(0, headings, provider.getClass().getSimpleName());
                if (provider == GT6MachineComponentProvider.INSTANCE) assertFalse(lines.isEmpty(), "Machine state remains visible");
            }
        }
    }

    @Test void directControllersRequestTheSameServerSnapshotAsTheirParts() {
        for (var controller : List.of(new MultiTileEntityCrucible(), new gregtech.tileentity.multiblocks.MultiTileEntityCokeOven(),
            new gregtech.tileentity.multiblocks.MultiTileEntityTank3x3x3Metal())) {
            controller.setLevel(server.overworld());
            assertTrue(GT6DetailsProvider.INSTANCE.shouldRequestData(accessor(controller)), controller.getClass().getSimpleName());
            var part = new MultiTileEntityMultiBlockPart() {
                @Override public ITileEntityMultiBlockController getTarget(boolean check) { return controller; }
            };
            var direct = new CompoundTag();
            var indirect = new CompoundTag();
            GT6DetailsProvider.INSTANCE.appendServerData(direct, accessor(controller));
            GT6DetailsProvider.INSTANCE.appendServerData(indirect, accessor(part));
            var first = direct.getCompoundOrEmpty(GT6DetailsProvider.DATA);
            var second = indirect.getCompoundOrEmpty(GT6DetailsProvider.DATA);
            assertFalse(first.isEmpty());
            assertFalse(first.getBooleanOr("through_part", true));
            assertTrue(second.getBooleanOr("through_part", false));
            first.remove("through_part");
            second.remove("through_part");
            assertEquals(first, second);
        }
    }

    @Test void tankControllerHudReadsNativeTankEvenWithoutAnExposedCapability() {
        var controller = new gregtech.tileentity.multiblocks.MultiTileEntityTank3x3x3Metal();
        long amount = (long) Integer.MAX_VALUE + 10000;
        controller.mTank = new FluidTankGT(new FluidStack(Fluids.WATER, 1), amount, amount * 2);
        var groups = GT6FluidStorageProvider.INSTANCE.getGroups(accessor(controller));
        assertNotNull(groups);
        assertEquals(amount, groups.getFirst().views.getFirst().fluids().getFirst().getAmount());
        assertEquals(amount * 2, groups.getFirst().views.getFirst().capacity());
    }

    @Test void emptyTankControllerStillRequestsAndSendsItsCapacityDirectly() {
        var controller = new gregtech.tileentity.multiblocks.MultiTileEntityTank3x3x3Metal();
        controller.setLevel(server.overworld());
        controller.mTank = new FluidTankGT(8000);
        var data = new CompoundTag();
        GT6DetailsProvider.INSTANCE.appendServerData(data, accessor(controller));
        assertTrue(GT6DetailsProvider.INSTANCE.shouldRequestData(accessor(controller)));
        var snapshot = data.getCompoundOrEmpty(GT6DetailsProvider.DATA);
        assertEquals("MultiTileEntityTank", snapshot.getStringOr("kind", ""));
        assertEquals(8000, snapshot.getLongOr("gtquality.capacity", 0));
        assertFalse(snapshot.getBooleanOr("through_part", true));
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

    @Test void crucibleContentsShowMaterialNameInsteadOfTranslationKey() throws Exception {
        CompoundTag material = new CompoundTag();
        material.putShort("i", MT.Fe.mID);
        material.putLong("a", 648648000L);
        CompoundTag materials = new CompoundTag();
        materials.put("0", material);
        List<String> lines = new ArrayList<>();
        ITooltip tooltip = (ITooltip) Proxy.newProxyInstance(ITooltip.class.getClassLoader(), new Class<?>[] { ITooltip.class },
            (proxy, method, args) -> {
                if (!method.getName().equals("add") || !(args[0] instanceof Component line))
                    throw new AssertionError("Unexpected tooltip call: " + method.getName());
                lines.add(line.getString());
                return null;
            });
        var appendMaterials = GT6DetailsComponentProvider.class.getDeclaredMethod("appendMaterials", ITooltip.class, CompoundTag.class);
        appendMaterials.setAccessible(true);
        appendMaterials.invoke(null, tooltip, materials);
        assertEquals(1, lines.size());
        assertTrue(lines.getFirst().endsWith("1.000 Iron"), lines.getFirst());
        assertFalse(lines.getFirst().contains("gt.material."));
    }

    private static BlockAccessor accessor(BlockEntity tile) {
        return (BlockAccessor) Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(), new Class<?>[] { BlockAccessor.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "getBlockEntity", "getTarget" -> tile;
                default -> throw new AssertionError("Unexpected accessor call: " + method.getName());
            });
    }
}
