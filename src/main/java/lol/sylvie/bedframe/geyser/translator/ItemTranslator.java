package lol.sylvie.bedframe.geyser.translator;

import lol.sylvie.bedframe.geyser.Translator;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import lol.sylvie.bedframe.geyser.item.ItemPackModule;

import java.nio.file.Path;

/**
 * Thin bridge: subscribes to GeyserDefineCustomItemsEvent and delegates to ItemPackModule.
 */
public class ItemTranslator extends Translator {

    private final ItemPackModule module = new ItemPackModule();

    @Override
    public void register(EventBus<EventRegistrar> eventBus, Path packRoot) {
        eventBus.subscribe(this, GeyserDefineCustomItemsEvent.class, event -> module.onDefineCustomItems(event));
    }

    // Utility for mixins to check if an item is registered
    public static boolean isTexturedItem(net.minecraft.item.Item item) {
        return ItemPackModule.isRegistered(item);
    }
}
