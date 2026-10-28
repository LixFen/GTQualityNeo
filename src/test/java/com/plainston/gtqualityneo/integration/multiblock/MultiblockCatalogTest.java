package com.plainston.gtqualityneo.integration.multiblock;

import static org.junit.jupiter.api.Assertions.*;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockBase;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class MultiblockCatalogTest {
    @org.junit.jupiter.api.BeforeAll static void initialize(net.minecraft.server.MinecraftServer server) throws Exception {
        // Loading the level runs GT6's deferred material, recipe and worldgen initialization.
        if (server.overworld() == null) {
            server.getWorldData().overworldData().setInitialized(true);
            var createLevels = net.minecraft.server.MinecraftServer.class.getDeclaredMethod("createLevels");
            createLevels.setAccessible(true);
            server.submit(() -> {
                try { createLevels.invoke(server); }
                catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
            }).get();
        }
    }
    @Test void capturedStructuresIncludeEveryRegisteredVisibleControllerAndPassNativeValidation(MinecraftServer server) {
        var registry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
        assertNotNull(registry);
        var pages = MultiblockCatalog.collect(server.registryAccess());
        long expected = registry.mRegistrations.stream().filter(entry -> !entry.mHidden && MultiblockCatalog.supportsController(entry.mClass)).count();
        assertTrue(expected > 0);
        assertEquals(expected, pages.size(), "Every supported controller must have a validated page");
        for (var page : pages) {
            assertFalse(page.ingredients.isEmpty());
            var world = page.world(server.registryAccess());
            var controller = (TileEntityBase09FacingSingle) world.getBlockEntity(new BlockPos(0, MultiblockCatalog.Y, 0));
            assertTrue(controller instanceof TileEntityBase10MultiBlockBase base ? base.checkStructure2(null, null, null)
                : ((TileEntityBase10MultiBlockMachine) controller).checkStructure2(null, null, null), "Controller " + page.id);
            assertEquals(page.parts.size(), page.ingredients.stream().mapToInt(stack -> stack.getCount()).sum());
        }
    }
    @Test void previewReadsAndWritesStayInsideItsOwnWorld(MinecraftServer server) {
        var world = new PreviewWorld(server.registryAccess());
        var pos = new BlockPos(2, 64, 3);
        world.setBlock(pos, Blocks.STONE.defaultBlockState(), 0);
        assertTrue(world.isClientSide());
        assertTrue(world.getBlockState(pos).is(Blocks.STONE));
        assertTrue(gregapi.util.WD.exists(world, 2, 64, 3));
        assertTrue(gregapi.util.WD.chunkNow(world, 0, 0).getBlockState(pos).is(Blocks.STONE));
        world.renderLayer = 65;
        assertTrue(world.getBlockState(pos).isAir());
    }
}
