package lol.sylvie.bedframe.api;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import lol.sylvie.bedframe.api.impl.SimpleBedframeBlock;
import net.minecraft.block.Block;

/**
 * Inteface used for creation of server side blocks with textures
 * The {@link SimpleBedframeBlock} class provides a barebones implementation
 */
public interface BedframeBlock extends PolymerTexturedBlock {
    String getTranslationKey();

    Block getBlock();
}
