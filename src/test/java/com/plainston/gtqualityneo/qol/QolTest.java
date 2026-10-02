package com.plainston.gtqualityneo.qol;

import static org.junit.jupiter.api.Assertions.*;
import com.plainston.gtqualityneo.fluid.CreativeTank;
import com.plainston.gtqualityneo.fluid.GuiFluidInteraction;
import gregapi.fluid.FluidTankGT;
import gregtech.tileentity.tools.MultiTileEntityMold;
import gregtech.tileentity.tools.MultiTileEntitySapBag;
import gregtech.tileentity.tools.MultiTileEntityScaffold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.ArrayList;

@ExtendWith(EphemeralTestServerProvider.class)
class QolTest {
    private static MinecraftServer server;
    @BeforeAll static void initialize(MinecraftServer instance) throws Exception {
        server = instance;
        // NeoForge's ephemeral server loads registries, but deliberately leaves levels/capabilities uninitialized.
        if (net.neoforged.neoforge.transfer.access.ItemAccess.forStack(new ItemStack(Items.WATER_BUCKET))
            .getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM) == null)
        {
            var constructor = net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            net.neoforged.neoforge.capabilities.CapabilityHooks.registerFallbackVanillaProviders(constructor.newInstance());
        }
        if (server.overworld() == null) {
            server.getWorldData().overworldData().setInitialized(true);
            var createLevels = MinecraftServer.class.getDeclaredMethod("createLevels");
            createLevels.setAccessible(true);
            server.submit(() -> {
                try { createLevels.invoke(server); }
                catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
            }).get();
        }
    }

    @Test void bucketDepositsAndWithdrawsWithoutMutatingOriginalItem() {
        var tank = new FluidTankGT(2000);
        var full = new ItemStack(Items.WATER_BUCKET);
        var empty = GuiFluidInteraction.transfer(tank, full, true);
        assertNotNull(empty);
        assertTrue(empty.is(Items.BUCKET));
        assertEquals(1000, tank.amount());
        assertTrue(full.is(Items.WATER_BUCKET));
        var refilled = GuiFluidInteraction.transfer(tank, empty, false);
        assertTrue(refilled.is(Items.WATER_BUCKET));
        assertEquals(0, tank.amount());
    }

    @Test void partialBucketDepositRollsBackTankAndContainer() {
        var tank = new FluidTankGT(500);
        assertNull(GuiFluidInteraction.transfer(tank, new ItemStack(Items.WATER_BUCKET), true));
        assertEquals(0, tank.amount());
    }

    @Test void guiWithdrawPreservesLongTankAmount() {
        long original = (long) Integer.MAX_VALUE + 5000;
        var tank = new FluidTankGT(new FluidStack(Fluids.WATER, 1), original, original * 2);
        assertTrue(GuiFluidInteraction.transfer(tank, new ItemStack(Items.BUCKET), false).is(Items.WATER_BUCKET));
        assertEquals(original - 1000, tank.amount());
    }

    @Test void outputTankCannotReceiveFluid() {
        var tank = new FluidTankGT(2000);
        assertNull(GuiFluidInteraction.transfer(tank, new ItemStack(Items.WATER_BUCKET), false));
        assertEquals(0, tank.amount());
    }

    @Test void incompatibleFluidDoesNotChangeTank() {
        var tank = new FluidTankGT(new FluidStack(Fluids.LAVA, 1000), 2000);
        assertNull(GuiFluidInteraction.transfer(tank, new ItemStack(Items.WATER_BUCKET), true));
        assertEquals(1000, tank.amount());
        assertEquals(Fluids.LAVA, tank.getFluid().getFluid());
    }

    @Test void creativeTankSimulationAndCommittedExtractionShareOneTickBudget() {
        var tank = new CreativeTank(BlockPos.ZERO, CreativeTank.BLOCK.get().defaultBlockState());
        tank.setLevel(server.overworld());
        tank.configure(new FluidStack(Fluids.WATER, 1), false, 1000);
        var water = FluidResource.of(Fluids.WATER);
        try (var transaction = Transaction.openRoot()) {
            assertEquals(700, tank.handler.extract(water, 700, transaction));
        }
        try (var transaction = Transaction.openRoot()) {
            assertEquals(800, tank.handler.extract(water, 800, transaction));
            transaction.commit();
        }
        try (var transaction = Transaction.openRoot()) {
            assertEquals(200, tank.handler.extract(water, 800, transaction));
            assertEquals(0, tank.handler.extract(water, 1, transaction));
            transaction.commit();
        }
    }

    @Test void zeroRateStopsCreativeTankAndFluidComponentsRemainDistinct() {
        var tank = new CreativeTank(BlockPos.ZERO, CreativeTank.BLOCK.get().defaultBlockState());
        tank.setLevel(server.overworld());
        var fluid = new FluidStack(Fluids.WATER, 1000);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("test water"));
        tank.configure(fluid, true, 0);
        assertEquals(1, tank.settings().fluid().getAmount());
        assertEquals(fluid.get(DataComponents.CUSTOM_NAME), tank.settings().fluid().get(DataComponents.CUSTOM_NAME));
        try (var transaction = Transaction.openRoot()) {
            assertEquals(0, tank.handler.extract(FluidResource.of(fluid), 1000, transaction));
        }
        tank.configure(fluid, false, 1000);
        try (var transaction = Transaction.openRoot()) {
            assertEquals(0, tank.handler.extract(FluidResource.of(Fluids.WATER), 1000, transaction));
            assertEquals(1000, tank.handler.extract(FluidResource.of(fluid), 1000, transaction));
        }
    }

    @Test void automaticOutputAndPipeExtractionShareBudgetAfterPartialAcceptance() throws Exception {
        var world = server.overworld();
        var pos = new BlockPos(8, 64, 8);
        var neighbor = pos.above();
        world.setBlock(neighbor, Blocks.OBSIDIAN.defaultBlockState(), 3);
        var sink = new net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler(1, 400);
        var constructor = net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance().registerBlock(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
            (level, position, state, tile, side) -> position.equals(neighbor) ? sink : null, Blocks.OBSIDIAN);
        var tank = new CreativeTank(pos, CreativeTank.BLOCK.get().defaultBlockState());
        tank.setLevel(world);
        tank.configure(new FluidStack(Fluids.WATER, 1), true, 1000);
        assertTrue(world.hasChunkAt(neighbor), "Fixture neighbor chunk must be loaded");
        assertSame(sink, world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
            neighbor, net.minecraft.core.Direction.DOWN));
        CreativeTank.tick(world, pos, tank.getBlockState(), tank);
        assertEquals(400, sink.getAmountAsInt(0));
        try (var transaction = Transaction.openRoot()) {
            assertEquals(600, tank.handler.extract(FluidResource.of(Fluids.WATER), 1000, transaction));
            assertEquals(0, tank.handler.extract(FluidResource.of(Fluids.WATER), 1, transaction));
            transaction.commit();
        }
    }

    @Test void storageFormConversionPreservesMaterialRemainderAndCustomName() {
        var storage = new gregtech.tileentity.inventories.MultiTileEntityMassStorageBarrel();
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putShort("gt.invsize", (short) 3);
        storage.readFromNBT2(tag);
        storage.setLevel(server.overworld());
        var ingots = gregapi.data.OP.ingot.mat(gregapi.data.MT.Fe, 19);
        assertNotNull(ingots);
        assertTrue(gregapi.util.ST.valid(ingots));
        ingots.set(DataComponents.CUSTOM_NAME, Component.literal("named iron"));
        storage.slot(1, ingots);
        storage.mPartialUnits = gregapi.data.CS.U / 2;
        long before = 19 * gregapi.data.CS.U + storage.mPartialUnits;
        assertEquals(100, storage.onToolClick2(gregapi.data.CS.TOOL_chisel, 10000, 1, null,
            new ArrayList<>(), null, false, ItemStack.EMPTY, (byte) 2, 0.5F, 0.5F, 0.5F));
        var data = gregapi.util.OM.anydata_(storage.slot(1));
        assertSame(gregapi.data.OP.blockIngot, data.mPrefix);
        assertEquals(before, storage.getUnitAmount(data.mPrefix) * storage.slot(1).getCount() + storage.mPartialUnits);
        assertEquals(Component.literal("named iron"), storage.slot(1).get(DataComponents.CUSTOM_NAME));
    }

    @Test void hopperShapeIncludesSpoutAndLeavesLowerCornersEmpty() {
        var hopper = new gregtech.tileentity.inventories.MultiTileEntityHopper();
        hopper.mFacing = 0;
        var boxes = CollisionShapes.hopper(hopper).toAabbs();
        assertTrue(boxes.stream().anyMatch(box -> box.contains(0.5, 0.1, 0.5)));
        assertTrue(boxes.stream().anyMatch(box -> box.contains(0.1, 0.9, 0.1)));
        assertFalse(boxes.stream().anyMatch(box -> box.contains(0.1, 0.1, 0.1)));
    }

    @Test void creativeTankSettingsSurviveBlockEntityAndDroppedItemComponents() {
        var tank = new CreativeTank(BlockPos.ZERO, CreativeTank.BLOCK.get().defaultBlockState());
        tank.setLevel(server.overworld());
        var fluid = new FluidStack(Fluids.LAVA, 1);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("component lava"));
        tank.configure(fluid, true, Integer.MAX_VALUE);
        var saved = tank.saveWithoutMetadata(server.registryAccess());
        var restored = new CreativeTank(BlockPos.ZERO, CreativeTank.BLOCK.get().defaultBlockState());
        restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
            net.minecraft.util.ProblemReporter.DISCARDING, server.registryAccess(), saved));
        assertEquals(Integer.MAX_VALUE, restored.settings().rate());
        assertTrue(restored.settings().automatic());
        assertTrue(FluidStack.isSameFluidSameComponents(tank.settings().fluid(), restored.settings().fluid()));
        var dropped = new ItemStack(CreativeTank.ITEM.get());
        dropped.applyComponents(tank.collectComponents());
        var placed = new CreativeTank(BlockPos.ZERO, CreativeTank.BLOCK.get().defaultBlockState());
        placed.applyComponentsFromItemStack(dropped);
        assertEquals(Integer.MAX_VALUE, placed.settings().rate());
        assertTrue(FluidStack.isSameFluidSameComponents(tank.settings().fluid(), placed.settings().fluid()));
    }

    @Test void moldAccessorAndEmptyCheckWorkOnTransformedGT6Class() {
        var mold = new MultiTileEntityMold();
        mold.readFromNBT2(new net.minecraft.nbt.CompoundTag());
        assertTrue(MoldInteraction.canEdit(mold));
        assertEquals(0, MoldInteraction.shape(mold));
        mold.slot(0, new ItemStack(Items.IRON_INGOT));
        assertFalse(MoldInteraction.canEdit(mold));
    }

    @Test void sapBagExposesOnlyOutputSlot() {
        var bag = new MultiTileEntitySapBag();
        for (byte side = 0; side < 6; side++) {
            assertArrayEquals(new int[]{0}, bag.getAccessibleSlotsFromSide2(side));
            assertTrue(bag.canExtractItem2(0, new ItemStack(Items.SLIME_BALL), side));
            assertFalse(bag.canExtractItem2(1, new ItemStack(Items.SLIME_BALL), side));
        }
    }

    @Test void sapBagExtractionCanBeDisabled() {
        var setting = com.plainston.gtqualityneo.GTQualityNeo.SAP_BAG_EXTRACTION;
        boolean previous = setting.get();
        setting.set(false);
        try {
            var bag = new MultiTileEntitySapBag();
            assertEquals(0, bag.getAccessibleSlotsFromSide2((byte) 2).length);
            assertFalse(bag.canExtractItem2(0, new ItemStack(Items.SLIME_BALL), (byte) 2));
        } finally { setting.set(previous); }
    }

    @Test void obstructionOptionCanFallBackToGT6Behavior() {
        var world = server.overworld();
        world.setBlock(new BlockPos(0, 70, 1), Blocks.STONE.defaultBlockState(), 3);
        var setting = com.plainston.gtqualityneo.GTQualityNeo.OBSTRUCTED_INTERACTION;
        boolean previous = setting.get(), gtPrevious = gregapi.data.CS.OBSTRUCTION_CHECKS;
        try {
            gregapi.data.CS.OBSTRUCTION_CHECKS = true;
            setting.set(false);
            assertTrue(gregapi.util.WD.obstructed(world, 0, 70, 0, (byte) 3));
            setting.set(true);
            assertFalse(gregapi.util.WD.obstructed(world, 0, 70, 0, (byte) 3));
        } finally {
            setting.set(previous);
            gregapi.data.CS.OBSTRUCTION_CHECKS = gtPrevious;
        }
    }

    @Test void scaffoldRemovesFarPostsAndKeepsNearPosts() {
        var scaffold = new MultiTileEntityScaffold();
        scaffold.setLevel(server.overworld());
        scaffold.setVisualData((byte) 1);
        scaffold.mFacing = 2;
        var collisions = new ArrayList<AABB>();
        scaffold.addCollisionBoxesToList2(new AABB(-1, -1, -1, 2, 2, 2), collisions, null);
        var posts = collisions.stream().filter(box -> box.getXsize() == 1.0 / 16 && box.getZsize() == 1.0 / 16 && box.getYsize() == 1).toList();
        assertEquals(2, posts.size());
        assertTrue(posts.stream().allMatch(box -> box.minZ == 15.0 / 16));
    }

    @Test void closedTrapdoorHasAirAndNoCombustionCollision() {
        var world = server.overworld();
        var pos = new BlockPos(2, 3, 4);
        world.setBlock(pos, Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, false), 3);
        assertFalse(CombustionAir.hasCollision(world, 2, 3, 4));
        assertTrue(CombustionAir.hasOxygen(world, 2, 3, 4));
        world.setBlock(pos, Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.WATERLOGGED, true), 3);
        assertTrue(CombustionAir.hasCollision(world, 2, 3, 4));
        world.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        assertTrue(CombustionAir.hasCollision(world, 2, 3, 4));
    }

    @Test void allBurningBoxClassesApplyTheirAirMixins() {
        for (String name : java.util.List.of("MultiTileEntityGeneratorSolid", "MultiTileEntityGeneratorLiquid",
            "MultiTileEntityGeneratorFluidBed"))
            assertDoesNotThrow(() -> Class.forName("gregtech.tileentity.energy.generators." + name));
    }
}
