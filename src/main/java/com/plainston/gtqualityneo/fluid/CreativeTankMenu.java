package com.plainston.gtqualityneo.fluid;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.qol.QolNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class CreativeTankMenu extends AbstractContainerMenu {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GTQualityNeo.MOD_ID);
    public static final Supplier<MenuType<CreativeTankMenu>> TYPE = MENUS.register("creative_tank", () -> IMenuTypeExtension.create(
        (id, inventory, buffer) -> new CreativeTankMenu(id, inventory, (CreativeTank) inventory.player.level().getBlockEntity(buffer.readBlockPos()))));
    public final CreativeTank tank;
    public CreativeTank.Settings settings = CreativeTank.Settings.DEFAULT;
    private CreativeTank.Settings lastSettings;
    private final Player player;

    public static void register(IEventBus bus) { MENUS.register(bus); }
    public CreativeTankMenu(int id, Inventory inventory, CreativeTank tank) {
        super(TYPE.get(), id);
        this.tank = tank;
        this.player = inventory.player;
        if (tank != null) settings = tank.settings();
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 102 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 160));
    }
    @Override public boolean stillValid(Player player) {
        return tank != null && !tank.isRemoved() && player.level().getBlockEntity(tank.getBlockPos()) == tank
            && player.isWithinBlockInteractionRange(tank.getBlockPos(), 0);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (tank != null && player instanceof ServerPlayer server && !tank.settings().equals(lastSettings)) {
            lastSettings = tank.settings();
            PacketDistributor.sendToPlayer(server, new QolNetwork.TankState(containerId, lastSettings));
        }
    }
}
