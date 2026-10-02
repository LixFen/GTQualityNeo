package com.plainston.gtqualityneo.client;

import static org.junit.jupiter.api.Assertions.*;
import com.plainston.gtqualityneo.GTQualityNeo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.junit.jupiter.api.Test;

class ClientInputTest {
    private static final class MenuScreen extends Screen {
        MenuScreen() { super(null, null, Component.empty()); }
    }

    @Test void menuMouseClicksDoNotReadUnloadedServerConfig() throws Exception {
        var spec = GTQualityNeo.SERVER_CONFIG;
        var field = spec.getClass().getDeclaredField("loadedConfig");
        field.setAccessible(true);
        Object previous = field.get(spec);
        var accept = spec.getClass().getMethod("acceptConfig", field.getType());
        accept.invoke(spec, new Object[]{null});
        try {
            assertFalse(spec.isLoaded());
            var handler = QolClient.class.getDeclaredMethod("fluidClick", ScreenEvent.MouseButtonPressed.Pre.class);
            handler.setAccessible(true);
            for (int modifiers : new int[]{0, 1}) {
                var event = new ScreenEvent.MouseButtonPressed.Pre(new MenuScreen(),
                    new MouseButtonEvent(10, 10, new MouseButtonInfo(0, modifiers)), false);
                assertDoesNotThrow(() -> handler.invoke(null, event));
                assertFalse(event.isCanceled());
            }
        } finally { accept.invoke(spec, previous); }
    }
}
