package com.plainston.gtqualityneo.client;

import com.plainston.gtqualityneo.GTQualityNeo;
import com.plainston.gtqualityneo.fluid.CreativeTankMenu;
import com.plainston.gtqualityneo.qol.QolNetwork;
import gregapi.block.multitileentity.MultiTileEntityItemInternal;
import gregapi.gui.ContainerCommonBasicMachine;
import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import gregtech.tileentity.tools.MultiTileEntityMold;
import gregtech.tileentity.tools.MultiTileEntityScaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MoverType;
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
    private static Object consumedFluidScreen;

    public QolClient(IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent event) -> {
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM)
                if (item instanceof gregapi.item.multiitem.MultiItemTool)
                    event.register(item, (graphics, font, stack, x, y) -> {
                        ToolBars.draw(graphics, stack, x, y);
                        return false;
                    });
        });
        bus.addListener((net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event) -> {
            event.register(QolNetwork.OpenCircuit.TYPE, (packet, context) ->
                Minecraft.getInstance().setScreen(new CircuitScreen(packet)));
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
        NeoForge.EVENT_BUS.addListener(QolClient::filterTooltip);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) ->
            com.plainston.gtqualityneo.qol.ObstructionConfig.apply());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
            com.plainston.gtqualityneo.qol.ObstructionConfig.restore());
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonReleased.Pre event) -> {
            if (consumedFluidScreen == event.getScreen() && (event.getButton() == 0 || event.getButton() == 1)) {
                consumedFluidScreen = null;
                event.setCanceled(true);
            }
        });
    }

    private static void tooltip(ItemTooltipEvent event) {
        if (GTQualityNeo.SERVER_CONFIG.isLoaded() && GTQualityNeo.CIRCUIT_SELECTOR.get()
            && com.plainston.gtqualityneo.qol.CircuitInteraction.isCircuit(event.getItemStack()))
            event.getToolTip().add(Component.translatable("gtqualityneo.circuit.tooltip"));
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

    private static void filterTooltip(ScreenEvent.Render.Foreground event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        double x = event.getMouseX() - screen.getLeftPos(), y = event.getMouseY() - screen.getTopPos();
        for (var slot : screen.getMenu().slots) if (com.plainston.gtqualityneo.qol.FilterGhost.isFilterSlot(screen.getMenu(), slot.index)
            && x >= slot.x - 1 && x < slot.x + 17 && y >= slot.y - 1 && y < slot.y + 17) {
            var lines = new java.util.ArrayList<Component>();
            if (slot.hasItem()) lines.addAll(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(Minecraft.getInstance(), slot.getItem()));
            lines.add(Component.translatable("gtqualityneo.jei.filter.ghost").withStyle(net.minecraft.ChatFormatting.GRAY));
            event.getGuiGraphics().setComponentTooltipForNextFrame(Minecraft.getInstance().font, lines,
                event.getMouseX(), event.getMouseY());
            return;
        }
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
        consumedFluidScreen = null;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
            || !(screen.getMenu() instanceof ContainerCommonBasicMachine menu) || menu.getCarried().isEmpty()
            || event.getButton() < 0 || event.getButton() > 1
            || !GTQualityNeo.SERVER_CONFIG.isLoaded() || !GTQualityNeo.GUI_FLUID_INTERACTION.get()) return;
        double x = event.getMouseX() - screen.getLeftPos(), y = event.getMouseY() - screen.getTopPos();
        for (var slot : menu.slots) if (com.plainston.gtqualityneo.fluid.GuiFluidInteraction.isFluidSlot(menu, slot.index)
            && x >= slot.x - 1 && x < slot.x + 17 && y >= slot.y - 1 && y < slot.y + 17) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new QolNetwork.ClickFluid(
                menu.containerId, slot.index, event.getButton(), event.getMouseButtonEvent().hasShiftDown()));
            consumedFluidScreen = screen;
            event.setCanceled(true);
            return;
        }
    }
}
