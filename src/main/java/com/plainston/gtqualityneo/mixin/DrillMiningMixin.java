package com.plainston.gtqualityneo.mixin;

import com.plainston.gtqualityneo.tools.DrillMining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class DrillMiningMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private DrillMining.Context gtqualityneo$context;
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void gtqualityneo$capture(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        gtqualityneo$context = DrillMining.capture(player, pos);
    }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void gtqualityneo$harvest(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var context = gtqualityneo$context;
        gtqualityneo$context = null;
        if (cir.getReturnValueZ()) DrillMining.harvest(player, pos, context);
    }
}
