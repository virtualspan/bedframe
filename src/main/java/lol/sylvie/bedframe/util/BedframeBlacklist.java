package lol.sylvie.bedframe.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static lol.sylvie.bedframe.util.BedframeConstants.GSON;
import static lol.sylvie.bedframe.util.BedframeConstants.LOGGER;

public class BedframeBlacklist {
    private static final Set<String> HARDCODED_BLACKLIST = Set.of(
            "rocks" //, "examplemod", "conflictingmod"
    );

    public static Set<String> getFullBlacklist(Path configDir) {
        Set<String> full = new HashSet<>(HARDCODED_BLACKLIST);
        Path configPath = configDir.resolve("bedframe_blacklist.json");

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            JsonArray array = obj.getAsJsonArray("blacklisted_mods");
            for (var el : array) {
                full.add(el.getAsString());
            }
        } catch (IOException | NullPointerException e) {
            LOGGER.warn("Couldn't load blacklist config, using defaults");
        }

        return full;
    }
}
