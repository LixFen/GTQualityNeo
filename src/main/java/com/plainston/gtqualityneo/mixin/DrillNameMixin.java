package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.tools.DrillInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class DrillNameMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void gtqualityneo$modeName(CallbackInfoReturnable<Component> cir) {
        var stack = (ItemStack) (Object) this;
        if (DrillInteraction.tier(stack) != 0)
            cir.setReturnValue(Component.translatable("gtqualityneo.drill.name", cir.getReturnValue(), DrillInteraction.label(stack)));
    }
}
