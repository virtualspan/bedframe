package lol.sylvie.bedframe;

import static lol.sylvie.bedframe.util.BedframeConstants.LOGGER;
import static lol.sylvie.bedframe.util.BedframeConstants.MOD_ID;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import lol.sylvie.bedframe.util.ClassHelper;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.geysermc.geyser.api.GeyserApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nucleoid.packettweaker.PacketContext;

import java.nio.file.Path;
import java.util.function.Predicate;

public class BedframeInitializer implements ModInitializer {
	public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Bedframe!");
		if (!CONFIG_DIR.toFile().exists() && !CONFIG_DIR.toFile().mkdir())
			throw new RuntimeException("Unable to create config directory. Your file permissions are messed up.");
	}
}