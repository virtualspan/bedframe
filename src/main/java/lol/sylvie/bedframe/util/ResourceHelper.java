package lol.sylvie.bedframe.util;

import com.google.gson.JsonObject;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceHelper {
    public static ResourcePackBuilder PACK_BUILDER = null;
    public static ZipFile VANILLA_PACK = null;

    public static InputStream getResource(String path) {
        if (PACK_BUILDER != null) {
            byte[] data = PACK_BUILDER.getData(path);
            if (data != null) return new ByteArrayInputStream(data);
        }

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (stream != null) return stream;

        try {
            ZipEntry entry = VANILLA_PACK.getEntry(path);
            return VANILLA_PACK.getInputStream(entry);
        } catch (IOException e) {
            BedframeConstants.LOGGER.error("Couldn't find resource {}", path);
            throw new RuntimeException(e);
        }
    }

    public static String getResourcePath(String namespace, String path) {
        return "assets/" + namespace + "/" + path;
    }

    public static InputStream getResource(String namespace, String path) {
        return getResource(getResourcePath(namespace, path));
    }

    public static void copyResource(String namespace, String path, Path destination) {
        try {
            if (Files.notExists(destination)) {
                destination.toFile().getParentFile().mkdirs(); // Filament, i'm lazy :P
                Files.copy(getResource(namespace, path), destination);
            }

        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Couldn't copy resource " + Identifier.of(namespace, path), e);
        }
    }

    public static JsonObject readJsonResource(String namespace, String path) {
        try (InputStream stream = getResource(namespace, path)) {
            return BedframeConstants.GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load resource " + Identifier.of(namespace, path), e);
        }
    }

    public static String javaToBedrockTexture(String javaPath) {
        return javaPath.replaceFirst("block", "blocks").replaceFirst("item", "items");
    }
}
