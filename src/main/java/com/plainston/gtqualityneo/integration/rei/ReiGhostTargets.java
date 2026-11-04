package com.plainston.gtqualityneo.integration.rei;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.client.screen.CreativeTankScreen;
import com.plainston.gtqualityneo.client.ui.FixedInventoryScreen;
import com.plainston.gtqualityneo.qol.FilterGhost;
import com.plainston.gtqualityneo.qol.QolNetwork;
import gregapi.data.FL;
import gregapi.gui.ContainerClient;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;
import java.util.stream.Stream;

final class ReiGhostTargets implements DraggableStackVisitor<Screen> {
    private record Target(Rectangle bounds, int slot) {}

    @Override public <T extends Screen> boolean isHandingScreen(T screen) {
        if (screen instanceof CreativeTankScreen) return GTQualityNeo.CREATIVE_TANK.get();
        return GTQualityNeo.FILTER_GHOST.get() && (screen instanceof ContainerClient || screen instanceof FixedInventoryScreen);
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<Screen> context, DraggableStack stack) {
        return targets(context.getScreen(), stack).stream().map(target -> BoundsProvider.ofRectangle(target.bounds()));
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(DraggingContext<Screen> context, DraggableStack stack) {
        var screen = context.getScreen();
        var point = context.getCurrentPosition();
        for (var target : targets(screen, stack)) {
            if (!target.bounds().contains(point)) continue;
            var value = stack.getStack().getValue();
            if (screen instanceof CreativeTankScreen tank) {
                if (value instanceof ItemStack item) {
                    if (!tank.select(item)) return DraggedAcceptorResult.PASS;
                } else tank.selectFluid(fluid((dev.architectury.fluid.FluidStack) value));
            } else {
                var menu = ((AbstractContainerScreen<?>) screen).getMenu();
                var item = value instanceof ItemStack selected ? selected.copyWithCount(1)
                    : FL.display(fluid((dev.architectury.fluid.FluidStack) value), false, false);
                if (item == null || item.isEmpty()) return DraggedAcceptorResult.PASS;
                ClientPacketDistributor.sendToServer(new QolNetwork.SelectFilter(menu.containerId, target.slot(), item));
            }
            return DraggedAcceptorResult.ACCEPTED;
        }
        return DraggedAcceptorResult.PASS;
    }

    private List<Target> targets(Screen screen, DraggableStack drag) {
        var stack = drag.getStack();
        if (!isHandingScreen(screen) || stack.isEmpty()
            || (stack.getType() != VanillaEntryTypes.ITEM && stack.getType() != VanillaEntryTypes.FLUID)) return List.of();
        if (screen instanceof CreativeTankScreen tank)
            return List.of(new Target(new Rectangle(tank.getLeftPos() + 7, tank.getTopPos() + 19, 18, 18), -1));
        var container = (AbstractContainerScreen<?>) screen;
        var menu = container.getMenu();
        return menu.slots.stream().filter(slot -> FilterGhost.isFilterSlot(menu, slot.index))
            .map(slot -> new Target(new Rectangle(container.getLeftPos() + slot.x - 1,
                container.getTopPos() + slot.y - 1, 18, 18), slot.index)).toList();
    }

    private static FluidStack fluid(dev.architectury.fluid.FluidStack stack) {
        return new FluidStack(stack.getFluid(), (int) Math.clamp(stack.getAmount(), 1L, Integer.MAX_VALUE), stack.getPatch());
    }
}
