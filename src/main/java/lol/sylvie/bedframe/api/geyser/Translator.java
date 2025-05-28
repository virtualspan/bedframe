package lol.sylvie.bedframe.api.geyser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lol.sylvie.bedframe.api.Bedframe;
import lol.sylvie.bedframe.api.BedframeBlock;
import net.minecraft.util.Identifier;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import xyz.nucleoid.server.translations.api.language.TranslationAccess;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Converts Java objects to Bedrock equivalents
 */
public abstract class Translator implements EventRegistrar {
    protected final Bedframe bedframe;

    public Translator(Bedframe bedframe) {
        this.bedframe = bedframe;
    }

    /**
     * Registers the Geyser events required of this translator
     * Translators are expected to write to the resource pack here as well.
     */
    public abstract void register(EventBus<EventRegistrar> eventBus, Path packRoot, TranslationAccess translations, FileWriter writer);

    // Helper methods
    protected void forEachKey(JsonObject root, BiConsumer<String, JsonElement> function) {
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            function.accept(entry.getKey(), entry.getValue());
        }
    }
}
