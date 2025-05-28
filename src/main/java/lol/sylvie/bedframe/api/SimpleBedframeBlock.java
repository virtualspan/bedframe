package lol.sylvie.bedframe.api;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * Simple implementation of a Bedframe block
 * Use the {@link BedframeBlock} interface if you need more control over block states (ex. a directional block)
 */
public class SimpleBedframeBlock extends Block implements BedframeBlock {
    protected final BlockModelType modelType;
    private final BlockState polymerBlockState;

    public SimpleBedframeBlock(Settings settings, BlockModelType modelType, Identifier model) {
        super(settings);
        this.modelType = modelType;
        this.polymerBlockState = PolymerBlockResourceUtils.requestBlock(modelType, PolymerBlockModel.of(model));
        if (this.polymerBlockState == null) throw new RuntimeException("There are too many Polymer textured blocks of type " + modelType.getClass().getName());
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return polymerBlockState;
    }

    public BlockModelType getModelType() {
        return modelType;
    }

    @Override
    public Block getBlock() {
        return this;
    }
}