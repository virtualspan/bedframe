package lol.sylvie.bedframe;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import lol.sylvie.bedframe.geyser.TranslationManager;
import lol.sylvie.bedframe.util.BedframeConstants;
import lol.sylvie.bedframe.util.ResourceHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import org.geysermc.pack.converter.util.DefaultLogListener;
import org.geysermc.pack.converter.util.VanillaPackProvider;

import java.nio.file.Path;
import java.util.function.Consumer;

import static lol.sylvie.bedframe.util.BedframeConstants.*;

public class BedframeInitializer implements ModInitializer {
	@Override
	public void onInitialize() {
		LOGGER.info("Bedframe - {}", METADATA.getVersion().getFriendlyString());
		LOGGER.info("Contributors: {}", String.join(", ", METADATA.getAuthors().stream().map(Person::getName).toList()));

		ServerLifecycleEvents.SERVER_STARTING.register(ignored -> {
			TranslationManager manager = new TranslationManager();
			manager.registerHooks();
		});

		PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(resourcePackBuilder -> {
			ResourceHelper.PACK_BUILDER = resourcePackBuilder;
        });
	}
}