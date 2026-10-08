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
import java.util.List;

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

    @Test void moldPersistenceAPIRecognizesEmptyAndOccupiedMolds() {
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
        ObstructionConfig.restore();
        boolean previous = setting.get(), gtPrevious = gregapi.data.CS.OBSTRUCTION_CHECKS;
        try {
            gregapi.data.CS.OBSTRUCTION_CHECKS = true;
            setting.set(false);
            ObstructionConfig.apply();
            assertTrue(gregapi.util.WD.obstructed(world, 0, 70, 0, (byte) 3));
            setting.set(true);
            ObstructionConfig.apply();
            assertFalse(gregapi.util.WD.obstructed(world, 0, 70, 0, (byte) 3));
            ObstructionConfig.restore();
            assertTrue(gregapi.data.CS.OBSTRUCTION_CHECKS);
            gregapi.data.CS.OBSTRUCTION_CHECKS = false;
            ObstructionConfig.apply();
            ObstructionConfig.restore();
            assertFalse(gregapi.data.CS.OBSTRUCTION_CHECKS, "Restore the upstream value, even when already disabled");
        } finally {
            ObstructionConfig.restore();
            setting.set(previous);
            gregapi.data.CS.OBSTRUCTION_CHECKS = gtPrevious;
            ObstructionConfig.apply();
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

    @Test void circuitSelectionPreservesCountModeAndComponentsAndRejectsStaleRequests() {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(server.overworld());
        var circuitItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
            .filter(item -> item instanceof gregapi.item.ItemIntegratedCircuit).findFirst().orElseThrow();
        var stack = new ItemStack(circuitItem, 16);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("named circuit"));
        gregapi.util.ST.meta_(stack, 0x0203);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, stack);
        try {
            assertFalse(CircuitInteraction.select(player, 0, 0x0203, 25));
            assertFalse(CircuitInteraction.select(player, 1, 0x0203, 7));
            assertFalse(CircuitInteraction.select(player, 0, 0x0204, 7));
            assertEquals(0x0203, gregapi.util.ST.meta_(stack));
            assertTrue(CircuitInteraction.select(player, 0, 0x0203, 24));
            assertEquals(0x0218, gregapi.util.ST.meta_(stack));
            assertEquals(16, stack.getCount());
            assertEquals(Component.literal("named circuit"), stack.get(DataComponents.CUSTOM_NAME));
            var option = com.plainston.gtqualityneo.GTQualityNeo.CIRCUIT_SELECTOR;
            boolean previous = option.get();
            option.set(false);
            try { assertFalse(CircuitInteraction.select(player, 0, 0x0218, 0)); }
            finally { option.set(previous); }
        } finally { player.getInventory().setItem(0, ItemStack.EMPTY); }
    }

    @Test void hazmatDefaultComponentsRetainDamageAndDoNotPersistEnhancedLimit() {
        var option = com.plainston.gtqualityneo.GTQualityNeo.UNIVERSAL_HAZMAT;
        boolean previous = option.get();
        var stack = new ItemStack(gregapi.data.CS.ArmorsGT.HAZMAT_UNIVERSAL[0]);
        stack.set(DataComponents.DAMAGE, 100);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("old suit"));
        try {
            option.set(true);
            assertEquals(512, stack.getMaxDamage());
            assertEquals(412, stack.getMaxDamage() - stack.getDamageValue());
            stack.setDamageValue(200);
            assertEquals(200, stack.getDamageValue());
            option.set(false);
            assertEquals(512, stack.getMaxDamage(), "Default component changes take effect at startup, not a live toggle");
            assertEquals(200, stack.get(DataComponents.DAMAGE));
            option.set(true);
            assertEquals(200, stack.getDamageValue());
            assertEquals(Component.literal("old suit"), stack.get(DataComponents.CUSTOM_NAME));
            assertNull(stack.getComponentsPatch().getPatch(DataComponents.MAX_DAMAGE), "Enhanced limit is inherited, not saved to stacks");
        } finally { option.set(previous); }
    }

    @Test void fluidRequestsValidateMenuAndUseRealCursorWithoutMenuMixin() {
        server.overworld().getChunkAt(BlockPos.ZERO);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(server.overworld());
        boolean[] usable = {true};
        var machine = new gregapi.tileentity.machines.MultiTileEntityBasicMachine() {
            @Override public boolean isUseableByPlayerGUI(net.minecraft.world.entity.player.Player ignored) { return usable[0]; }
            @Override public ItemStack getStackInSlotGUI(int slot) { return ItemStack.EMPTY; }
        };
        machine.setLevel(server.overworld());
        machine.mTanksInput = new FluidTankGT[]{new FluidTankGT(4000)};
        machine.mTanksOutput = new FluidTankGT[]{new FluidTankGT(new FluidStack(Fluids.WATER, 1000), 4000)};
        var menu = new gregapi.gui.ContainerCommonBasicMachine(player.getInventory(), machine, machine.mRecipes, 0) {
            @Override public int addSlots(net.minecraft.world.entity.player.Inventory inventory) {
                int offset = machine.mRecipes.mInputItemsCount + machine.mRecipes.mOutputItemsCount + 1;
                addSlot(new gregapi.gui.Slot_Render(machine, offset, 0, 0));
                addSlot(new gregapi.gui.Slot_Render(machine, offset + 1, 18, 0));
                return 84;
            }
        };
        var original = player.containerMenu;
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        try {
            assertFalse(GuiFluidInteraction.request(player, menu.containerId + 1, 0, 0, false));
            assertFalse(GuiFluidInteraction.request(player, menu.containerId, -1, 0, false));
            assertFalse(GuiFluidInteraction.request(player, menu.containerId, 0, 2, false));
            assertEquals(0, machine.mTanksInput[0].amount());
            assertTrue(GuiFluidInteraction.request(player, menu.containerId, 0, 0, false));
            assertEquals(1000, machine.mTanksInput[0].amount());
            assertTrue(menu.getCarried().is(Items.BUCKET));
            assertTrue(GuiFluidInteraction.request(player, menu.containerId, 1, 1, false));
            assertEquals(0, machine.mTanksOutput[0].amount());
            assertTrue(menu.getCarried().is(Items.WATER_BUCKET));
            assertTrue(GuiFluidInteraction.request(player, menu.containerId, 1, 0, false));
            assertEquals(0, machine.mTanksOutput[0].amount(), "Output slot cannot receive water");
            usable[0] = false;
            assertFalse(GuiFluidInteraction.request(player, menu.containerId, 0, 0, false));
            assertEquals(1000, machine.mTanksInput[0].amount());
        } finally { player.containerMenu = original; }
    }

    @Test void hazmatComponentsRespectDisabledStartupOption() {
        var predicates = new ArrayList<com.mojang.datafixers.util.Pair<
            net.neoforged.neoforge.event.ModifyDefaultComponentsEvent.ItemWithComponentsPredicate,
            net.neoforged.neoforge.event.ModifyDefaultComponentsEvent.Initializer>>();
        var event = new net.neoforged.neoforge.event.ModifyDefaultComponentsEvent(new java.util.HashMap<>(), predicates);
        UniversalHazmat.components(event);
        var item = gregapi.data.CS.ArmorsGT.HAZMAT_UNIVERSAL[0];
        var option = com.plainston.gtqualityneo.GTQualityNeo.UNIVERSAL_HAZMAT;
        boolean previous = option.get();
        try {
            for (boolean enabled : new boolean[]{false, true}) {
                option.set(enabled);
                var builder = net.minecraft.core.component.DataComponentMap.builder().set(DataComponents.MAX_DAMAGE, 128);
                for (var entry : predicates) if (entry.getFirst().test(item, builder))
                    entry.getSecond().run(builder, server.registryAccess(), item);
                assertEquals(enabled ? 512 : 128, builder.build().get(DataComponents.MAX_DAMAGE));
            }
        } finally { option.set(previous); }
    }

    @Test void moldShapeChangeThroughNBTKeepsTemperatureControlsAndContent() {
        var mold = new MultiTileEntityMold();
        var original = new net.minecraft.nbt.CompoundTag();
        original.putLong("gt.temperature", 70000);
        original.putByte("gt.connection", (byte) 12);
        original.putBoolean("gt.mode", true);
        new gregapi.oredict.OreDictMaterialStack(gregapi.data.MT.Fe, gregapi.data.CS.U).save(gregapi.data.CS.NBT_MATERIALS, original);
        mold.readFromNBT2(original);
        MoldInteraction.setShape(mold, 1);
        var saved = new net.minecraft.nbt.CompoundTag();
        mold.writeToNBT2(saved);
        assertEquals(1, MoldInteraction.shape(mold));
        assertEquals(70000, saved.getLongOr("gt.temperature", 0));
        assertEquals(12, saved.getByteOr("gt.connection", (byte) 0));
        assertTrue(saved.getBooleanOr("gt.mode", false));
        assertEquals(gregapi.data.CS.U, gregapi.oredict.OreDictMaterialStack.load(gregapi.data.CS.NBT_MATERIALS, saved).mAmount);
        assertFalse(MoldInteraction.canEdit(mold));
    }

    @Test void fullUniversalSuitAddsArmorAndRemovingPieceOrDisablingRestoresBase() throws Exception {
        var wearer = new net.minecraft.world.entity.monster.zombie.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, server.overworld());
        wearer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        var slots = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        var update = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");
        update.setAccessible(true);
        var option = com.plainston.gtqualityneo.GTQualityNeo.UNIVERSAL_HAZMAT;
        boolean previous = option.get();
        try {
            option.set(true);
            for (int i = 0; i < slots.length; i++) wearer.setItemSlot(slots[i], new ItemStack(gregapi.data.CS.ArmorsGT.HAZMAT_UNIVERSAL[i]));
            update.invoke(wearer);
            UniversalHazmat.update(wearer);
            assertTrue(UniversalHazmat.fullSet(wearer));
            assertEquals(20, wearer.getArmorValue());
            option.set(false);
            UniversalHazmat.update(wearer);
            assertEquals(4, wearer.getArmorValue());
            option.set(true);
            wearer.setItemSlot(slots[0], ItemStack.EMPTY);
            update.invoke(wearer);
            UniversalHazmat.update(wearer);
            assertFalse(UniversalHazmat.fullSet(wearer));
            assertEquals(3, wearer.getArmorValue());
        } finally { option.set(previous); }
    }

    @Test void fullUniversalSuitBlocksFireWithArmorUpgradeDisabledAndRespectsImmunityOption() {
        var wearer = new net.minecraft.world.entity.monster.zombie.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, server.overworld());
        var slots = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        var armor = com.plainston.gtqualityneo.GTQualityNeo.UNIVERSAL_HAZMAT;
        var fire = com.plainston.gtqualityneo.GTQualityNeo.HEAT_HAZMAT_IMMUNITY;
        boolean oldArmor = armor.get(), oldFire = fire.get();
        try {
            armor.set(false);
            fire.set(true);
            for (int i = 0; i < slots.length; i++) wearer.setItemSlot(slots[i], new ItemStack(gregapi.data.CS.ArmorsGT.HAZMAT_UNIVERSAL[i]));
            for (var source : List.of(wearer.damageSources().inFire(), wearer.damageSources().onFire(), wearer.damageSources().lava())) {
                assertTrue(source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE));
                var event = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(wearer,
                    new net.neoforged.neoforge.common.damagesource.DamageContainer(source, 10));
                QolEvents.damage(event);
                assertTrue(event.isCanceled());
            }
            var generic = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(wearer,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(wearer.damageSources().generic(), 10));
            QolEvents.damage(generic);
            assertFalse(generic.isCanceled());
            fire.set(false);
            var disabled = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(wearer,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(wearer.damageSources().lava(), 10));
            QolEvents.damage(disabled);
            assertFalse(disabled.isCanceled());
            fire.set(true);
            wearer.setItemSlot(slots[0], ItemStack.EMPTY);
            var partial = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(wearer,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(wearer.damageSources().lava(), 10));
            QolEvents.damage(partial);
            assertFalse(partial.isCanceled());
        } finally { armor.set(oldArmor); fire.set(oldFire); }
    }

    @Test void filterGhostSetsItemAndFluidWithoutChangingCursorAndRejectsInvalidSlots() {
        server.overworld().getChunkAt(BlockPos.ZERO);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(server.overworld());
        boolean[] usable = {true};
        var filter = new gregtech.tileentity.extenders.MultiTileEntityFilter() {
            @Override public boolean isUseableByPlayerGUI(net.minecraft.world.entity.player.Player ignored) { return usable[0]; }
        };
        filter.setLevel(server.overworld());
        filter.mModes = gregtech.tileentity.extenders.MultiTileEntityExtender.EXTENDER_INV;
        var menu = filter.new MultiTileEntityGUICommonFilter(player.getInventory(), filter, 0);
        var originalMenu = player.containerMenu;
        var held = new ItemStack(Items.DIAMOND, 3);
        menu.setCarried(held);
        player.containerMenu = menu;
        try {
            assertFalse(FilterGhost.select(player, menu.containerId + 1, 0, new ItemStack(Items.IRON_INGOT)));
            assertFalse(FilterGhost.select(player, menu.containerId, -1, new ItemStack(Items.IRON_INGOT)));
            assertFalse(FilterGhost.select(player, menu.containerId, 54, new ItemStack(Items.IRON_INGOT)));
            assertTrue(FilterGhost.select(player, menu.containerId, 0, new ItemStack(Items.IRON_INGOT, 64)));
            assertTrue(filter.mFilter[0].is(Items.IRON_INGOT));
            assertEquals(1, filter.mFilter[0].getCount());
            filter.mModes = gregtech.tileentity.extenders.MultiTileEntityExtender.EXTENDER_TANK;
            assertTrue(FilterGhost.select(player, menu.containerId, 1, new ItemStack(Items.WATER_BUCKET)));
            assertTrue(filter.allowInput(Fluids.WATER));
            assertFalse(filter.allowInput(Fluids.LAVA));
            assertTrue(FilterGhost.select(player, menu.containerId, 1, gregapi.data.FL.display(Fluids.LAVA)));
            assertTrue(filter.allowInput(Fluids.LAVA));
            usable[0] = false;
            assertFalse(FilterGhost.select(player, menu.containerId, 1, gregapi.data.FL.display(Fluids.WATER)));
            assertTrue(filter.allowInput(Fluids.LAVA));
            assertSame(held, menu.getCarried());
            assertEquals(3, held.getCount());
        } finally { player.containerMenu = originalMenu; }
    }


}
