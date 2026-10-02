package com.plainston.gtqualityneo.integration.jade;

import com.plainston.gtqualityneo.GTQualityNeo;

import gregapi.tileentity.base.TileEntityBase01Root;
import gregapi.block.multitileentity.example.MultiTileEntityChest;
import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import gregtech.tileentity.inventories.MultiTileEntityDrawerQuad;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin("gtqualityneo")
public final class GTQualityJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        if (!GTQualityNeo.JADE_INTEGRATION.get()) return;
        registration.registerBlockDataProvider(GT6MachineProvider.INSTANCE, TileEntityBase01Root.class);
        registration.registerBlockDataProvider(GT6DetailsProvider.INSTANCE, TileEntityBase01Root.class);
        registration.registerItemStorage(GT6ItemStorageProvider.INSTANCE, MultiTileEntityMassStorage.class);
        registration.registerItemStorage(GT6ItemStorageProvider.INSTANCE, MultiTileEntityChest.class);
        registration.registerItemStorage(GT6ItemStorageProvider.INSTANCE, MultiTileEntityDrawerQuad.class);
        registration.registerFluidStorage(GT6FluidStorageProvider.INSTANCE, TileEntityBase01Root.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (!GTQualityNeo.JADE_INTEGRATION.get()) return;
        registration.registerBlockComponent(GT6MachineComponentProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(GT6DetailsComponentProvider.INSTANCE, Block.class);
        registration.registerItemStorageClient(GT6ItemStorageClientProvider.INSTANCE);
        registration.registerFluidStorageClient(GT6FluidStorageClientProvider.INSTANCE);
        registration.registerBlockComponent(GT6HarvestProvider.INSTANCE, Block.class);
    }
}
