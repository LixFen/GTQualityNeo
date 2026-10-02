package com.plainston.gtqualityneo.integration.jade;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

@ExtendWith(EphemeralTestServerProvider.class)
class GT6StorageTest {
    @BeforeAll static void initializeComponents(MinecraftServer server) {
        // Item components in 26.1 are baked during world loading, after registry bootstrap.
        assertNotNull(server);
    }
    @Test void aggregatesCopiesWithoutChangingInventory() {
        ItemStack first = new ItemStack(Items.IRON_INGOT, 64);
        ItemStack second = new ItemStack(Items.IRON_INGOT, 64);
        ItemStack third = new ItemStack(Items.IRON_INGOT, 2);
        var groups = GT6ItemStorageProvider.collect(new SimpleContainer(first, second, third), 0, 3);
        assertEquals(1, groups.views.size());
        assertEquals(130, groups.views.getFirst().getCount());
        assertEquals(64, first.getCount());
        assertEquals(64, second.getCount());
    }

    @Test void keepsDifferentComponentsSeparate() {
        ItemStack plain = new ItemStack(Items.IRON_INGOT, 10);
        ItemStack named = plain.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Different material"));
        var group = GT6ItemStorageProvider.collect(new SimpleContainer(plain, named), 0, 2);
        assertEquals(2, group.views.size());
        assertEquals(Component.literal("Different material"), group.views.get(1).get(DataComponents.CUSTOM_NAME));
    }

    @Test void avoidsIntegerOverflowWhenAggregatingLargeStorage() {
        ItemStack first = new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE);
        ItemStack second = new ItemStack(Items.IRON_INGOT, 20);
        var group = GT6ItemStorageProvider.collect(new SimpleContainer(first, second), 0, 2);
        assertEquals((long) Integer.MAX_VALUE + 20, group.views.stream().mapToLong(ItemStack::getCount).sum());
        assertTrue(group.views.stream().allMatch(stack -> stack.getCount() > 0));
    }

    @Test void excludesMassStorageTemplateAndBufferSlots() {
        var inventory = new SimpleContainer(new ItemStack(Items.GOLD_INGOT, 1),
            new ItemStack(Items.IRON_INGOT, 1000), new ItemStack(Items.DIAMOND, 5));
        var group = GT6ItemStorageProvider.collect(inventory, 1, 2);
        assertEquals(1, group.views.size());
        assertTrue(group.views.getFirst().is(Items.IRON_INGOT));
        assertEquals(1000, group.views.getFirst().getCount());
    }

    @Test void selectsDrawerQuadrantsAndBoundaryLikeGt6() {
        assertEquals(0, GT6ItemStorageClientProvider.drawerIndex(0.25F, 0.25F));
        assertEquals(1, GT6ItemStorageClientProvider.drawerIndex(0.75F, 0.25F));
        assertEquals(2, GT6ItemStorageClientProvider.drawerIndex(0.25F, 0.75F));
        assertEquals(3, GT6ItemStorageClientProvider.drawerIndex(0.75F, 0.75F));
        assertEquals(0, GT6ItemStorageClientProvider.drawerIndex(0.5F, 0.5F));
    }
}
