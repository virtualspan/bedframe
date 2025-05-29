package lol.sylvie.bedframe.geyser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lol.sylvie.bedframe.api.Bedframe;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import xyz.nucleoid.server.translations.api.language.ServerLanguage;
import xyz.nucleoid.server.translations.api.language.ServerLanguageDefinition;
import xyz.nucleoid.server.translations.api.language.TranslationAccess;
import xyz.nucleoid.server.translations.impl.ServerTranslations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;

import static lol.sylvie.bedframe.util.BedframeConstants.GSON;

/**
 * Converts Java objects to Bedrock equivalents
 */
public abstract class Translator implements EventRegistrar {
    protected final Bedframe bedframe;
    private boolean providedResources = false;

    public Translator(Bedframe bedframe) {
        this.bedframe = bedframe;
    }

    /**
     * Registers the Geyser events required of this translator
     * Translators are expected to write to the resource pack here as well.
     */
    public abstract void register(EventBus<EventRegistrar> eventBus, Path packRoot);

    protected void markResourcesProvided() {
        this.providedResources = true;
    }

    public boolean hasProvidedResources() {
        return providedResources;
    }

    // Helper methods
    protected void forEachKey(JsonObject root, BiConsumer<String, JsonElement> function) {
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            function.accept(entry.getKey(), entry.getValue());
        }
    }

    protected void forEachLanguage(Path packDir, BiConsumer<FileWriter, TranslationAccess> function) {
        Path textsDir = packDir.resolve("texts");
        try {
            if (Files.notExists(textsDir)) Files.createDirectory(textsDir);
        } catch (IOException ignored) {}

        for (ServerLanguageDefinition language : ServerLanguageDefinition.getAllLanguages()) {
            String code = language.code();
            ServerLanguage serverLanguage = ServerTranslations.INSTANCE.getLanguage(code);

            File file = textsDir.resolve(code + ".lang").toFile();
            try (FileWriter writer = new FileWriter(file)) {
                function.accept(writer, serverLanguage.serverTranslations());
            } catch (IOException e) {
                bedframe.getLogger().error("Couldn't write language file");
            }
        }
    }

    protected void writeOrThrow(FileWriter writer, String content) {
        try {
            writer.write(content);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    protected static void writeJsonToFile(JsonObject object, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(object, writer);
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}
