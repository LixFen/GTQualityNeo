package com.plainston.gtqualityneo.integration.jade;

import java.util.List;

import gregapi.util.UT;
import gregtech.tileentity.inventories.MultiTileEntityDrawerQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

public enum GT6ItemStorageClientProvider implements IClientExtensionProvider<ItemStack, ItemView> {
    INSTANCE;
    @Override public Identifier getUid() { return GT6ItemStorageProvider.UID; }

    @Override public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        if (accessor instanceof BlockAccessor block && block.getBlockEntity() instanceof MultiTileEntityDrawerQuad drawer) {
            var hit = block.getHitResult();
            if (hit == null || hit.getDirection().get3DDataValue() != drawer.mFacing) return List.of();
            var pos = drawer.getBlockPos();
            float[] coords = UT.Code.getFacingCoordsClicked(drawer.mFacing,
                (float) (hit.getLocation().x - pos.getX()), (float) (hit.getLocation().y - pos.getY()),
                (float) (hit.getLocation().z - pos.getZ()));
            int index = drawerIndex(coords[0], coords[1]);
            if (index >= groups.size()) return List.of();
            return ClientViewGroup.map(List.of(groups.get(index)), ItemView::new, null);
        }
        return ClientViewGroup.map(groups, ItemView::new, null);
    }

    static int drawerIndex(float x, float y) {
        return (x > 0.5F ? 1 : 0) | (y > 0.5F ? 2 : 0);
    }
}
