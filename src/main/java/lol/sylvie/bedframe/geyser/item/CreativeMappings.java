package lol.sylvie.bedframe.geyser.item;

import net.minecraft.block.Block;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;

/**
 * Handles mapping blocks to creative inventory groups in Bedrock.
 */
public final class CreativeMappings {
    private CreativeMappings() {}

    /**
     * Assign a creative group for a block item.
     * Currently defaults all blocks to "itemGroup.blocks".
     *
     * @param block the block being registered
     * @param opts  the Bedrock options builder to modify
     */
    public static void setupBlock(Block block, CustomItemBedrockOptions.Builder opts) {
        // Default mapping: all blocks go into the blocks creative group
        opts.creativeGroup("itemGroup.blocks");
    }
}
