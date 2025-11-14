package lol.sylvie.bedframe.geyser.translator;

import lol.sylvie.bedframe.geyser.Translator;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;

import java.nio.file.Path;

/**
 * Disabled ItemTranslator — Bedframe will not convert items,
 * nor add any mappings, textures, or language keys to the resource pack.
 */
public class ItemTranslator extends Translator {

    public ItemTranslator() {
        // No-op: do not collect or process items
    }

    @Override
    public void register(EventBus<EventRegistrar> eventBus, Path packRoot) {
        // No-op: do not subscribe to item events
    }
}
