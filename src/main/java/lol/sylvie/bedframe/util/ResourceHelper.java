package lol.sylvie.bedframe.util;

import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourceHelper {
    public static InputStream getResource(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    public static InputStream getResource(String namespace, String path) {
        return getResource("assets/" + namespace + "/" + path);
    }

    public static void copyResource(String namespace, String path, Path destination) {
        try {
            if (Files.notExists(destination))
                Files.copy(getResource(namespace, path), destination);
        } catch (IOException e) {
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
