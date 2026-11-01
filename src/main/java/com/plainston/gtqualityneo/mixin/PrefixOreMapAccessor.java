package com.plainston.gtqualityneo.mixin;

import gregapi.block.prefixblock.PrefixBlockOreMap;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PrefixBlockOreMap.class, remap = false)
public interface PrefixOreMapAccessor {
    @Accessor("mMap") Int2ShortOpenHashMap gtqualityneo$entries();
}
