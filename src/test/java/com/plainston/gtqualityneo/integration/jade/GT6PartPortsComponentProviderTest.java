package com.plainston.gtqualityneo.integration.jade;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;

class GT6PartPortsComponentProviderTest {
    @Test void portsFollowJadeDetailsKeyAndTargetFace() {
        CompoundTag ports = new CompoundTag();
        ports.putInt("gtquality.part.item_input_sides", 1 << Direction.NORTH.get3DDataValue());
        CompoundTag data = new CompoundTag();
        data.put(GT6PartPortsProvider.DATA, ports);
        assertTrue(lines(data, false, Direction.NORTH).isEmpty());
        assertEquals(List.of(Component.translatable("gtqualityneo.jade.part_item_input").getString()),
            lines(data, true, Direction.NORTH));
        assertTrue(lines(data, true, Direction.SOUTH).isEmpty());
        assertTrue(lines(data, false, Direction.NORTH).isEmpty());
    }

    @Test void detailsWithoutPortDataRemainEmpty() {
        assertTrue(lines(new CompoundTag(), true, Direction.NORTH).isEmpty());
    }

    private static List<String> lines(CompoundTag data, boolean details, Direction side) {
        List<String> lines = new ArrayList<>();
        ITooltip tooltip = (ITooltip) Proxy.newProxyInstance(ITooltip.class.getClassLoader(), new Class<?>[] { ITooltip.class },
            (proxy, method, args) -> {
                if (!method.getName().equals("add") || !(args[0] instanceof Component line))
                    throw new AssertionError("Unexpected tooltip call: " + method.getName());
                lines.add(line.getString());
                return null;
            });
        BlockAccessor accessor = (BlockAccessor) Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(),
            new Class<?>[] { BlockAccessor.class }, (proxy, method, args) -> switch (method.getName()) {
                case "showDetails" -> details;
                case "getServerData" -> data;
                case "getHitResult" -> new BlockHitResult(Vec3.ZERO, side, BlockPos.ZERO, false);
                default -> throw new AssertionError("Unexpected accessor call: " + method.getName());
            });
        GT6PartPortsComponentProvider.INSTANCE.appendTooltip(tooltip, accessor, null);
        return lines;
    }
}
