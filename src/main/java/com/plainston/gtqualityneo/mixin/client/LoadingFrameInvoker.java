package com.plainston.gtqualityneo.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface LoadingFrameInvoker {
    @Invoker("renderFrame") void gtqualityneo$loadingFrame(boolean advanceGameTime);
}
