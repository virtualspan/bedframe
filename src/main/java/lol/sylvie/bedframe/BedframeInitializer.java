package lol.sylvie.bedframe;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class BedframeInitializer implements ModInitializer {
	public static final String MOD_ID = "bedframe";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Bedframe!");
		if (!CONFIG_DIR.toFile().exists() && !CONFIG_DIR.toFile().mkdir()) throw new RuntimeException("Unable to create config directory. Your file permissions are messed up.");
	}
}