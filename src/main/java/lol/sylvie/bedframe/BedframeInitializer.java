package lol.sylvie.bedframe;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import lol.sylvie.bedframe.geyser.TranslationManager;
import lol.sylvie.bedframe.polymc.GeyserPolyMapHandler;
import lol.sylvie.bedframe.polymc.GeyserTracker;
import lol.sylvie.bedframe.util.ResourceHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

/**
 * Main mod initializer for Bedframe.
 * Wires Polymer resource pack builder into ResourceHelper,
 * registers Geyser player tracking, and sets up translation manager hooks.
 */
public class BedframeInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        // Capture Polymer resource pack builder once created
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(resourcePackBuilder -> {
            ResourceHelper.PACK_BUILDER = resourcePackBuilder;
        });

        // Register Geyser player tracker early
        GeyserTracker.register();

        // Hook PolyMapProvider once the server has started, if PolyMC is present
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
            if (FabricLoader.getInstance().isModLoaded("polymc")) {
                GeyserPolyMapHandler.register(server);
            }
        });

        // Register translation manager hooks when server is starting
        ServerLifecycleEvents.SERVER_STARTING.register(ignored -> {
            TranslationManager manager = new TranslationManager();
            manager.registerHooks();
        });
    }
}