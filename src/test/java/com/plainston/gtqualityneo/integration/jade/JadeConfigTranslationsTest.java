package com.plainston.gtqualityneo.integration.jade;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IWailaClientRegistration;

@ExtendWith(EphemeralTestServerProvider.class)
class JadeConfigTranslationsTest {
    @BeforeAll static void initialize(MinecraftServer server) { assertNotNull(server); }
    @Test void allRegisteredHudProvidersHaveConfigLabelsInBothLanguages() throws Exception {
        var keys = new ArrayList<String>();
        var registration = (IWailaClientRegistration) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {IWailaClientRegistration.class}, (proxy, method, args) -> {
                if (method.getName().equals("registerBlockComponent") && args[0] instanceof IBlockComponentProvider provider) {
                    var uid = provider.getUid();
                    keys.add("config.jade.plugin_" + uid.getNamespace() + "." + uid.getPath());
                }
                return null;
            });
        new GTQualityJadePlugin().registerClient(registration);
        assertFalse(keys.isEmpty());
        for (var language : new String[] {"en_us", "zh_cn"}) {
            try (var stream = getClass().getResourceAsStream("/assets/gtqualityneo/lang/" + language + ".json")) {
                assertNotNull(stream);
                var translations = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var key : keys) assertTrue(translations.has(key) && !translations.get(key).getAsString().isBlank(), language + ": " + key);
            }
        }
    }
}
