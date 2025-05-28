package lol.sylvie.bedframe.api.geyser.translator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lol.sylvie.bedframe.BedframeInitializer;
import lol.sylvie.bedframe.api.Bedframe;
import lol.sylvie.bedframe.api.BedframeBlock;
import lol.sylvie.bedframe.api.geyser.Translator;
import lol.sylvie.bedframe.util.ResourceHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.TransformationComponent;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import xyz.nucleoid.packettweaker.PacketContext;
import xyz.nucleoid.server.translations.api.language.TranslationAccess;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static lol.sylvie.bedframe.util.BedframeConstants.GSON;

public class BlockTranslator extends Translator {
    // Maps parent models to a map containing the translations between Java sides and Bedrock sides
    private static final Map<String, List<Pair<String, String>>> parentFaceMap = Map.of(
            "minecraft:block/cube_all", List.of(
                    new Pair<>("all", "*")
            ),
            "minecraft:block/cube_bottom_top", List.of(
                    new Pair<>("side", "*"),
                    new Pair<>("top", "up"),
                    new Pair<>("bottom", "down"),
                    new Pair<>("side", "north"),
                    new Pair<>("side", "south"),
                    new Pair<>("side", "east"),
                    new Pair<>("side", "west")
            )
            // TODO: other block types

    );

    public BlockTranslator(Bedframe bedframe) {
        super(bedframe);
    }

    private void forEachBlock(BiConsumer<Identifier, BedframeBlock> function) {
        for (Map.Entry<Identifier, BedframeBlock> entry : this.bedframe.getBlocks().entrySet()) {
            function.accept(entry.getKey(), entry.getValue());
        }
    }

    private void populateProperties(CustomBlockData.Builder builder, Collection<Property<?>> properties) {
        for (Property<?> property : properties) {
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
        }
    }

    private String nonPrefixedBlockState(BlockState state, Identifier identifier) {
        String stateString = state.toString();
        return stateString.substring(identifier.toString().length() + 1, stateString.length() - 1);
    }

    // Referenced https://github.com/GeyserMC/Hydraulic/blob/master/shared/src/main/java/org/geysermc/hydraulic/block/BlockPackModule.java#L54
    public void handle(GeyserDefineCustomBlocksEvent event, Path packRoot, TranslationAccess translations, FileWriter writer) {
        forEachBlock((identifier, block) -> {
            Block realBlock = block.getBlock();
            NonVanillaCustomBlockData.Builder builder = NonVanillaCustomBlockData.builder()
                    .name(identifier.getPath())
                    .namespace(identifier.getNamespace())
                    .includedInCreativeInventory(true);
            CustomBlockComponents.Builder components = CustomBlockComponents.builder();

            // Properties
            populateProperties(builder, realBlock.getStateManager().getProperties());

            // Models
            HashMap<String, Pair<Integer, Integer>> rotationData = new HashMap<>();
            HashMap<String, ModelData> models = new HashMap<>();
            JsonObject variants = ResourceHelper.readJsonResource(identifier.getNamespace(), "blockstates/" + identifier.getPath() + ".json", GSON)
                    .getAsJsonObject("variants");
            forEachKey(variants, (key, element) -> {
                JsonObject object = element.getAsJsonObject();

                JsonElement potentialX = object.get("x");
                int x = potentialX == null ? 0 : potentialX.getAsInt();

                JsonElement potentialY = object.get("y");
                int y = potentialY == null ? 0 : potentialY.getAsInt();

                String modelPath = object.get("model").getAsString();
                JsonObject model = ResourceHelper.readJsonResource(identifier.getNamespace(), modelPath + ".json", GSON);

                rotationData.put(key, new Pair<>(x, y));
                models.put(key, ModelData.fromJson(model));
            });

            NonVanillaCustomBlockData data = builder.build();
            // Permutations
            List<CustomBlockPermutation> permutations = new ArrayList<>();
            for (BlockState state : realBlock.getStateManager().getStates()) {
                String stateKey = nonPrefixedBlockState(state, identifier);
                CustomBlockComponents.Builder stateComponents = CustomBlockComponents.builder();

                // Rotated variants
                Pair<Integer, Integer> rotation = rotationData.getOrDefault(stateKey, new Pair<>(0, 0));
                TransformationComponent rotationComponent = new TransformationComponent((360 - rotation.getLeft()) % 360, (360 - rotation.getRight()) % 360, 0);
                stateComponents.transformation(rotationComponent);

                // Geometry
                ModelData modelData = models.get(stateKey);
                GeometryComponent geometryComponent = GeometryComponent.builder().identifier("minecraft:geometry.full_block").build();
                String renderMethod = "opaque";
                switch (modelData.parent()) {
                    case "minecraft:block/cross": {
                        geometryComponent = GeometryComponent.builder().identifier("minecraft:geometry.cross").build();
                        renderMethod = "alpha_test_single_sided";
                        break;
                    }
                }
                stateComponents.geometry(geometryComponent);

                // Textures
                List<Pair<String, String>> faceMap = parentFaceMap.getOrDefault(modelData.parent(), parentFaceMap.get("minecraft:block/cube_all"));
                stateComponents.materialInstance()

                // Block state overrides
                CustomBlockState.Builder stateBuilder = data.blockStateBuilder();
                for (Property<?> property : state.getProperties()) {
                    if (property instanceof IntProperty intProperty) {
                        stateBuilder.intProperty(property.getName(), state.get(intProperty));
                    } else if (property instanceof BooleanProperty booleanProperty) {
                        stateBuilder.booleanProperty(property.getName(), state.get(booleanProperty));
                    } else if (property instanceof EnumProperty<?> enumProperty) {
                        stateBuilder.stringProperty(enumProperty.getName(), state.get(enumProperty).name());
                    } else {
                        throw new IllegalArgumentException("Unknown property type: " + property.getClass().getName());
                    }
                }

                CustomBlockState customBlockState = stateBuilder.build();
                event.registerOverride(BlockArgumentParser.stringifyBlockState(block.getPolymerBlockState(state, PacketContext.get())), customBlockState);
            }


        });
    }

    @Override
    public void register(EventBus<EventRegistrar> eventBus, Path packRoot, TranslationAccess translations, FileWriter writer) {
        eventBus.subscribe(this, GeyserDefineCustomBlocksEvent.class, event -> {
            handle(event, packRoot, translations, writer);
        });
    }

    record ModelData(String parent, Map<String, String> textures) {
        public static ModelData fromJson(JsonObject object) {
            JsonObject texturesObject = object.getAsJsonObject("textures");
            HashMap<String, String> texturesMap = new HashMap<>();
            texturesObject.entrySet().forEach(e -> texturesMap.put(e.getKey(), e.getValue().getAsString()));
            return new ModelData(object.get("parent").getAsString(), texturesMap);
        }
    }
}
