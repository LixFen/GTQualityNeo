package com.plainston.gtqualityneo.scanner.client;

import com.plainston.gtqualityneo.scanner.ScannerIntegration;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.minecraft.resources.Identifier;

public final class ScannerClient {
    private ScannerClient() {}

    public static void register(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, HudConfig.SPEC, "gtqualityneo-scanner-client.toml");
        ScannerIntegration.openScreen = () -> Minecraft.getInstance().setScreen(ScannerScreen.create());
        ScannerIntegration.receiveFrame = ClientState::receive;
        bus.addListener(ScannerClient::layers);
        NeoForge.EVENT_BUS.addListener(ScannerClient::logout);
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientState.clear(); }
    private static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(ScannerIntegration.ID, "target"), (graphics, delta) -> ScanHud.draw(graphics));
    }
}
