package com.plainston.gtqualityneo.integration.rei;

import com.plainston.gtqualityneo.integration.WorkbenchTransfer;
import com.plainston.gtqualityneo.qol.QolNetwork;
import gregapi.item.IItemGTContainerTool;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerMeta;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.transfer.info.stack.PlayerInventorySlotAccessor;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.api.common.transfer.info.stack.VanillaSlotAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Uses other plugins' public slot declarations instead of maintaining a list of supported menus. */
final class ReiWorkbenchTransfer implements TransferHandler, TransferHandlerMeta {
    private final TransferHandlerRegistry registry;

    ReiWorkbenchTransfer(TransferHandlerRegistry registry) { this.registry = registry; }

    @Override public double getPriority() { return 100; }

    @Override public ApplicabilityResult checkApplicable(Context context) {
        return context.getMenu() != null && context.getMinecraft().player != null
            && context.getDisplay().getCategoryIdentifier().getIdentifier().toString().equals("minecraft:plugins/crafting")
            && context.getDisplay().getInputEntries().stream().flatMap(entry -> entry.stream())
                .anyMatch(entry -> entry.getType() == VanillaEntryTypes.ITEM
                    && entry.<ItemStack>castValue().getItem() instanceof IItemGTContainerTool)
            ? ApplicabilityResult.createApplicable() : ApplicabilityResult.createNotApplicable();
    }

    @Override public Result handle(Context context) {
        var menu = context.getMenu();
        var player = context.getMinecraft().player;
        var layout = locate(context);
        if (layout == null) return Result.createNotApplicable();
        var grid = layout.grid();
        var inventory = layout.inventory();
        var declared = layout.declared();
        var ingredients = new ArrayList<List<ItemStack>>(Collections.nCopies(grid.size(), List.of()));
        if (menu instanceof AbstractCraftingMenu crafting && crafting.getGridWidth() != 3
            && context.getDisplay() instanceof SimpleGridMenuDisplay display) {
            int width = display.getInputWidth(crafting.getGridWidth(), crafting.getGridHeight());
            int height = display.getInputHeight(crafting.getGridWidth(), crafting.getGridHeight());
            if (width <= 0 || width > crafting.getGridWidth() || height > crafting.getGridHeight())
                return Result.createFailed(Component.translatable("error.rei.transfer.too_small",
                    crafting.getGridWidth(), crafting.getGridHeight())).blocksFurtherHandling();
            var entries = display.getInputEntries();
            for (int i = 0; i < entries.size(); i++) {
                int index = i % width + i / width * crafting.getGridWidth();
                if (index >= ingredients.size()) return Result.createNotApplicable();
                ingredients.set(index, InputIngredient.withType(InputIngredient.of(index, entries.get(i)), VanillaEntryTypes.ITEM).get());
            }
        } else {
            var inputs = declared != null ? declared.getInputsIndexed(context)
                : context.getDisplay().getInputIngredients(menu, player).stream()
                    .map(input -> InputIngredient.withType(input, VanillaEntryTypes.ITEM)).toList();
            for (var input : inputs) {
                if (input.getIndex() < 0 || input.getIndex() >= grid.size()) return Result.createNotApplicable();
                ingredients.set(input.getIndex(), input.get());
            }
        }
        var plan = WorkbenchTransfer.plan(grid, inventory, player, ingredients, context.isStackedCrafting(), true);
        if (plan == null)
            return Result.createFailed(Component.translatable("error.rei.not.enough.materials")).blocksFurtherHandling();
        if (context.isActuallyCrafting()) {
            ClientPacketDistributor.sendToServer(new QolNetwork.TransferWorkbench(menu.containerId,
                plan.slots().stream().map(slot -> slot.index).toList(), grid.size(), plan.inputs()));
            context.getMinecraft().setScreenAndShow(context.getContainerScreen());
        }
        return Result.createSuccessful();
    }

    @Override public Iterable<ItemStack> getAvailableIngredients(Context context) {
        var layout = locate(context);
        if (layout == null) return List.of();
        var available = new ArrayList<ItemStack>();
        layout.grid().forEach(slot -> available.add(slot.getItem()));
        layout.inventory().forEach(slot -> available.add(slot.getItem()));
        return available;
    }

    private record Layout(List<Slot> grid, List<Slot> inventory, SimpleTransferHandler declared) {}

    private Layout locate(Context context) {
        var menu = context.getMenu();
        var player = context.getMinecraft().player;
        if (menu == null || player == null) return null;
        List<Slot> grid = null, inventory = null;
        SimpleTransferHandler declared = null;
        for (var handler : registry) {
            if (!(handler instanceof SimpleTransferHandler simple) || !simple.checkApplicable(context).isSuccessful()) continue;
            grid = slots(menu, simple.getInputSlots(context), player.getInventory());
            inventory = slots(menu, simple.getInventorySlots(context), player.getInventory());
            if (grid != null && inventory != null) {
                declared = simple;
                break;
            }
        }
        // REI's 2x2 adapter is not itself a SimpleTransferHandler. Vanilla exposes its grid publicly.
        if ((grid == null || inventory == null) && menu instanceof AbstractCraftingMenu crafting) {
            grid = crafting.getInputGridSlots();
            inventory = menu.slots.stream().filter(slot -> slot.container == player.getInventory()
                && slot.getSlotIndex() < player.getInventory().getNonEquipmentItems().size()).toList();
        }
        if (grid == null || inventory == null) return null;
        // Some declarations include grid slots as inventory sources; count each physical slot once.
        var inputSlots = grid;
        inventory = inventory.stream().filter(slot -> !inputSlots.contains(slot)).distinct().toList();
        return new Layout(grid, inventory, declared);
    }

    private static List<Slot> slots(AbstractContainerMenu menu, Iterable<SlotAccessor> accessors,
                                    net.minecraft.world.entity.player.Inventory inventory) {
        var slots = new ArrayList<Slot>();
        for (var accessor : accessors) {
            Slot slot;
            if (accessor instanceof VanillaSlotAccessor vanilla) slot = vanilla.getSlot();
            else if (accessor instanceof PlayerInventorySlotAccessor player) slot = menu.slots.stream()
                .filter(candidate -> candidate.container == inventory && candidate.getSlotIndex() == player.getIndex())
                .findFirst().orElse(null);
            else return null;
            if (slot == null || !menu.slots.contains(slot)) return null;
            if (!slots.contains(slot)) slots.add(slot);
        }
        return slots;
    }
}
