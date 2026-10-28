package com.plainston.gtqualityneo.integration.multiblock;

import gregapi.dummies.DummyWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import java.util.HashMap;
import java.util.Map;

/** A private, unticked world. Nothing is placed in the player's world. */
public final class PreviewWorld extends DummyWorld {
    final Map<BlockPos, BlockState> blocks = new HashMap<>();
    final Map<BlockPos, BlockEntity> tiles = new HashMap<>();
    int renderLayer = -1;
    private final Map<ChunkPos, LevelChunk> chunks = new HashMap<>();
    public PreviewWorld(RegistryAccess registries) { super(registries); }
    @Override public boolean isClientSide() { return true; }
    @Override public boolean isLoaded(BlockPos pos) { return true; }
    @Override public boolean hasChunk(int x, int z) { return true; }
    @Override public BlockState getBlockState(BlockPos pos) { return renderLayer >= 0 && pos.getY() != renderLayer ? Blocks.AIR.defaultBlockState() : blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public BlockEntity getBlockEntity(BlockPos pos) { return renderLayer >= 0 && pos.getY() != renderLayer ? null : tiles.get(pos); }
    @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
        blocks.put(pos.immutable(), state);
        return true;
    }
    @Override public void setBlockEntity(BlockEntity tile) { tiles.put(tile.getBlockPos(), tile); }
    public LevelChunk chunk(int x, int z) {
        return chunks.computeIfAbsent(new ChunkPos(x, z), pos -> new LevelChunk(this, pos) {
            @Override public BlockState getBlockState(BlockPos at) { return PreviewWorld.this.getBlockState(at); }
            @Override public BlockEntity getBlockEntity(BlockPos at) { return PreviewWorld.this.getBlockEntity(at); }
            @Override public BlockEntity getBlockEntity(BlockPos at, EntityCreationType creation) { return PreviewWorld.this.getBlockEntity(at); }
            @Override public boolean isEmpty() { return false; }
        });
    }
}
