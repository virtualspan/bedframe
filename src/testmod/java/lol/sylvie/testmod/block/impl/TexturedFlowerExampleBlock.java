package lol.sylvie.testmod.block.impl;

import eu.pb4.polymer.blocks.api.BlockModelType;
import lol.sylvie.bedframe.api.impl.SimpleBedframeBlock;
import lol.sylvie.testmod.Testmod;
import net.minecraft.util.Identifier;

public class TexturedFlowerExampleBlock extends SimpleBedframeBlock {
    public TexturedFlowerExampleBlock(Settings settings) {
        super(settings, BlockModelType.PLANT_BLOCK, Identifier.of(Testmod.MOD_ID, "block/example_flower"));
    }
}
