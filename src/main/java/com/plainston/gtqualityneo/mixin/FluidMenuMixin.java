package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.fluid.GuiFluidInteraction;
import gregapi.gui.ContainerCommon;
import gregapi.gui.ContainerCommonBasicMachine;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerCommon.class, remap = false)
public abstract class FluidMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void gtquality$fluidClick(int slot, int button, ContainerInput type, Player player, CallbackInfo ci) {
        if ((Object) this instanceof ContainerCommonBasicMachine menu
            && GuiFluidInteraction.click(menu, slot, button, type, player)) ci.cancel();
    }
}
