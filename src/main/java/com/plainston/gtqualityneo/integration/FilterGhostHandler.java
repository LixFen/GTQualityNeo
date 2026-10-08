package com.plainston.gtqualityneo.integration;

import com.plainston.gtqualityneo.qol.FilterGhost;
import com.plainston.gtqualityneo.qol.QolNetwork;
import gregapi.gui.ContainerClient;
import gregapi.data.FL;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;

final class FilterGhostHandler implements IGhostIngredientHandler<ContainerClient> {
    @Override public <I> List<Target<I>> getTargetsTyped(ContainerClient screen, ITypedIngredient<I> ingredient, boolean doStart) {
        if (!(ingredient.getIngredient() instanceof ItemStack || ingredient.getIngredient() instanceof FluidStack)) return List.of();
        var menu = screen.getMenu();
        List<Target<I>> targets = new ArrayList<>();
        for (var slot : menu.slots) if (FilterGhost.isFilterSlot(menu, slot.index)) targets.add(new Target<>() {
            @Override public Rect2i getArea() { return new Rect2i(screen.getLeftPos() + slot.x - 1, screen.getTopPos() + slot.y - 1, 18, 18); }
            @Override public void accept(I selected) {
                ItemStack stack = selected instanceof ItemStack item ? item.copyWithCount(1)
                    : selected instanceof FluidStack fluid ? FL.display(fluid, false, false) : ItemStack.EMPTY;
                if (stack != null && !stack.isEmpty())
                    ClientPacketDistributor.sendToServer(new QolNetwork.SelectFilter(menu.containerId, slot.index, stack));
            }
        });
        return targets;
    }
    @Override public void onComplete() {}
}
