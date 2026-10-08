package com.plainston.gtqualityneo.integration.jade;

import java.util.ArrayList;
import java.util.List;

import gregapi.fluid.FluidTankGT;
import gregapi.tileentity.base.TileEntityBase01Root;
import gregapi.tileentity.tank.TileEntityBase08FluidContainer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.IFluidTank;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

/** The native GT6 tanks preserve long quantities that capability adapters can clamp to int. */
public enum GT6FluidStorageProvider implements IServerExtensionProvider<FluidView.Data> {
    INSTANCE;
    static final Identifier UID = Identifier.fromNamespaceAndPath("gtqualityneo", "fluid_storage");
    @Override public Identifier getUid() { return UID; }
    @Override public int getDefaultPriority() { return -100; }

    @Override public List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        IFluidTank[] tanks;
        if (accessor.getTarget() instanceof TileEntityBase08FluidContainer container) tanks = new IFluidTank[] { container.mTank };
        else if (accessor.getTarget() instanceof gregtech.tileentity.multiblocks.MultiTileEntityTank tank)
            tanks = new IFluidTank[] { tank.mTank };
        else if (accessor.getTarget() instanceof TileEntityBase01Root root) tanks = root.getFluidTanksForCapability(null);
        else return null;
        if (tanks == null || tanks.length == 0) return null;
        List<FluidView.Data> fluids = new ArrayList<>();
        for (IFluidTank tank : tanks) {
            if (tank == null) continue;
            var fluid = tank.getFluid();
            if (fluid == null || fluid.isEmpty()) continue;
            long amount = tank instanceof FluidTankGT gt ? gt.amount() : fluid.getAmount();
            long capacity = tank instanceof FluidTankGT gt ? gt.capacity() : tank.getCapacity();
            fluids.add(new FluidView.Data(JadeFluidObject.of(fluid.getFluid(), amount, fluid.getComponentsPatch()), capacity));
        }
        return fluids.isEmpty() ? List.of() : List.of(new ViewGroup<>(fluids));
    }
}
