package com.plainston.gtqualityneo.scanner;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;

public final class ScanNetwork {
    public record Request(int mode, boolean offhand, boolean cancel) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.mode); b.writeBoolean(p.offhand); b.writeBoolean(p.cancel); },
                b -> new Request(b.readVarInt(), b.readBoolean(), b.readBoolean()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Resource(String key, int colour, int count) {}
    public record Cell(int resource, int y) {}
    public record ChunkData(int index, boolean available, List<Resource> palette, List<List<Cell>> columns, int[] surface) {}
    /** BEGIN=0, CHUNK=1, DONE=2, ERROR=3. Every packet is tied to one server-issued scan ID. */
    public record Frame(long scanId, int kind, int x, int z, int width, int mode, ChunkData chunk, String message) implements CustomPacketPayload {
        public static final Type<Frame> TYPE = new Type<>(id("frame"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Frame> CODEC = StreamCodec.of(ScanNetwork::encode, ScanNetwork::decode);
        @Override public Type<Frame> type() { return TYPE; }
        public static Frame begin(long id, ScanGrid g, ScanMode m, Identifier dimension) { return new Frame(id, 0, g.chunkX(), g.chunkZ(), g.width(), m.ordinal(), null, dimension.toString()); }
        public static Frame chunk(long id, ChunkData c) { return new Frame(id, 1, 0, 0, 0, 0, c, ""); }
        public static Frame done(long id) { return new Frame(id, 2, 0, 0, 0, 0, null, ""); }
        public static Frame error(long id, String key) { return new Frame(id, 3, 0, 0, 0, 0, null, key); }
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath(ScannerIntegration.ID, "scanner/" + name); }
    private static void encode(RegistryFriendlyByteBuf b, Frame f) {
        b.writeLong(f.scanId); b.writeByte(f.kind);
        switch (f.kind) {
            case 0 -> { b.writeInt(f.x); b.writeInt(f.z); b.writeVarInt(f.width); b.writeVarInt(f.mode); b.writeUtf(f.message, 128); }
            case 1 -> {
                var c = f.chunk;
                b.writeVarInt(c.index); b.writeBoolean(c.available);
                if (!c.available) return;
                b.writeVarInt(c.palette.size());
                for (var r : c.palette) { b.writeUtf(r.key, 128); b.writeInt(r.colour); b.writeVarInt(r.count); }
                for (var column : c.columns) {
                    b.writeByte(column.size());
                    for (var cell : column) { b.writeVarInt(cell.resource); b.writeShort(cell.y); }
                }
                for (int rgb : c.surface) b.writeInt(rgb);
            }
            case 3 -> b.writeUtf(f.message, 128);
        }
    }
    private static int bounded(int n, int max) {
        if (n < 0 || n > max) throw new IllegalArgumentException("Invalid scanner payload length: " + n);
        return n;
    }
    private static Frame decode(RegistryFriendlyByteBuf b) {
        long id = b.readLong(); int kind = b.readUnsignedByte();
        return switch (kind) {
            case 0 -> {
                int x = b.readInt(), z = b.readInt(), width = bounded(b.readVarInt(), 31), mode = bounded(b.readVarInt(), ScanMode.values().length - 1);
                if (width == 0) throw new IllegalArgumentException("Empty scan grid");
                yield new Frame(id, kind, x, z, width, mode, null, b.readUtf(128));
            }
            case 1 -> {
                int index = bounded(b.readVarInt(), 960); boolean available = b.readBoolean();
                if (!available) yield Frame.chunk(id, new ChunkData(index, false, List.of(), List.of(), new int[0]));
                int size = bounded(b.readVarInt(), 2048);
                var palette = new ArrayList<Resource>(size);
                for (int i = 0; i < size; i++) palette.add(new Resource(b.readUtf(128), b.readInt(), bounded(b.readVarInt(), 2_000_000)));
                var columns = new ArrayList<List<Cell>>(256);
                for (int i = 0; i < 256; i++) {
                    int count = bounded(b.readUnsignedByte(), ColumnData.LIMIT);
                    var cells = new ArrayList<Cell>(count);
                    for (int j = 0; j < count; j++) cells.add(new Cell(bounded(b.readVarInt(), size - 1), b.readShort()));
                    columns.add(List.copyOf(cells));
                }
                int[] surface = new int[256];
                for (int i = 0; i < 256; i++) surface[i] = b.readInt();
                yield Frame.chunk(id, new ChunkData(index, true, List.copyOf(palette), List.copyOf(columns), surface));
            }
            case 2 -> Frame.done(id);
            case 3 -> Frame.error(id, b.readUtf(128));
            default -> throw new IllegalArgumentException("Invalid scanner frame kind");
        };
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToServer(Request.TYPE, Request.CODEC, (p, c) -> c.enqueueWork(() -> ScanService.request(p, c.player())));
        r.playToClient(Frame.TYPE, Frame.CODEC, (p, c) -> c.enqueueWork(() -> ScannerIntegration.receiveFrame.accept(p)));
    }
    private ScanNetwork() {}
}
