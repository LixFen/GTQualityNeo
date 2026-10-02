package com.plainston.gtqualityneo.integration;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.client.CreativeTankScreen;
import gregapi.gui.ContainerCommon;
import gregapi.gui.Slot_Base;
import gregapi.jei.GT6_JEI_CraftingCategory;
import gregapi.tileentity.tools.MultiTileEntityAdvancedCraftingTable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@JeiPlugin
public final class GTQualityJeiPlugin implements IModPlugin {
    @Override public Identifier getPluginUid() { return Identifier.fromNamespaceAndPath(GTQualityNeo.MOD_ID, "qol"); }
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new TableTransfer<>(GT6_JEI_CraftingCategory.TYPE));
        registration.addRecipeTransferHandler(new TableTransfer<>(RecipeTypes.CRAFTING));
    }
    private record TableTransfer<R>(IRecipeType<R> type) implements IRecipeTransferInfo<ContainerCommon, R> {
        @Override public Class<? extends ContainerCommon> getContainerClass() { return ContainerCommon.class; }
        @Override public Optional<MenuType<ContainerCommon>> getMenuType() { return Optional.of(ContainerCommon.MENU_TYPE.get()); }
        @Override public IRecipeType<R> getRecipeType() { return type; }
        @Override public boolean canHandle(ContainerCommon menu, R recipe) {
            return menu instanceof MultiTileEntityAdvancedCraftingTable.MultiTileEntityGUICommonAdvancedCraftingTable;
        }
        @Override public List<Slot> getRecipeSlots(ContainerCommon menu, R recipe) {
            return menu.slots.stream().filter(slot -> slot instanceof Slot_Base base && base.mInventory == menu.mTileEntity
                && slot.getSlotIndex() >= 21 && slot.getSlotIndex() <= 29)
                .sorted(Comparator.comparingInt(Slot::getSlotIndex)).toList();
        }
        @Override public List<Slot> getInventorySlots(ContainerCommon menu, R recipe) {
            return menu.slots.stream().filter(slot -> slot.container == menu.mInventoryPlayer
                || slot instanceof Slot_Base base && base.mInventory == menu.mTileEntity && slot.getSlotIndex() <= 20).toList();
        }
    }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(CreativeTankScreen.class, new TankGhost());
    }
    private static final class TankGhost implements IGhostIngredientHandler<CreativeTankScreen> {
        @Override public <I> List<Target<I>> getTargetsTyped(CreativeTankScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
            Object value = ingredient.getIngredient();
            if (!(value instanceof FluidStack || value instanceof ItemStack)) return List.of();
            return List.of(new Target<>() {
                @Override public Rect2i getArea() { return new Rect2i(screen.getLeftPos() + 7, screen.getTopPos() + 19, 18, 18); }
                @Override public void accept(I selected) {
                    if (selected instanceof FluidStack fluid) screen.selectFluid(fluid);
                    else if (selected instanceof ItemStack stack) screen.select(stack);
                }
            });
        }
        @Override public void onComplete() {}
    }
}
