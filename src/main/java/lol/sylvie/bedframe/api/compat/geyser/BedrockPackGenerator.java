package lol.sylvie.bedframe.api.compat.geyser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lol.sylvie.bedframe.api.Bedframe;
import lol.sylvie.bedframe.api.BedframeBlock;
import lol.sylvie.bedframe.api.SimpleBedframeBlock;
import lol.sylvie.bedframe.util.ResourceHelper;
import lol.sylvie.bedframe.util.ZipHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.util.Identifier;
import xyz.nucleoid.server.translations.api.language.ServerLanguageDefinition;
import xyz.nucleoid.server.translations.impl.ServerTranslations;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BedrockPackGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonArray getVersionArray(ModMetadata metadata) {
        // TODO: A regex would be more inclusive
        Version version = metadata.getVersion();
        List<Integer> friendly = Arrays.stream(version.getFriendlyString()
                .split("\\."))
                .map(x -> x.replaceAll("[^0-9]", ""))
                .map(Integer::valueOf)
                .toList();

        JsonArray array = new JsonArray(friendly.size());
        friendly.forEach(array::add);
        return array;
    }

    private static void writeJsonToFile(JsonObject object, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(object, writer);
        }
    }

    private static void writeManifestFile(Path directory, ModMetadata metadata) throws IOException {
        String versionIdentifier = metadata.getId() + "-" + metadata.getVersion().getFriendlyString();
        boolean shouldRandomize = FabricLoader.getInstance().isDevelopmentEnvironment();

        // Manifest
        File manifestFile = directory.resolve("manifest.json").toFile();
        JsonObject manifestObject = new JsonObject();
        manifestObject.addProperty("format_version", 2);
        JsonArray version = getVersionArray(metadata);
        // Header
        JsonObject header = new JsonObject();
        header.addProperty("description", metadata.getDescription());
        header.addProperty("name", metadata.getId());
        header.addProperty("uuid", (shouldRandomize ? UUID.randomUUID() : UUID.fromString(versionIdentifier)).toString());
        header.add("version", version);

        JsonArray engineVersion = new JsonArray();
        engineVersion.add(1); engineVersion.add(20); engineVersion.add(80);
        header.add("min_engine_version", engineVersion);

        manifestObject.add("header", header);

        // Modules
        JsonArray modules = new JsonArray();
        JsonObject module = new JsonObject();
        module.addProperty("description", metadata.getName() + " Resources");
        module.addProperty("type", "resources");
        module.addProperty("uuid", (shouldRandomize ? UUID.randomUUID() : UUID.fromString(versionIdentifier + "-resources")).toString());
        module.add("version", version);

        modules.add(module);
        manifestObject.add("modules", modules);

        writeJsonToFile(manifestObject, manifestFile);
    }

    private static void writeLanguageFiles(Bedframe bedframe, Path textsDirectory) throws IOException {
        for (ServerLanguageDefinition language : ServerTranslations.INSTANCE.getAllLanguages()) {
            String code = language.code();
            File file = textsDirectory.resolve(code + ".lang").toFile();
            try (FileWriter writer = new FileWriter(file)) {
                for (Identifier identifier : bedframe.blocks.keySet()) {
                    BedframeBlock block = bedframe.blocks.get(identifier);
                    String name = ServerTranslations.INSTANCE.getLanguage(code).serverTranslations().get(block.getTranslationKey());
                    writer.write("tile." + identifier.toString() + ".name=" + name);
                }
            }
        }
    }

    /*
    private static void writeBlockTextures(Bedframe bedframe, Path packTexturePath) throws IOException {
        Path packPath = packTexturePath.getParent();
        Files.createDirectory(packTexturePath.resolve("blocks"));

        // terrain_texture.json
        JsonObject terrainTextureObject = new JsonObject();
        terrainTextureObject.addProperty("resource_pack_name", bedframe.getModId());
        terrainTextureObject.addProperty("texture_name", "atlas.terrain");

        JsonObject textureDataObject = new JsonObject();
        for (Identifier identifier : bedframe.blocks.keySet()) {
            BedframeBlock block = bedframe.blocks.get(identifier);
            JsonObject blockStates = ResourceHelper.readJsonResource(identifier.getNamespace(), "models/block/" + identifier.getPath() + ".json", GSON)
                    .getAsJsonObject("variants");

            blockStates.entrySet().forEach((entry) -> {
                String blockStateIdentifier = entry.getKey();
                JsonObject state = entry.getValue().getAsJsonObject();

                // TODO: LOOK UP AND HANDLE EACH MODIFIER
            });

            switch (block.getModelType()) {
                case FULL_BLOCK -> {
                    JsonObject model = ResourceHelper.readJsonResource(identifier.getNamespace(), "models/block/" + identifier.getPath() + ".json", GSON);
                    JsonObject modelTextures = model.getAsJsonObject("textures");

                    if (modelTextures.has("all")) {
                        String allTexture = identifier.toString();
                        String javaPath = "textures/" + modelTextures.get("all").getAsString().split(":", 2)[1];
                        String bedrockPath = ResourceHelper.javaToBedrockTexture(javaPath);

                        // TERRAIN TEXTURE
                        JsonObject thisTexture = new JsonObject();
                        thisTexture.addProperty("textures", bedrockPath);
                        textureDataObject.add(allTexture, thisTexture);

                        ResourceHelper.copyResource(identifier.getNamespace(), javaPath + ".png", packPath.resolve(bedrockPath + ".png"));
                    }
                }
            }
        }

        terrainTextureObject.add("texture_data", textureDataObject);
        writeJsonToFile(terrainTextureObject, packTexturePath.resolve("terrain_texture.json").toFile());
    }*/

    public static void generate(Bedframe bedframe, Path path) throws IOException {
        Path tempDir = Files.createTempDirectory("bedframe");
        ModMetadata metadata = FabricLoader.getInstance().getModContainer(bedframe.getModId()).orElseThrow().getMetadata();
        writeManifestFile(tempDir, metadata);

        Path textsDir = Files.createDirectory(tempDir.resolve("texts"));
        writeLanguageFiles(bedframe, textsDir);

        //Path texturesDir = Files.createDirectory(tempDir.resolve("textures"));
        //writeBlockTextures(bedframe, texturesDir);

        ZipHelper.zipFolder(tempDir, path.toFile());
    }
}
