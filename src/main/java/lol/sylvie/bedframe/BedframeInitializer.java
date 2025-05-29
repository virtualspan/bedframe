package lol.sylvie.bedframe;

import static lol.sylvie.bedframe.util.BedframeConstants.LOGGER;
import static lol.sylvie.bedframe.util.BedframeConstants.MOD_ID;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class BedframeInitializer implements ModInitializer {
	public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Bedframe!");
		if (!CONFIG_DIR.toFile().exists() && !CONFIG_DIR.toFile().mkdir()) throw new RuntimeException("Unable to create config directory. Your file permissions are messed up.");
	}
}