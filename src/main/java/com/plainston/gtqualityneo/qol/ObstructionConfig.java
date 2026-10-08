package com.plainston.gtqualityneo.qol;

import com.plainston.gtqualityneo.GTQualityNeo;
import gregapi.data.CS;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class ObstructionConfig {
    private static Boolean previous;
    private ObstructionConfig() {}
    public static void apply() {
        if (GTQualityNeo.SERVER_CONFIG.isLoaded() && GTQualityNeo.OBSTRUCTED_INTERACTION.get()) {
            if (previous == null) previous = CS.OBSTRUCTION_CHECKS;
            CS.OBSTRUCTION_CHECKS = false;
        } else restore();
    }
    public static void restore() {
        if (previous != null) {
            CS.OBSTRUCTION_CHECKS = previous;
            previous = null;
        }
    }
    public static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != GTQualityNeo.SERVER_CONFIG) return;
        if (event instanceof ModConfigEvent.Unloading) restore();
        else if (event instanceof ModConfigEvent.Reloading) apply();
    }
    public static void serverStarted(ServerStartedEvent event) { apply(); }
}
