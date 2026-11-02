package com.plainston.gtqualityneo.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.plainston.gtqualityneo.client.loading.GtLoadingProgress;
import gregapi.api.Abstract_Mod;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Abstract_Mod.class, remap = false)
public abstract class GtLoadingServerMixin {
    @WrapMethod(method = "onModServerStarting")
    private void gtqualityneo$starting(ServerStartingEvent event, Operation<Void> original) {
        try (var progress = GtLoadingProgress.begin("server_start", 0)) { original.call(event); }
    }
    @WrapMethod(method = "onModServerStarted")
    private void gtqualityneo$started(ServerStartedEvent event, Operation<Void> original) {
        try (var progress = GtLoadingProgress.begin("server_ready", 0)) { original.call(event); }
    }
}
