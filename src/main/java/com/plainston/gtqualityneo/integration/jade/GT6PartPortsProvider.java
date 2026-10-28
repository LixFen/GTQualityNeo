package com.plainston.gtqualityneo.integration.jade;

import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum GT6PartPortsProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;
    static final String DATA = "gtqualityneo.part_ports";
    static final Identifier UID = Identifier.fromNamespaceAndPath("gtqualityneo", "part_ports");
    @Override public Identifier getUid() { return UID; }
    @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof MultiTileEntityMultiBlockPart part) {
            CompoundTag ports = new CompoundTag();
            PartPorts.write(part, ports);
            data.put(DATA, ports);
        }
    }
}
