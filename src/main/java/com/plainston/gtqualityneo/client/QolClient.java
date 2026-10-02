package com.plainston.gtqualityneo.client;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.fluid.CreativeTankMenu;
import com.plainston.gtqualityneo.qol.QolNetwork;
import gregapi.block.multitileentity.MultiTileEntityItemInternal;
import gregapi.gui.ContainerCommonBasicMachine;
import gregapi.gui.Slot_Render;
import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import gregtech.tileentity.tools.MultiTileEntityMold;
import gregtech.tileentity.tools.MultiTileEntityScaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@Mod(value = GTQualityNeo.MOD_ID, dist = Dist.CLIENT)
public final class QolClient {
    private static Object consumedShiftScreen;

    public QolClient(IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event) -> {
            event.register(QolNetwork.OpenMold.TYPE, (packet, context) ->
                Minecraft.getInstance().setScreen(new MoldScreen(packet)));
            event.register(QolNetwork.TankState.TYPE, (packet, context) -> {
                var player = Minecraft.getInstance().player;
                if (player != null && player.containerMenu instanceof CreativeTankMenu menu && menu.containerId == packet.window())
                    menu.settings = packet.settings();
            });
        });
        bus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
            event.register(CreativeTankMenu.TYPE.get(), CreativeTankScreen::new));
        NeoForge.EVENT_BUS.addListener(QolClient::tooltip);
        NeoForge.EVENT_BUS.addListener(QolClient::climb);
        NeoForge.EVENT_BUS.addListener(QolClient::fluidClick);
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonReleased.Pre event) -> {
            if (consumedShiftScreen == event.getScreen() && (event.getButton() == 0 || event.getButton() == 1)) {
                consumedShiftScreen = null;
                event.setCanceled(true);
            }
        });
    }

    private static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(com.plainston.gtqualityneo.fluid.CreativeTank.ITEM.get())) {
            event.getToolTip().add(Component.translatable("gtqualityneo.creative_tank.tip"));
            return;
        }
        if (!(event.getItemStack().getItem() instanceof MultiTileEntityItemInternal item)) return;
        var container = item.mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(event.getItemStack());
        if (container == null) return;
        if (container.mTileEntity.getClass() == MultiTileEntityMold.class)
            event.getToolTip().add(Component.translatable("gtqualityneo.tooltip.mold").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        else if (container.mTileEntity instanceof MultiTileEntityMassStorage)
            event.getToolTip().add(Component.translatable("gtqualityneo.tooltip.storage").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    private static void climb(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || player.isShiftKeyDown() || !player.onClimbable()
            || !(player.level().getBlockEntity(player.blockPosition()) instanceof MultiTileEntityScaffold)) return;
        double distance = 0;
        if (player.getXRot() < 0 && player.zza > 0) distance = -player.getXRot() / 90.0 * GTQualityNeo.SCAFFOLD_UP.get();
        else if (player.getXRot() > 0 && player.zza == 0) distance = -player.getXRot() / 90.0 * GTQualityNeo.SCAFFOLD_DOWN.get();
        if (distance != 0) player.move(MoverType.SELF, new Vec3(0, distance, 0));
    }

    private static void fluidClick(ScreenEvent.MouseButtonPressed.Pre event) {
        consumedShiftScreen = null;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
            || !(screen.getMenu() instanceof ContainerCommonBasicMachine menu) || menu.getCarried().isEmpty()
            || !event.getMouseButtonEvent().hasShiftDown() || event.getButton() < 0 || event.getButton() > 1
            || !GTQualityNeo.SERVER_CONFIG.isLoaded() || !GTQualityNeo.GUI_FLUID_INTERACTION.get()) return;
        double x = event.getMouseX() - screen.getLeftPos(), y = event.getMouseY() - screen.getTopPos();
        for (var slot : menu.slots) if (slot instanceof Slot_Render
            && x >= slot.x - 1 && x < slot.x + 17 && y >= slot.y - 1 && y < slot.y + 17) {
            var minecraft = Minecraft.getInstance();
            minecraft.gameMode.handleContainerInput(menu.containerId, slot.index, event.getButton(), ContainerInput.QUICK_MOVE, minecraft.player);
            consumedShiftScreen = screen;
            event.setCanceled(true);
            return;
        }
    }
}
