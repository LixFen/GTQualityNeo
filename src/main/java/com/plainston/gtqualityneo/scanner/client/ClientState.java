package com.plainston.gtqualityneo.scanner.client;

import com.plainston.gtqualityneo.scanner.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import gregapi.oredict.OreDictMaterial;
import java.util.*;

public final class ClientState {
    public static long id;
    public static ScanGrid grid;
    public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
    public static ScanMode mode = ScanMode.LARGE;
    public static final Map<Integer, ScanNetwork.ChunkData> chunks = new HashMap<>();
    public static final Map<String, Integer> counts = new HashMap<>();
    public static final Set<String> filter = new HashSet<>();
    public static boolean scanning, terrain = false, hud = true;
    public static String status = "gtqualityneo.scanner.ready";
    public static int revision;
    public static BlockMark mark;
    public record BlockMark(int x, int y, int z, String resource, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {}
    public static void receive(ScanNetwork.Frame frame) {
        if (frame.kind() == 0) {
            if (Minecraft.getInstance().level == null) return;
            dimension = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(frame.message()));
            id = frame.scanId(); grid = new ScanGrid(frame.x(), frame.z(), frame.width());
            mode = ScanMode.values()[frame.mode()]; chunks.clear(); counts.clear(); filter.clear();
            scanning = true; status = "gtqualityneo.scanner.scanning";
        } else if (frame.kind() == 3) {
            scanning = false; status = frame.message();
            Minecraft.getInstance().gui.setOverlayMessage(Component.translatable(status), false);
        } else if (frame.scanId() == id && grid != null) {
            if (frame.kind() == 1 && frame.chunk().index() < grid.width() * grid.width()) {
                var c = frame.chunk();
                if (chunks.putIfAbsent(c.index(), c) == null) c.palette().forEach(r -> counts.merge(r.key(), r.count(), Integer::sum));
            } else if (frame.kind() == 2) { scanning = false; status = "gtqualityneo.scanner.complete"; }
        }
        revision++;
    }
    public static Component name(String key) {
        try {
            if (key.startsWith("m:")) {
                var m = OreDictMaterial.MATERIAL_ARRAY[Integer.parseInt(key.substring(2))];
                return Component.literal(gregapi.data.LH.get("gt.material." + m.mNameInternal, m.mNameLocal));
            }
            if (key.startsWith("b:")) return BuiltInRegistries.BLOCK.getValue(Identifier.parse(key.substring(2))).getName();
            if (key.startsWith("f:")) return BuiltInRegistries.FLUID.getValue(Identifier.parse(key.substring(2))).getFluidType().getDescription();
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) { return Component.literal(key); }
        return Component.literal(key);
    }
    public static int colour(ScanNetwork.Resource resource) {
        if (resource.key().startsWith("f:")) {
            var fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(resource.key().substring(2)));
            return com.lowdragmc.lowdraglib2.utils.FluidHelperClient.getColor(new net.neoforged.neoforge.fluids.FluidStack(fluid, 1000)) | 0xff000000;
        }
        return resource.colour();
    }
    public static void clear() { grid = null; dimension = null; chunks.clear(); counts.clear(); filter.clear(); mark = null; scanning = false; status = "gtqualityneo.scanner.ready"; revision++; }
    private ClientState() {}
}
