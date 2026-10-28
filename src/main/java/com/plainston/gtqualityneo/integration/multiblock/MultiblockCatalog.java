package com.plainston.gtqualityneo.integration.multiblock;

import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.tileentity.multiblocks.ITileEntityMultiBlockController;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockBase;
import gregapi.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;
import gregtech.tileentity.multiblocks.MultiTileEntityBedrockDrill;
import gregtech.tileentity.multiblocks.MultiTileEntityFusionReactor;
import gregtech.tileentity.multiblocks.MultiTileEntityLightningRod;
import gregtech.tileentity.multiblocks.MultiTileEntityMatterFabricator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class MultiblockCatalog {
    private static final Logger LOG = LogUtils.getLogger();
    static final int Y = 64;
    private MultiblockCatalog() {}
    static boolean supportsController(Class<?> type) {
        return TileEntityBase10MultiBlockBase.class.isAssignableFrom(type) || TileEntityBase10MultiBlockMachine.class.isAssignableFrom(type);
    }
    public static List<Page> collect(RegistryAccess registries) {
        var registry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
        if (registry == null) return List.of();
        var pages = new ArrayList<Page>();
        for (var entry : registry.mRegistrations) {
            if (entry.mHidden || !supportsController(entry.mClass)) continue;
            try {
                var world = new PreviewWorld(registries);
                var controller = (TileEntityBase09FacingSingle) place(world, registry, entry.mID, new BlockPos(0, Y, 0));
                if (controller.getValidSides()[2]) controller.mFacing = 2;
                seed(world, controller);
                var kind = controller instanceof MultiTileEntityFusionReactor ? PreviewLayout.Kind.FUSION
                    : controller instanceof MultiTileEntityMatterFabricator ? PreviewLayout.Kind.MATTER
                    : controller instanceof MultiTileEntityLightningRod ? PreviewLayout.Kind.LIGHTNING : PreviewLayout.Kind.STANDARD;
                var layout = new PreviewLayout(kind, 0, Y, 0, 1);
                try (var capture = new PreviewCapture(controller, layout)) {
                    if (!check(controller) || layout.parts().isEmpty()) throw new IllegalStateException("No complete structure captured");
                }
                var parts = List.copyOf(layout.parts());
                populate(world, controller, parts);
                if (!check(controller)) throw new IllegalStateException("Captured structure failed GT6 validation");
                pages.add(new Page(registry, entry.mID, controller.mFacing, parts, controller instanceof MultiTileEntityBedrockDrill));
            } catch (RuntimeException error) {
                LOG.warn("Cannot preview GT6 multiblock {}", entry.mID, error);
            }
        }
        LOG.info("Registered {} GT6 multiblock previews", pages.size());
        return List.copyOf(pages);
    }
    private static boolean check(TileEntityBase09FacingSingle tile) {
        return tile instanceof TileEntityBase10MultiBlockBase base ? base.checkStructure2(null, null, null)
            : ((TileEntityBase10MultiBlockMachine) tile).checkStructure2(null, null, null);
    }
    static BlockEntity place(PreviewWorld world, MultiTileEntityRegistry registry, int id, BlockPos pos) {
        var container = registry.getNewTileEntityContainer(world, pos.getX(), pos.getY(), pos.getZ(), id, null);
        if (container == null) throw new IllegalStateException("Missing GT6 part " + id);
        world.setBlock(pos, container.mBlock.defaultBlockState(), 0);
        world.setBlockEntity(container.mTileEntity);
        return container.mTileEntity;
    }
    private static void seed(PreviewWorld world, BlockEntity controller) {
        if (controller instanceof MultiTileEntityBedrockDrill)
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                world.setBlock(new BlockPos(x, Y - 5, z), Blocks.BEDROCK.defaultBlockState(), 0);
    }
    private static void populate(PreviewWorld world, TileEntityBase09FacingSingle controller, List<PreviewLayout.Part> parts) {
        for (var cell : parts) {
            var p = cell.position();
            var part = (MultiTileEntityMultiBlockPart) place(world, MultiTileEntityRegistry.getRegistry(cell.registry()), cell.id(), new BlockPos(p.x(), p.y(), p.z()));
            part.setTarget((ITileEntityMultiBlockController) controller, cell.design(), cell.mode());
        }
    }
    public static final class Page {
        public final MultiTileEntityRegistry registry;
        public final int id;
        public final byte facing;
        final List<PreviewLayout.Part> parts;
        final boolean bedrock;
        public final List<ItemStack> ingredients;
        Page(MultiTileEntityRegistry registry, int id, byte facing, List<PreviewLayout.Part> parts, boolean bedrock) {
            this.registry = registry; this.id = id; this.facing = facing; this.parts = parts; this.bedrock = bedrock;
            var counts = new LinkedHashMap<String, ItemStack>();
            for (var part : parts) {
                String key = part.registry() + ":" + part.id();
                var stack = counts.get(key);
                if (stack == null) counts.put(key, MultiTileEntityRegistry.getRegistry(part.registry()).getItem(part.id()));
                else stack.grow(1);
            }
            ingredients = List.copyOf(counts.values());
        }
        public ItemStack controllerStack() { return registry.getItem(id); }
        public PreviewWorld world(RegistryAccess registries) {
            var world = new PreviewWorld(registries);
            var controller = (TileEntityBase09FacingSingle) place(world, registry, id, new BlockPos(0, Y, 0));
            controller.mFacing = facing;
            seed(world, controller);
            populate(world, controller, parts);
            controller.setDirectionData((byte) (facing | 8));
            return world;
        }
        public ItemStack stackAt(BlockPos pos) {
            if (pos.equals(new BlockPos(0, Y, 0))) return controllerStack();
            for (var part : parts) {
                var p = part.position();
                if (p.x() == pos.getX() && p.y() == pos.getY() && p.z() == pos.getZ())
                    return MultiTileEntityRegistry.getRegistry(part.registry()).getItem(part.id());
            }
            return ItemStack.EMPTY;
        }
    }
}
