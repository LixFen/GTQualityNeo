package com.plainston.gtqualityneo.scanner;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import java.util.*;

public final class ScannerIntegration {
    public static final String ID = com.plainston.gtqualityneo.GTQualityNeo.MOD_ID;
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final List<java.util.function.Supplier<ScannerItem>> SCANNERS = new ArrayList<>();
    /** Assigned by the client entry point; never resolves a client class on a dedicated server. */
    public static Runnable openScreen = () -> {};
    public static java.util.function.Consumer<ScanNetwork.Frame> receiveFrame = frame -> {};
    static {
        for (int t = 2; t <= 5; t++) {
            final int tier = t;
            String name = "scanner_" + new String[]{"mv", "hv", "ev", "iv"}[t - 2];
            // Resolve saved stacks from GT6 Scanner Modern without changing their components.
            ITEMS.addAlias(Identifier.fromNamespaceAndPath("gt6scan", name), Identifier.fromNamespaceAndPath(ID, name));
            SCANNERS.add(ITEMS.register(name, () -> new ScannerItem(tier,
                    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, name))).stacksTo(1))));
        }
    }
    private ScannerIntegration() {}

    public static void register(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, ScanConfig.SPEC, "gtqualityneo-scanner-server.toml");
        bus.addListener(ScanNetwork::register);
        bus.addListener(ScannerIntegration::creativeTab);
        // Queue after GT6's lifecycle loaders so materials and sensors exist when recipes are built.
        bus.addListener((FMLLoadCompleteEvent event) -> event.enqueueWork(() ->
            gregapi.GT_API.deferItemInit(ScannerRecipes::register)));
        NeoForge.EVENT_BUS.addListener(ScanService::tick);
        NeoForge.EVENT_BUS.addListener(ScanService::stopping);
        NeoForge.EVENT_BUS.addListener(ScanService::logout);
    }
    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES
                || event.getTabKey().identifier().equals(Identifier.fromNamespaceAndPath(ID, "main")))
            SCANNERS.forEach(s -> event.accept(s.get()));
    }
}
