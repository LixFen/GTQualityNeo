package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.client.ToolBars;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiGraphicsExtractor.class, remap = false)
public abstract class ToolDecorationMixin {
    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void gtquality$bars(Font font, ItemStack stack, int x, int y, String count, CallbackInfo ci) {
        ToolBars.draw((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }
}
