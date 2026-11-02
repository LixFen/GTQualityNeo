// 兼容说明：依赖 JEI 29.43 的注册日志调用；升级 JEI 后需复核。
package com.plainston.gtqualityneo.mixin.client;

import com.plainston.gtqualityneo.integration.jei.machine.MachineRecipeManagerPlugin;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** MachineRecipes reports the batch once; preserve other plugins' registration messages. */
@Pseudo
@Mixin(targets = "mezz.jei.library.load.registration.AdvancedRegistration", remap = false)
public abstract class JeiRegistrationLoggingMixin {
    @Redirect(method = "addSimpleRecipeManagerPlugin", at = @At(value = "INVOKE",
        target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"))
    private void gtqualityneo$summarizeMachineRegistration(Logger logger, String message, Object pluginClass) {
        if (pluginClass != MachineRecipeManagerPlugin.class) logger.info(message, pluginClass);
    }
}
