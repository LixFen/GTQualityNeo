package com.plainston.gtqualityneo.scanner;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class ScanService {
    private static final Map<UUID, Job> JOBS = new LinkedHashMap<>();
    private static long sequence;
    public static void request(ScanNetwork.Request request, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (request.cancel()) {
            var job = JOBS.remove(player.getUUID());
            if (job != null) send(serverPlayer, ScanNetwork.Frame.error(job.id, "gtqualityneo.scanner.cancelled"));
            return;
        }
        if (request.mode() < 0 || request.mode() >= ScanMode.values().length) return;
        var stack = player.getItemInHand(request.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof ScannerItem item)) return;
        if (JOBS.containsKey(player.getUUID())) return; // no duplicate billing while a scan runs
        long id = ++sequence;
        long cost = item.voltage() * ScanConfig.CONSUMPTION.get();
        if (!player.isCreative() && !item.useEnergy(gregapi.data.TD.Energy.EU, stack, cost, player,
                player.getInventory(), player.level(), player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ(), true)) {
            send(serverPlayer, ScanNetwork.Frame.error(id, "gtqualityneo.scanner.no_energy"));
            return;
        }
        var tag = gregapi.code.ItemNBT.get(stack);
        if (tag == null) tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("gtqualityneo.scanner.mode", request.mode());
        gregapi.util.UT.NBT.set(stack, tag);
        var job = new Job(id, serverPlayer, ScanMode.values()[request.mode()], ScanConfig.range(item.tier));
        JOBS.put(player.getUUID(), job);
        send(serverPlayer, ScanNetwork.Frame.begin(id, job.grid, job.mode, job.level.dimension().identifier()));
    }
    public static void tick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) return;
        long deadline = System.nanoTime() + ScanConfig.BUDGET_MS.get() * 1_000_000L;
        // Rotate the queue after a time slice so one large scan cannot starve another player.
        for (UUID key : List.copyOf(JOBS.keySet())) {
            Job job = JOBS.remove(key);
            if (job == null) continue;
            try {
                if (job.valid()) {
                    if (!job.step(deadline)) JOBS.put(key, job);
                } else if (!job.player.hasDisconnected()) send(job.player, ScanNetwork.Frame.error(job.id, "gtqualityneo.scanner.cancelled"));
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().error("GT6 scan failed", e);
                if (job.valid()) send(job.player, ScanNetwork.Frame.error(job.id, "gtqualityneo.scanner.failed"));
            }
            if (System.nanoTime() >= deadline) break;
        }
    }
    public static void stopping(ServerStoppingEvent event) { JOBS.clear(); }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { JOBS.remove(event.getEntity().getUUID()); }
    private static void send(ServerPlayer player, ScanNetwork.Frame frame) { PacketDistributor.sendToPlayer(player, frame); }

    private static final class Job {
        final long id;
        final ServerPlayer player;
        final ServerLevel level;
        final ScanMode mode;
        final ScanGrid grid;
        final ResourceResolver resolver;
        final int[] order;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final Map<String, ResourceResolver.Resource> resources = new LinkedHashMap<>();
        final Map<String, Integer> counts = new HashMap<>();
        final List<ColumnData> columns = new ArrayList<>();
        final int[] surface = new int[256];
        LevelChunk chunk;
        int next, surfaceColumn, cursor, section;
        long[] oreEntries;
        Iterator<net.minecraft.world.level.block.entity.BlockEntity> tiles;
        Job(long id, ServerPlayer player, ScanMode mode, int width) {
            this.id = id; this.player = player; this.level = (ServerLevel) player.level(); this.mode = mode;
            grid = ScanGrid.around(player.blockPosition().getX(), player.blockPosition().getZ(), width);
            order = grid.nearestFirst(player.blockPosition().getX(), player.blockPosition().getZ());
            resolver = new ResourceResolver(mode);
        }
        boolean valid() { return !player.hasDisconnected() && player.isAlive() && player.level() == level; }
        boolean step(long deadline) {
            do {
                if (next == order.length) { send(player, ScanNetwork.Frame.done(id)); return true; }
                int index = order[next];
                if (chunk == null) {
                    chunk = ScanConfig.LOAD_CHUNKS.get() ? level.getChunk(grid.x(index), grid.z(index)) : level.getChunkSource().getChunkNow(grid.x(index), grid.z(index));
                    if (chunk == null) {
                        send(player, ScanNetwork.Frame.chunk(id, new ScanNetwork.ChunkData(index, false, List.of(), List.of(), new int[0])));
                        next++;
                        continue;
                    }
                    surfaceColumn = 0; cursor = 0; section = 0; oreEntries = null; tiles = null;
                    columns.clear(); resources.clear(); counts.clear();
                    for (int i = 0; i < 256; i++) columns.add(new ColumnData());
                }
                if (surfaceColumn < 256) { sampleSurface(index); continue; }
                boolean finished = switch (mode.source()) {
                    case ORE_INDEX -> scanOreEntry(index);
                    case BLOCK_ENTITIES -> scanEntity(index);
                    case BLOCK_SECTIONS -> scanSection();
                };
                if (finished) {
                    sendChunk(index); chunk = null; next++;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
        void sampleSurface(int index) {
            int x = grid.x(index) * 16 + surfaceColumn / 16, z = grid.z(index) * 16 + surfaceColumn % 16;
            int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, surfaceColumn / 16, surfaceColumn % 16);
            var surfacePos = new BlockPos(x, top, z);
            surface[surfaceColumn++] = 0xff000000 | chunk.getBlockState(surfacePos).getMapColor(level, surfacePos).col;
        }
        boolean scanOreEntry(int index) {
            if (oreEntries == null) oreEntries = PrefixOreIndex.snapshot(chunk.getExistingDataOrNull(gregapi.block.prefixblock.PrefixBlockOreMap.TYPE.get()));
            if (cursor == oreEntries.length) return true;
            long entry = oreEntries[cursor++];
            int x = PrefixOreIndex.x(entry), z = PrefixOreIndex.z(entry), y = PrefixOreIndex.y(entry);
            pos.set(grid.x(index) * 16 + x, y, grid.z(index) * 16 + z);
            record(x, y, z, resolver.resolveOre(chunk.getBlockState(pos), PrefixOreIndex.material(entry)));
            return cursor == oreEntries.length;
        }
        boolean scanEntity(int index) {
            if (tiles == null) tiles = chunk.getBlockEntities().values().stream().filter(resolver::acceptsEntity).toList().iterator();
            if (!tiles.hasNext()) return true;
            var tile = tiles.next();
            if (!tile.isRemoved()) {
                var p = tile.getBlockPos();
                record(p.getX() - grid.x(index) * 16, p.getY(), p.getZ() - grid.z(index) * 16, resolver.resolveEntity(tile));
            }
            return !tiles.hasNext();
        }
        boolean scanSection() {
            var sections = chunk.getSections();
            if (section == sections.length) return true;
            var current = sections[section];
            if (cursor == 0 && (current.hasOnlyAir() || !current.maybeHas(resolver::mayMatch))) {
                section++;
                return section == sections.length;
            }
            // Check the shared tick deadline between batches, rather than after a whole column or chunk.
            int end = Math.min(cursor + 64, 4096);
            for (; cursor < end; cursor++) {
                int x = cursor & 15, z = (cursor >>> 4) & 15, localY = cursor >>> 8;
                var state = current.getBlockState(x, localY, z);
                var resource = resolver.resolveBlock(state);
                if (resource == null) continue;
                int y = chunk.getSectionYFromSectionIndex(section) * 16 + localY;
                record(x, y, z, resource);
            }
            if (cursor == 4096) { cursor = 0; section++; }
            return section == sections.length;
        }
        void record(int x, int y, int z, ResourceResolver.Resource resource) {
            if (resource == null || x < 0 || x >= 16 || z < 0 || z >= 16
                    || (!resources.containsKey(resource.key()) && resources.size() >= 2048)) return;
            resources.putIfAbsent(resource.key(), resource);
            counts.merge(resource.key(), 1, Integer::sum);
            columns.get(x * 16 + z).record(resource.key(), y);
        }
        void sendChunk(int index) {
            var palette = new ArrayList<ScanNetwork.Resource>();
            var ids = new HashMap<String, Integer>();
            resources.forEach((key, r) -> {
                ids.put(key, palette.size());
                palette.add(new ScanNetwork.Resource(key, r.colour(), counts.get(key)));
            });
            var cells = columns.stream().map(c -> c.hits().stream().map(h -> new ScanNetwork.Cell(ids.get(h.key()), h.y())).toList()).toList();
            send(player, ScanNetwork.Frame.chunk(id, new ScanNetwork.ChunkData(index, true, palette, cells, surface.clone())));
        }
    }
    private ScanService() {}
}
