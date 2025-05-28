package lol.sylvie.bedframe.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

public class BedframeConstants {
    // To save file space it's technically better to disable pretty printing
    public static final Gson GSON = FabricLoader.getInstance().isDevelopmentEnvironment() ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
}
