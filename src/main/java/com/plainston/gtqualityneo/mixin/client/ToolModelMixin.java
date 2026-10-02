package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.client.ToolBars;
import gregapi.render.GT6ItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GT6ItemModel.class, remap = false)
public abstract class ToolModelMixin {
    @Shadow private static boolean isBarOverlayIcon(Identifier icon) { throw new AssertionError(); }
    @Inject(method = "iconForPass", at = @At("RETURN"), cancellable = true)
    private static void gtquality$hideLegacyBars(Object item, ItemStack stack, int pass, CallbackInfoReturnable<Identifier> cir) {
        if (ToolBars.enabled(stack) && isBarOverlayIcon(cir.getReturnValue())) cir.setReturnValue(null);
    }
}
