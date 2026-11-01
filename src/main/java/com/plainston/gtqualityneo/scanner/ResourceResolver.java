package com.plainston.gtqualityneo.scanner;

import gregapi.block.prefixblock.PrefixBlock;
import gregapi.data.*;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.*;
import gregtech.blocks.BlockDiggable;
import gregtech.blocks.BlockSands;
import gregtech.blocks.stone.*;
import gregtech.tileentity.misc.MultiTileEntityFluidSpring;
import gregtech.tileentity.placeables.MultiTileEntityRock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class ResourceResolver {
    public record Resource(String key, int colour) {}
    private final ScanMode mode;
    private final Map<BlockState, Optional<Resource>> cache = new HashMap<>();
    private final Map<Integer, Optional<Resource>> materials = new HashMap<>();
    private final Map<PrefixBlock, Boolean> prefixes = new HashMap<>();
    public ResourceResolver(ScanMode mode) { this.mode = mode; }
    public Resource resolveOre(BlockState state, short materialId) {
        if (!(state.getBlock() instanceof PrefixBlock prefix)) return null;
        if (!prefixes.computeIfAbsent(prefix, this::acceptsPrefix)) return null;
        return materials.computeIfAbsent((int) materialId, id -> Optional.ofNullable(material(id))).orElse(null);
    }
    private boolean acceptsPrefix(PrefixBlock prefix) {
        var target = switch (mode) {
            case LARGE -> OP.ore; case SMALL -> OP.oreSmall; case BEDROCK -> OP.oreBedrock; default -> null;
        };
        return target != null && (prefix.mPrefix.mFamiliarPrefixes.contains(target)
                || mode == ScanMode.BEDROCK && prefix.mNameInternal.contains("bedrock"));
    }
    public boolean acceptsEntity(BlockEntity tile) {
        return mode == ScanMode.ROCK && tile instanceof MultiTileEntityRock
                || mode == ScanMode.FLUID_BEDROCK && tile instanceof MultiTileEntityFluidSpring;
    }
    public Resource resolveEntity(BlockEntity tile) {
        if (mode == ScanMode.FLUID_BEDROCK && tile instanceof MultiTileEntityFluidSpring spring)
            return spring.mFluid == null || spring.mFluid.isEmpty() ? null : fluid(spring.mFluid.getFluid());
        if (mode == ScanMode.ROCK && tile instanceof MultiTileEntityRock rock) {
            Resource resource = itemMaterial(rock.getRock(1));
            if (resource == null) resource = itemMaterial(rock.getDefaultRock(1));
            return resource == null ? material(MT.Stone.mID) : resource;
        }
        return null;
    }
    private static Resource itemMaterial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var data = OM.anydata_(stack);
        return data == null || data.mMaterial == null || data.mMaterial.mMaterial == null ? null : material(data.mMaterial.mMaterial.mID);
    }
    public boolean mayMatch(BlockState state) { return resolveBlock(state) != null; }
    public Resource resolveBlock(BlockState state) {
        return cache.computeIfAbsent(state, s -> Optional.ofNullable(resolveState(s))).orElse(null);
    }
    private Resource resolveState(BlockState state) {
        // Prefix ores belong to the indexed modes, just as the original excluded their TileEntities here.
        if (state.isAir() || state.getBlock() instanceof PrefixBlock) return null;
        if (mode == ScanMode.FLUID) {
            var f = state.getFluidState();
            Fluid fluid;
            if (state.getBlock() instanceof gregapi.block.fluid.BlockFluidBaseGT gtFluid) fluid = gtFluid.getFluid();
            else if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock && !f.isEmpty()) fluid = f.getType();
            else return null; // Waterlogged solid blocks are not fluid blocks in the original scan.
            String name = BuiltInRegistries.FLUID.getKey(fluid).getPath();
            if (List.of("water", "lava", "oil", "natural", "honey", "sulfuric", "acid", "poison", "infused", "mana").stream().anyMatch(name::contains)) return fluid(fluid);
            return null;
        }
        var block = state.getBlock();
        if (mode != ScanMode.OREDICT && mode != ScanMode.DENSE_AND_NORMAL) return null;
        int flattened = CS.Flattened.metaOf(block);
        int meta = flattened >= 0 ? flattened : block instanceof gregapi.block.IBlockExtendedMetaData extended ? extended.getExtendedMetaData(state) : 0;
        if (mode == ScanMode.DENSE_AND_NORMAL) {
            if (block instanceof BlockRockOres) return material(BlockRockOres.ORE_MATERIALS[meta].mID);
            if (block instanceof BlockCrystalOres) return material(BlockCrystalOres.ORE_MATERIALS[meta].mID);
            if (block instanceof BlockVanillaOresA) return material(BlockVanillaOresA.ORE_MATERIALS[meta].mID);
            if (block instanceof BlockDiggable) return switch (meta) {
                case 1 -> material(MT.ClayBrown.mID); case 2 -> material(MT.Peat.mID);
                case 3 -> material(MT.ClayRed.mID); case 4 -> material(MT.Bentonite.mID);
                case 5 -> material(MT.Palygorskite.mID); case 6 -> material(MT.Kaolinite.mID);
                default -> null;
            };
            if (block instanceof BlockSands) return switch (meta) {
                case 0 -> material(MT.OREMATS.Magnetite.mID);
                case 1 -> material(MT.OREMATS.BasalticMineralSand.mID);
                case 2 -> material(MT.OREMATS.GraniticMineralSand.mID); default -> null;
            };
            return null;
        }
        if (state.hasBlockEntity()) return null;
        ItemStack stack = ST.make(block, 1, meta);
        var data = OM.anydata_(stack);
        if (data != null && data.mPrefix != null && data.mPrefix.mNameInternal.startsWith("ore") && data.mMaterial != null)
            return material(data.mMaterial.mMaterial.mID);
        // Modern equivalent of the ore dictionary: conventional ore block tags, including other mods.
        if (block.builtInRegistryHolder().tags().anyMatch(t -> t.location().getNamespace().equals("c") &&
                (t.location().getPath().equals("ores") || t.location().getPath().startsWith("ores/")))) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            return new Resource("b:" + id, 0xff000000 | (id.hashCode() & 0x7f7f7f) | 0x606060);
        }
        return null;
    }
    public static Resource material(int id) {
        if (id <= 0 || id >= OreDictMaterial.MATERIAL_ARRAY.length) return null;
        var m = OreDictMaterial.MATERIAL_ARRAY[id];
        if (m == null || m.contains(TD.Properties.AUTO_MATERIAL)) return null;
        return new Resource("m:" + id, 0xff000000 | UT.Code.getRGBInt(m.fRGBa[0]));
    }
    private static Resource fluid(Fluid fluid) {
        if (fluid instanceof net.minecraft.world.level.material.FlowingFluid flowing) fluid = flowing.getSource();
        String key = BuiltInRegistries.FLUID.getKey(fluid).toString();
        // A stable registry ID distinguishes heavy/light/medium oil across client and server.
        int colour = 0xff000000 | (key.hashCode() & 0x7f7f7f) | 0x404040;
        return new Resource("f:" + key, colour);
    }
}
