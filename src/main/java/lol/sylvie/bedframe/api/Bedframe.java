package lol.sylvie.bedframe.api;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import lol.sylvie.bedframe.api.compat.geyser.GeyserHandlerOLD;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple implementation of a Bedframe block
 * Use the {@link BedframeBlock} interface if you need more control over block states (ex. a directional block)
 */
public class Bedframe {
    private final String modId;
    public HashMap<Identifier, BedframeBlock> blocks = new HashMap<>();
    public ArrayList<Item> items = new ArrayList<>();

    private final Logger logger;

    public Bedframe(String modId) {
        this.modId = modId;
        this.logger = LoggerFactory.getLogger(String.format("Bedframe (%s)", modId));

        if (!PolymerResourcePackUtils.addModAssets(modId)) throw new IllegalStateException("Mod ID " + modId + " is invalid!");

        try {
            Class.forName("org.geysermc.api.Geyser");
            logger.info("Geyser detected! Registering Geyser hooks!");

            GeyserHandlerOLD hooks = new GeyserHandlerOLD(this);
            hooks.registerGeyserHooks();
        } catch (ClassNotFoundException e) {
            logger.info("Geyser is not loaded.");
        }
    }

    public void register(BedframeBlock block, Identifier identifier) {
        blocks.put(identifier, block);
    }

    public void register(Item item) {
        items.add(item);
    }

    public String getModId() {
        return modId;
    }

    public HashMap<Identifier, BedframeBlock> getBlocks() {
        return blocks;
    }

    public ArrayList<Item> getItems() {
        return items;
    }
}
