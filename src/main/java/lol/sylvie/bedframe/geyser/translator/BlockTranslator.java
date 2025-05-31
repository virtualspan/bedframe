package lol.sylvie.bedframe.geyser.translator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lol.sylvie.bedframe.BedframeInitializer;
import lol.sylvie.bedframe.api.Bedframe;
import lol.sylvie.bedframe.api.BedframeBlock;
import lol.sylvie.bedframe.geyser.Translator;
import lol.sylvie.bedframe.util.ResourceHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
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
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.component.TransformationComponent;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.util.CreativeCategory;
import xyz.nucleoid.packettweaker.PacketContext;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

import static lol.sylvie.bedframe.util.BedframeConstants.GSON;
import static lol.sylvie.bedframe.util.BedframeConstants.LOGGER;
import static lol.sylvie.bedframe.util.PathHelper.createDirectoryOrThrow;

public class BlockTranslator extends Translator {
    // Maps parent models to a map containing the translations between Java sides and Bedrock sides
    private static final Map<String, List<Pair<String, String>>> parentFaceMap = Map.of(
            "minecraft:block/cube_all", List.of(
                    new Pair<>("all", "*")
            ),
            "minecraft:block/cross", List.of(
                    new Pair<>("cross", "*")
            ),
            "minecraft:block/cube_bottom_top", List.of(
                    new Pair<>("side", "*"),
                    new Pair<>("top", "up"),
                    new Pair<>("bottom", "down"),
                    new Pair<>("side", "north"),
                    new Pair<>("side", "south"),
                    new Pair<>("side", "east"),
                    new Pair<>("side", "west")
            ),
            "minecraft:block/cube_column", List.of(
                    new Pair<>("side", "*"),
                    new Pair<>("end", "up"),
                    new Pair<>("end", "down"),
                    new Pair<>("side", "north"),
                    new Pair<>("side", "south"),
                    new Pair<>("side", "east"),
                    new Pair<>("side", "west")
            ),
            "minecraft:block/cube_column_horizontal", List.of(
                    new Pair<>("side", "*"),
                    new Pair<>("end", "up"),
                    new Pair<>("end", "down"),
                    new Pair<>("side", "north"),
                    new Pair<>("side", "south"),
                    new Pair<>("side", "east"),
                    new Pair<>("side", "west")
            )
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
                        builder.stringProperty(enumProperty.getName(), enumProperty.getValues().stream().map(Enum::name).map(String::toLowerCase).toList());
                default ->
                        LOGGER.error("Unknown property type: {}", property.getClass().getName());
            }
        }
    }

    private String nonPrefixedBlockState(BlockState state, Identifier identifier) {
        if (state.getProperties().isEmpty()) return "";
        String stateString = BlockArgumentParser.stringifyBlockState(state);
        return stateString.substring(identifier.toString().length() + 1, stateString.length() - 1);
    }

    // Referenced https://github.com/GeyserMC/Hydraulic/blob/master/shared/src/main/java/org/geysermc/hydraulic/block/BlockPackModule.java#L54
    public void handle(GeyserDefineCustomBlocksEvent event, Path packRoot) {
        Path textureDir = createDirectoryOrThrow(packRoot.resolve("textures"));
        createDirectoryOrThrow(textureDir.resolve("blocks"));

        JsonObject terrainTextureObject = new JsonObject();
        terrainTextureObject.addProperty("resource_pack_name", bedframe.getModId());
        terrainTextureObject.addProperty("texture_name", "atlas.terrain");

        JsonObject textureDataObject = new JsonObject();

        forEachBlock((identifier, block) -> {
            Block realBlock = block.getBlock();
            // Block names
            addTranslationKey("tile." + identifier.toString() + ".name", realBlock.getTranslationKey());

            NonVanillaCustomBlockData.Builder builder = NonVanillaCustomBlockData.builder()
                    .name(identifier.getPath())
                    .namespace(identifier.getNamespace())
                    .creativeCategory(CreativeCategory.CONSTRUCTION)
                    .includedInCreativeInventory(true);

            // Properties
            populateProperties(builder, realBlock.getStateManager().getProperties());

            // Parsing models
            HashMap<String, Pair<Integer, Integer>> rotationData = new HashMap<>();
            HashMap<String, ModelData> models = new HashMap<>();
            JsonObject variants = ResourceHelper.readJsonResource(identifier.getNamespace(), "blockstates/" + identifier.getPath() + ".json")
                    .getAsJsonObject("variants");
            forEachKey(variants, (key, element) -> {
                JsonObject object = element.getAsJsonObject();

                JsonElement potentialX = object.get("x");
                int x = potentialX == null ? 0 : potentialX.getAsInt();

                JsonElement potentialY = object.get("y");
                int y = potentialY == null ? 0 : potentialY.getAsInt();

                String modelPath = object.get("model").getAsString();
                JsonObject model = ResourceHelper.readJsonResource(identifier.getNamespace(), "models/" + Identifier.of(modelPath).getPath() + ".json");

                rotationData.put(key, new Pair<>(x, y));
                models.put(key, ModelData.fromJson(model));
            });

            // Block states/permutations
            List<CustomBlockPermutation> permutations = new ArrayList<>();
            for (BlockState state : realBlock.getStateManager().getStates()) {
                String stateKey = nonPrefixedBlockState(state, identifier);
                CustomBlockComponents.Builder stateComponentBuilder = CustomBlockComponents.builder();

                // Rotation
                Pair<Integer, Integer> rotation = rotationData.getOrDefault(stateKey, new Pair<>(0, 0));
                TransformationComponent rotationComponent = new TransformationComponent((360 - rotation.getLeft()) % 360, (360 - rotation.getRight()) % 360, 0);
                stateComponentBuilder.transformation(rotationComponent);

                // Geometry
                // TODO: More geometry types
                ModelData modelData = models.get(stateKey);
                boolean cross = modelData.parent().equals("minecraft:block/cross");
                String geometryIdentifier = cross ?  "minecraft:geometry.cross" : "minecraft:geometry.full_block";
                String renderMethod = cross ? "alpha_test_single_sided" : "opaque";

                GeometryComponent geometryComponent = GeometryComponent.builder().identifier(geometryIdentifier).build();
                stateComponentBuilder.geometry(geometryComponent);

                // Textures
                List<Pair<String, String>> faceMap = parentFaceMap.getOrDefault(modelData.parent(), parentFaceMap.get("minecraft:block/cube_all"));
                for (Pair<String, String> face : faceMap) {
                    String javaFaceName = face.getLeft();
                    String bedrockFaceName = face.getRight();
                    String texturePath = "textures/" + modelData.textures.getOrDefault(javaFaceName, "block/" + identifier.getPath());
                    String bedrockPath = ResourceHelper.javaToBedrockTexture(texturePath);

                    String bedrockTextureName = identifier.toString(); // Java face name since bedrock uses an * for all
                    if (!javaFaceName.equals("all"))
                        bedrockTextureName += "_" + javaFaceName;

                    JsonObject thisTexture = new JsonObject();
                    thisTexture.addProperty("textures", bedrockPath);
                    textureDataObject.add(bedrockTextureName, thisTexture);

                    stateComponentBuilder.materialInstance(bedrockFaceName, MaterialInstance.builder()
                            .renderMethod(renderMethod)
                            .texture(bedrockTextureName)
                            .faceDimming(true)
                            .ambientOcclusion(true)
                            .build());

                    ResourceHelper.copyResource(identifier.getNamespace(), texturePath + ".png", packRoot.resolve(bedrockPath + ".png"));
                }

                CustomBlockComponents stateComponents = stateComponentBuilder.build();
                if (state.getProperties().isEmpty()) {
                    builder.components(stateComponents);
                    continue;
                }

                // Conditions
                // Essentially telling Bedrock what components to activate when
                List<String> conditions = new ArrayList<>();
                for (Property<?> property : state.getProperties()) {
                    String propertyValue = state.get(property).toString();
                    if (property instanceof EnumProperty<?>) {
                        propertyValue = "'" + propertyValue.toLowerCase() + "'";
                    }
                    conditions.add("query.block_property('%name%') == %value%"
                            .replace("%name%", property.getName())
                            .replace("%value%", propertyValue));
                }
                String stateCondition = String.join(" && ", conditions);
                permutations.add(new CustomBlockPermutation(stateComponents, stateCondition));
            }
            builder.permutations(permutations);

            NonVanillaCustomBlockData data = builder.build();
            event.register(data);

            // Registering the block states
            for (BlockState state : realBlock.getStateManager().getStates()) {
                CustomBlockState.Builder stateBuilder = data.blockStateBuilder();

                for (Property<?> property : state.getProperties()) {
                    switch (property) {
                        case IntProperty intProperty ->
                                stateBuilder.intProperty(property.getName(), state.get(intProperty));
                        case BooleanProperty booleanProperty ->
                                stateBuilder.booleanProperty(property.getName(), state.get(booleanProperty));
                        case EnumProperty<?> enumProperty ->
                                stateBuilder.stringProperty(enumProperty.getName(), state.get(enumProperty).toString().toLowerCase());
                        default ->
                                throw new IllegalArgumentException("Unknown property type: " + property.getClass().getName());
                    }
                }

                CustomBlockState customBlockState = stateBuilder.build();
                event.registerOverride(BlockArgumentParser.stringifyBlockState(block.getPolymerBlockState(state, PacketContext.get())), customBlockState);
            }

            //Item blockItem = realBlock.asItem();
            //if (blockItem != Items.AIR) {
            //    String itemId = Registries.ITEM.getEntry(blockItem).getIdAsString();
            //    System.out.println(itemId);
            //    event.registerItemOverride(itemId, data);
            //}
        });

        terrainTextureObject.add("texture_data", textureDataObject);
        writeJsonToFile(terrainTextureObject, textureDir.resolve("terrain_texture.json").toFile());
        markResourcesProvided();
    }

    @Override
    public void register(EventBus<EventRegistrar> eventBus, Path packRoot) {
        eventBus.subscribe(this, GeyserDefineCustomBlocksEvent.class, event -> {
            handle(event, packRoot);
        });
    }

    record ModelData(String parent, Map<String, String> textures) {
        public static ModelData fromJson(JsonObject object) {
            JsonObject texturesObject = object.getAsJsonObject("textures");
            HashMap<String, String> texturesMap = new HashMap<>();
            texturesObject.entrySet().forEach(e -> {
                String texture = e.getValue().getAsString();
                if (texture.contains(":")) texture = Identifier.of(texture).getPath();
                texturesMap.put(e.getKey(), texture);
            });
            return new ModelData(object.get("parent").getAsString(), texturesMap);
        }
    }
}
