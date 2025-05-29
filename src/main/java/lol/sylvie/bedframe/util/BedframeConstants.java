package lol.sylvie.bedframe.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lol.sylvie.bedframe.geyser.GeyserHandler;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BedframeConstants {
    // To save file space it's technically better to disable pretty printing
    public static final Gson GSON = FabricLoader.getInstance().isDevelopmentEnvironment() ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
    public static final String MOD_ID = "bedframe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean isGeyserLoaded = false;

    static {
        try {
            Class.forName("org.geysermc.api.Geyser");
            LOGGER.info("Geyser detected!");

            isGeyserLoaded = true;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Geyser is not loaded.");
        }
    }
}
