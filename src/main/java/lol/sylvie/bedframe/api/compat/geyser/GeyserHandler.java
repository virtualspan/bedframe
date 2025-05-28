package lol.sylvie.bedframe.api.compat.geyser;

import lol.sylvie.bedframe.BedframeInitializer;
import lol.sylvie.bedframe.api.Bedframe;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.geysermc.api.Geyser;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles Geyser integration for a specific {@link lol.sylvie.bedframe.api.Bedframe} mod
 */
public class GeyserHandler implements EventRegistrar {
    // This class should be static, but I have to do it in this weird way to have an EventRegistrar
    // TODO: Figure out a way to not do that
    private final Bedframe bedframe;

    private boolean hasGeneratedResourcePacks = false;

    public GeyserHandler(Bedframe bedframe) {
        this.bedframe = bedframe;
    }

    public void registerGeyserHooks() {
        // This will always be unregistered the first time it's called, but this saves us a nest.
        if (!Geyser.isRegistered()) {
            ServerLifecycleEvents.SERVER_STARTING.register(ignored -> registerGeyserHooks());
            return;
        }

        EventBus<EventRegistrar> eventBus = GeyserApi.api().eventBus();
        eventBus.subscribe(this, GeyserDefineCustomItemsEvent.class, event -> {
            BedrockItemTranslator.register(bedframe, event);
        });

        eventBus.subscribe(this, GeyserDefineCustomBlocksEvent.class, event -> {
            BedrockBlockTranslator.register(bedframe, event);
        });

        eventBus.subscribe(this, GeyserDefineResourcePacksEvent.class, event -> {
            try {
                Path resourcePackDir = BedframeInitializer.CONFIG_DIR.resolve("resource-packs");
                if (Files.notExists(resourcePackDir))
                    Files.createDirectory(resourcePackDir);

                // This event is called multiple times, but regenerating the resource pack each time is redundant
                Path resourcePack = resourcePackDir.resolve(bedframe.getModId() + ".zip");
                if (!hasGeneratedResourcePacks) {
                    Files.deleteIfExists(resourcePack);
                    BedrockPackGenerator.generate(bedframe, resourcePack);
                    hasGeneratedResourcePacks = true;
                }

                event.register(ResourcePack.create(PackCodec.path(resourcePack)));
            } catch (IOException ignored) {}
        });
    }

    public interface ResourceProvider {
        void writeToPack(Bedframe bedframe, Path path);
    }
}
