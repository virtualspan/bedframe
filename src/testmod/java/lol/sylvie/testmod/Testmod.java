package lol.sylvie.testmod;

import lol.sylvie.bedframe.api.Bedframe;
import lol.sylvie.testmod.block.ModBlocks;
import lol.sylvie.testmod.item.ModItems;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Testmod implements ModInitializer {
	public static final String MOD_ID = "bedframe-testmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Bedframe BEDFRAME = new Bedframe(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();


		LOGGER.info("Hello Fabric world!");
	}
}