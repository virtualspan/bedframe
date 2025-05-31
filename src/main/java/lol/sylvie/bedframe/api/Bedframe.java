package lol.sylvie.bedframe.api;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import lol.sylvie.bedframe.geyser.GeyserHandler;
import lol.sylvie.bedframe.util.BedframeConstants;
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
    public HashMap<Identifier, BedframeItem> items = new HashMap<>();

    private final Logger logger;

    public Bedframe(String modId) {
        this.modId = modId;
        this.logger = LoggerFactory.getLogger(String.format("Bedframe (%s)", modId));

        if (!PolymerResourcePackUtils.addModAssets(modId)) throw new IllegalStateException("Mod ID " + modId + " is invalid!");

        if (BedframeConstants.isGeyserLoaded) {
            GeyserHandler hooks = new GeyserHandler(this);
            hooks.registerHooks();
        }
    }

    // TODO: I really would like to avoid having mods register altogether, and that should be possible?
    // If so, we can forego the entire `api` package, which would be nice :)
    public void register(BedframeBlock block, Identifier identifier) {
        blocks.put(identifier, block);
    }

    public void register(BedframeItem item, Identifier identifier) {
        items.put(identifier, item);
    }

    public String getModId() {
        return modId;
    }

    public HashMap<Identifier, BedframeBlock> getBlocks() {
        return blocks;
    }

    public HashMap<Identifier, BedframeItem> getItems() {
        return items;
    }

    public Logger getLogger() {
        return logger;
    }
}
