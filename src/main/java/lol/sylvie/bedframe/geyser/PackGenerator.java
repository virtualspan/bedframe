package lol.sylvie.bedframe.geyser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lol.sylvie.bedframe.geyser.item.ItemTextureConverter;
import lol.sylvie.bedframe.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static lol.sylvie.bedframe.util.BedframeConstants.GSON;
import static lol.sylvie.bedframe.util.BedframeConstants.METADATA;

public class PackGenerator {

    private static JsonArray getVersionArray() {
        // TODO: A regex would be more inclusive
        Version version = METADATA.getVersion();
        List<Integer> friendly = Arrays.stream(version.getFriendlyString().split("\\."))
                .map(x -> x.replaceAll("[^0-9]", ""))
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .toList();
        JsonArray array = new JsonArray(friendly.size());
        friendly.forEach(array::add);
        return array;
    }

    private static void writeJsonToFile(JsonElement object, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(object, writer);
        }
    }

    private static String getUuidString(String base) {
        return UUID.nameUUIDFromBytes(base.getBytes()).toString();
    }

    private static void writeManifestFile(Path directory) throws IOException {
        // TODO: Maybe generate this based on the mod list?
        // It seems like bedrock uses UUID to cache resource packs
        String versionIdentifier = METADATA.getId() + "-" + METADATA.getVersion().getFriendlyString();
        boolean shouldRandomize = FabricLoader.getInstance().isDevelopmentEnvironment();

        // Manifest
        File manifestFile = directory.resolve("manifest.json").toFile();
        JsonObject manifestObject = new JsonObject();
        manifestObject.addProperty("format_version", 2);
        JsonArray version = getVersionArray();
        // Header
        JsonObject header = new JsonObject();
        header.addProperty("description", METADATA.getDescription());
        header.addProperty("name", METADATA.getId());
        header.addProperty("uuid", shouldRandomize ? UUID.randomUUID().toString() : getUuidString(versionIdentifier));
        header.add("version", version);

        // Modules
        JsonArray engineVersion = new JsonArray();
        engineVersion.add(1); engineVersion.add(21); engineVersion.add(70);
        header.add("min_engine_version", engineVersion);

        manifestObject.add("header", header);

        JsonArray modules = new JsonArray();
        JsonObject module = new JsonObject();
        module.addProperty("description", METADATA.getName() + " Resources");
        module.addProperty("type", "resources");
        module.addProperty("uuid", shouldRandomize ? UUID.randomUUID().toString() : getUuidString(versionIdentifier + "-resources"));
        module.add("version", version);
        modules.add(module);

        manifestObject.add("modules", modules);
        writeJsonToFile(manifestObject, manifestFile);
    }

    private static void writeItemAtlas(Path packRoot, Set<String> iconKeys) throws IOException {
        Path texturesDir = packRoot.resolve("textures");
        PathHelper.createDirectoryOrThrow(texturesDir);

        if (iconKeys.isEmpty()) {
            BedframeConstants.LOGGER.error("No item icons exported; item_texture.json would be empty. Aborting atlas write.");
            // Still write an empty atlas to avoid file-missing issues, but log loudly
        }

        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", METADATA.getId());
        atlas.addProperty("texture_name", "atlas.items");

        JsonObject textureData = new JsonObject();
        for (String key : iconKeys) {
            JsonObject entry = new JsonObject();
            entry.addProperty("textures", "textures/items/" + key);
            textureData.add(key, entry);
        }
        atlas.add("texture_data", textureData);

        writeJsonToFile(atlas, texturesDir.resolve("item_texture.json").toFile());
        BedframeConstants.LOGGER.info("Wrote item atlas with {} entries", iconKeys.size());
    }

    public void generatePack(Path packPath, File outputFile, List<Translator> translators) throws IOException {
        writeManifestFile(packPath);

        // Ensure converter directories exist (idempotent)
        ItemTextureConverter.init(packPath);

        Path textsDir = packPath.resolve("texts");
        PathHelper.createDirectoryOrThrow(textsDir);

        // TODO: I'm not sure if translations are even necessary
        JsonArray languages = new JsonArray();

        ArrayList<net.minecraft.util.Pair<String, String>> allKeys = new ArrayList<>();
        translators.forEach(t -> allKeys.addAll(t.getTranslations()));

        TranslationHelper.LANGUAGES.forEach((code) -> {
            try (FileWriter writer = new FileWriter(textsDir.resolve(code + ".lang").toFile())) {
                for (net.minecraft.util.Pair<String, String> keyPair : allKeys) {
                    writer.write(keyPair.getLeft() + "=" +
                            net.minecraft.text.Text.translatable(keyPair.getRight()).getString() + "\n");
                }
            } catch (IOException e) {
                BedframeConstants.LOGGER.error("Couldn't write language file", e);
            }
            languages.add(code);
        });
        writeJsonToFile(languages, textsDir.resolve("languages.json").toFile());

        writeItemAtlas(packPath, ItemTextureConverter.getExportedIconKeys());

        Optional<String> icon = METADATA.getIconPath(512);
        if (icon.isPresent()) {
            Files.copy(ResourceHelper.getResource(icon.get()), packPath.resolve("pack_icon.png"));
        }

        ZipHelper.zipFolder(packPath, outputFile);
        BedframeConstants.LOGGER.info("Zipped resource pack to {}", outputFile.getAbsolutePath());
    }
}
