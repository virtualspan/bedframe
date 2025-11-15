package lol.sylvie.bedframe.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static lol.sylvie.bedframe.util.BedframeConstants.GSON;
import static lol.sylvie.bedframe.util.BedframeConstants.LOGGER;

public class BedframeBlacklist {
    // Hardcoded defaults: always excluded, cannot be removed by config
    private static final Set<String> HARDCODED_BLACKLIST = Set.of(
            "rocks" // "examplemod", "examplemod2"
    );

    /**
     * Returns the full blacklist: hardcoded defaults + user config entries.
     */
    public static Set<String> getFullBlacklist(Path configDir) {
        Set<String> full = new HashSet<>(HARDCODED_BLACKLIST);
        Path configPath = configDir.resolve("bedframe").resolve("bedframe-blacklist.json");

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray array = obj.getAsJsonArray("blacklist");
                if (array != null) {
                    for (var el : array) {
                        full.add(el.getAsString());
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Couldn't read blacklist config, using defaults", e);
            } catch (Exception e) {
                LOGGER.warn("Malformed blacklist config, using defaults", e);
            }
        } else {
            // Auto-generate a starter config if missing
            try {
                Files.createDirectories(configPath.getParent());

                JsonObject obj = new JsonObject();
                JsonArray array = new JsonArray();
                // Example entries players can edit/remove
                array.add("examplemodid");
                array.add("examplemodid2");
                obj.add("blacklist", array);

                try (Writer writer = Files.newBufferedWriter(configPath)) {
                    GSON.toJson(obj, writer);
                }
                LOGGER.info("Created default bedframe-blacklist.json at {}", configPath);
            } catch (IOException e) {
                LOGGER.warn("Couldn't create default blacklist config", e);
            }
        }

        return full;
    }
}
