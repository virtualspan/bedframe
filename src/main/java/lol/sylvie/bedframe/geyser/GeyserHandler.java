package lol.sylvie.bedframe.geyser;

import lol.sylvie.bedframe.BedframeInitializer;
import lol.sylvie.bedframe.api.Bedframe;
import lol.sylvie.bedframe.geyser.translator.BlockTranslator;
import lol.sylvie.bedframe.geyser.translator.ItemTranslator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.geysermc.api.Geyser;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class GeyserHandler implements EventRegistrar {
    private final Bedframe bedframe;
    private final PackGenerator packGenerator;
    private final ArrayList<Translator> translators = new ArrayList<>();

    private boolean generatedResources = false;

    public GeyserHandler(Bedframe bedframe) {
        this.bedframe = bedframe;
        this.packGenerator = new PackGenerator(this.bedframe);

        translators.add(new BlockTranslator(bedframe));
        translators.add(new ItemTranslator(bedframe));
    }

    public void registerHooks() {
        if (!Geyser.isRegistered()) {
            ServerLifecycleEvents.SERVER_STARTING.register(ignored -> registerHooks());
            return;
        }

        Path packSourceDir;
        Path resourcePackDir;
        try {
            packSourceDir = Files.createTempDirectory("bedframe");

            resourcePackDir = BedframeInitializer.CONFIG_DIR.resolve("resource-packs");
            if (Files.notExists(resourcePackDir))
                Files.createDirectory(resourcePackDir);

        } catch (IOException e) { throw new RuntimeException(e); };
        Path resourcePack = resourcePackDir.resolve(bedframe.getModId() + ".zip");

        EventBus<EventRegistrar> eventBus = GeyserApi.api().eventBus();
        for (Translator translator : translators) {
            translator.register(eventBus, packSourceDir);
        }

        eventBus.subscribe(this, GeyserDefineResourcePacksEvent.class, event -> {
            try {
                // For some reason, GeyserDefineResourcePacksEvent is called *before* blocks
                // FIXME: It's probably better to generate resources before the event is ever called
                if (!translators.stream().allMatch(Translator::hasProvidedResources)) return;

                // This event is called multiple times, but regenerating the resource pack each time is redundant
                if (!generatedResources) {
                    Files.deleteIfExists(resourcePack);
                    packGenerator.generatePack(packSourceDir, resourcePack.toFile(), translators);
                    generatedResources = true;
                }

                event.register(ResourcePack.create(PackCodec.path(resourcePack)));
            } catch (IOException e) { throw new RuntimeException(e); }
        });
    }
}
