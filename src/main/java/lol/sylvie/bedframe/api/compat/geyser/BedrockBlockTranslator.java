package lol.sylvie.bedframe.api.compat.geyser;

import lol.sylvie.bedframe.api.Bedframe;
import lol.sylvie.bedframe.api.BedframeBlock;
import lol.sylvie.bedframe.api.SimpleBedframeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.util.Identifier;
import org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.util.CreativeCategory;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Map;

public class BedrockBlockTranslator {
    public static void register(Bedframe bedframe, GeyserDefineCustomBlocksEvent event) {
        Map<Identifier, BedframeBlock> blocks = bedframe.getBlocks();
        for (Identifier key : blocks.keySet()) {
            BedframeBlock block = blocks.get(key);
            BlockState defaultState = block.getPolymerBlockState(block.getBlock().getDefaultState(), PacketContext.get());

            NonVanillaCustomBlockData.Builder builder = NonVanillaCustomBlockData.builder()
                    .name(key.getPath())
                    .namespace(key.getNamespace())
                    .includedInCreativeInventory(true);

            CustomBlockComponents components = CustomBlockComponents.builder()
                    .geometry(GeometryComponent.builder().identifier("minecraft:geometry.full_block").build())
                    .materialInstance("*", MaterialInstance.builder().renderMethod("opaque").texture(key.toString()).build())
                    .build();

            builder.components(components);

            /*for (Property<?> property : block.getStateManager().getProperties()) {
                switch (property) {
                    case IntProperty intProperty ->
                            builder.intProperty(property.getName(), List.copyOf(intProperty.getValues()));
                    case BooleanProperty booleanProperty ->
                            builder.booleanProperty(property.getName());
                    case EnumProperty<?> enumProperty ->
                            builder.stringProperty(enumProperty.getName(), enumProperty.getValues().stream().map(Enum::name).toList());
                    default ->
                            BedframeInitializer.LOGGER.error("Unknown property type: {}", property.getClass().getName());
                }
            }*/

            NonVanillaCustomBlockData data = builder.build();

            event.register(data);
            event.registerItemOverride("minecraft:grass_block[item_model=\"bedframe-testmod:example_block\"]", data);
            event.registerOverride(BlockArgumentParser.stringifyBlockState(defaultState), data.defaultBlockState());
        }
    }
}
