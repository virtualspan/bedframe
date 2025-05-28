package lol.sylvie.bedframe.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourceHelper {
    public static InputStream getResource(String namespace, String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/" + namespace + "/" + path);
    }
    public static void copyResource(String namespace, String path, Path destination) {
        try {
            Files.copy(getResource(namespace, path), destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonObject readJsonResource(String namespace, String path, Gson gson) {
        try (InputStream stream = getResource(namespace, path)) {
            return gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String javaToBedrockTexture(String javaPath) {
        return javaPath.replaceFirst("block", "blocks");
    }
}
