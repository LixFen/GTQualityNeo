package com.plainston.gtqualityneo.integration.ftbquests;

import static org.junit.jupiter.api.Assertions.*;

import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem.ComponentMatchType;
import gregapi.GT_API;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.util.ST;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
class FtbItemMatchingTest {
    @BeforeAll static void initialize(MinecraftServer server) throws Exception {
        if (server.overworld() == null) {
            server.getWorldData().overworldData().setInitialized(true);
            var createLevels = MinecraftServer.class.getDeclaredMethod("createLevels");
            createLevels.setAccessible(true);
            server.submit(() -> {
                try { createLevels.invoke(server); }
                catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
            }).get();
        }
    }

    @Test void defaultModeDistinguishesMaterialsAndForms(MinecraftServer server) {
        var iron = OP.dust.mat(MT.Fe, 1);
        var copper = OP.dust.mat(MT.Cu, 1);
        assertSame(iron.getItem(), copper.getItem(), "Exercise GT6's shared Item with different subtypes");
        assertNotEquals(ST.meta_(iron), ST.meta_(copper));
        assertFalse(matches(iron, copper, ComponentMatchType.NONE, server));
        assertFalse(matches(iron, OP.ingot.mat(MT.Fe, 1), ComponentMatchType.NONE, server));
        assertTrue(matches(iron, OP.dust.mat(MT.Fe, 32), ComponentMatchType.NONE, server));
        assertFalse(matches(iron, ItemStack.EMPTY, ComponentMatchType.NONE, server));
    }

    @Test void defaultModeIgnoresOtherComponentsOnEitherStack(MinecraftServer server) {
        var iron = OP.dust.mat(MT.Fe, 1);
        var changed = iron.copy();
        var nbt = new CompoundTag();
        nbt.putLong("Energy", 1234);
        nbt.putLong("Damage", 17);
        changed.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        changed.set(DataComponents.CUSTOM_NAME, Component.literal("Named iron"));
        assertTrue(matches(iron, changed, ComponentMatchType.NONE, server));
        assertTrue(matches(changed, iron, ComponentMatchType.NONE, server));
        assertFalse(matches(changed, OP.dust.mat(MT.Cu, 1), ComponentMatchType.NONE, server));
        assertNotEquals(iron.getComponents(), changed.getComponents(), "Matching must not strip components");
    }

    @Test void absentSubtypeAndExplicitZeroAreEquivalent(MinecraftServer server) {
        var zero = OP.dust.mat(MT.Fe, 1);
        ST.meta_(zero, 0);
        assertFalse(zero.has(GT_API.SUBTYPE_TYPE));
        var explicitZero = zero.copy();
        explicitZero.set(GT_API.SUBTYPE_TYPE, 0);
        assertTrue(matches(zero, explicitZero, ComponentMatchType.NONE, server));
        assertFalse(matches(zero, OP.dust.mat(MT.Fe, 1), ComponentMatchType.NONE, server));
    }

    @Test void explicitComponentModesKeepFtbRules(MinecraftServer server) {
        var iron = OP.dust.mat(MT.Fe, 1);
        var named = iron.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Named iron"));
        assertTrue(matches(iron, iron.copy(), ComponentMatchType.STRICT, server));
        assertFalse(matches(iron, named, ComponentMatchType.STRICT, server));
        assertTrue(matches(iron, named, ComponentMatchType.FUZZY, server));
        assertFalse(matches(named, iron, ComponentMatchType.FUZZY, server));
        assertFalse(matches(iron, OP.dust.mat(MT.Cu, 1), ComponentMatchType.FUZZY, server));
    }

    @Test void nonGregItemsKeepFtbRules(MinecraftServer server) {
        var iron = new ItemStack(Items.IRON_INGOT);
        var named = iron.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Vanilla iron"));
        assertTrue(matches(iron, named, ComponentMatchType.NONE, server));
        assertFalse(matches(iron, named, ComponentMatchType.STRICT, server));
        assertFalse(matches(iron, new ItemStack(Items.COPPER_INGOT), ComponentMatchType.NONE, server));
    }

    private static boolean matches(ItemStack required, ItemStack offered, ComponentMatchType mode, MinecraftServer server) {
        return ItemMatchingSystem.INSTANCE.doesItemMatch(required, offered, mode, server.registryAccess());
    }
}
