package lol.sylvie.bedframe.geyser.translator;

import com.google.gson.JsonObject;
import eu.pb4.polymer.blocks.api.BlockResourceCreator;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import lol.sylvie.bedframe.geyser.Translator;
import lol.sylvie.bedframe.geyser.model.JavaGeometryConverter;
import lol.sylvie.bedframe.mixin.BlockResourceCreatorAccessor;
import lol.sylvie.bedframe.mixin.PolymerBlockResourceUtilsAccessor;
import lol.sylvie.bedframe.util.ResourceHelper;
import net.kyori.adventure.key.Key;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.Registries;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData;
import org.geysermc.geyser.api.block.custom.component.*;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBoundingBox;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.geyser.util.SoundUtils;
import org.geysermc.pack.bedrock.resource.models.entity.ModelEntity;
import org.geysermc.pack.converter.converter.model.ModelStitcher;
import org.geysermc.pack.converter.util.VanillaPackProvider;
import org.joml.Vector3f;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;
import team.unnamed.creative.serialize.minecraft.model.ModelSerializer;
import xyz.nucleoid.packettweaker.PacketContext;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static lol.sylvie.bedframe.util.BedframeConstants.LOGGER;
import static lol.sylvie.bedframe.util.PathHelper.createDirectoryOrThrow;

public class BlockTranslator extends Translator {
    // Maps parent models to a map containing the translations between Java sides and Bedrock sides
    private static final Map<String, List<Pair<String, String>>> parentFaceMap = Map.of(
            "block/cube_all", List.of(
                    new Pair<>("all", "*")
            ),
            "block/cross", List.of(
                    new Pair<>("particle", "*"),
                    new Pair<>("cross", "*")
            ),
            "block/cube_bottom_top", List.of(
                    new Pair<>("side", "*"),
                    new Pair<>("top", "up"),
                    new Pair<>("bottom", "down"),
                    new Pair<>("side", "north"),
                    new Pair<>("side", "south"),
                    new Pair<>("side", "east"),
                    new Pair<>("side", "west")
            ),
            "block/cube_column", List.of(
                    new Pair<>("side", "*"),
                    new Pair<>("end", "up"),
                    new Pair<>("end", "down"),
                    new Pair<>("side", "north"),
                    new Pair<>("side", "south"),
                    new Pair<>("side", "east"),
                    new Pair<>("side", "west")
            ),
            "block/cube_column_horizontal", List.of(
                    new Pair<>("side", "*"),
                    new Pair<>("end", "up"),
                    new Pair<>("end", "down"),
                    new Pair<>("side", "north"),
                    new Pair<>("side", "south"),
                    new Pair<>("side", "east"),
                    new Pair<>("side", "west")
            ),
            "block/orientable", List.of(
                    new Pair<>("side", "*"),
                    new Pair<>("front", "north"),
                    new Pair<>("top", "up"),
                    new Pair<>("bottom", "down")
            )
    );

    private static final ArrayList<PolymerBlock> registeredBlocks = new ArrayList<>();
    private final HashMap<Identifier, PolymerTexturedBlock> blocks = new HashMap<>();

    public BlockTranslator() {
        Stream<Identifier> blockIds = Registries.BLOCK.getIds().stream();

        blockIds.forEach(identifier -> {
            Block block = Registries.BLOCK.get(identifier);
            if (block instanceof PolymerTexturedBlock texturedBlock) {
                blocks.put(identifier, texturedBlock);
            }
        });
    }

    private void forEachBlock(BiConsumer<Identifier, PolymerTexturedBlock> function) {
        for (Map.Entry<Identifier, PolymerTexturedBlock> entry : blocks.entrySet()) {
            try {
                function.accept(entry.getKey(), entry.getValue());
            } catch (RuntimeException e) {
                LOGGER.error("Couldn't load block {}", entry.getKey(), e);
            }
        }
    }

    private void populateProperties(CustomBlockData.Builder builder, Collection<Property<?>> properties) {
        for (Property<?> property : properties) {
            switch (property) {
                case IntProperty intProperty ->
                        builder.intProperty(property.getName(), List.copyOf(intProperty.getValues()));
                case BooleanProperty ignored ->
                        builder.booleanProperty(property.getName());
                case EnumProperty<?> enumProperty ->
                        builder.stringProperty(enumProperty.getName(), enumProperty.getValues().stream().map(Enum::name).map(String::toLowerCase).toList());
                default ->
                        LOGGER.error("Unknown property type: {}", property.getClass().getName());
            }
        }
    }

    // okay so this is very much hydraulic code
    // TODO: see if shape.getBoundingBox() can replace the code here
    private BoxComponent voxelShapeToBoxComponent(VoxelShape shape) {
        if (shape.isEmpty())
            return BoxComponent.emptyBox();

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (Box boundingBox : shape.getBoundingBoxes()) {
            double offsetX = boundingBox.getLengthX() * 0.5;
            double offsetY = boundingBox.getLengthY() * 0.5;
            double offsetZ = boundingBox.getLengthZ() * 0.5;

            Vec3d center = boundingBox.getCenter();

            minX = Math.min(minX, (float) (center.getX() - offsetX));
            minY = Math.min(minY, (float) (center.getY() - offsetY));
            minZ = Math.min(minZ, (float) (center.getZ() - offsetZ));

            maxX = Math.max(maxX, (float) (center.getX() + offsetX));
            maxY = Math.max(maxY, (float) (center.getY() + offsetY));
            maxZ = Math.max(maxZ, (float) (center.getZ() + offsetZ));
        }

        minX = MathUtils.clamp(minX, 0, 1);
        minY = MathUtils.clamp(minY, 0, 1);
        minZ = MathUtils.clamp(minZ, 0, 1);

        maxX = MathUtils.clamp(maxX, 0, 1);
        maxY = MathUtils.clamp(maxY, 0, 1);
        maxZ = MathUtils.clamp(maxZ, 0, 1);

        return new BoxComponent(
                16 * (1 - maxX) - 8,
                16 * minY,
                16 * minZ - 8,
                16 * (maxX - minX),
                16 * (maxY - minY),
                16 * (maxZ - minZ)
        );
    }

    private Model resolveModel(Identifier identifier) {
        // This is unstable (https://unnamed.team/docs/creative/latest/serialization/minecraft)
        try {
            String modelPath = identifier.getPath();
            if (!(modelPath.startsWith("item/") || modelPath.startsWith("block/"))) modelPath = "block/" + modelPath;
            JsonObject model = ResourceHelper.readJsonResource(identifier.getNamespace(), "models/" + modelPath + ".json");
            return ModelSerializer.INSTANCE.deserializeFromJson(model, Key.key(identifier.toString()));
        } catch (RuntimeException e) {
            LOGGER.warn("Couldn't resolve model {}", identifier);
            return null;
        }
    }

    public static boolean isRegisteredBlock(PolymerBlock block) {
        return registeredBlocks.contains(block);
    }

    // Referenced https://github.com/GeyserMC/Hydraulic/blob/master/shared/src/main/java/org/geysermc/hydraulic/block/BlockPackModule.java#L54
    public void handle(GeyserDefineCustomBlocksEvent event, Path packRoot) {
        Path textureDir = createDirectoryOrThrow(packRoot.resolve("textures"));
        createDirectoryOrThrow(textureDir.resolve("blocks"));

        Path modelsDir = createDirectoryOrThrow(packRoot.resolve("models"));
        Path blockModelsDir = createDirectoryOrThrow(modelsDir.resolve("blocks"));

        JsonObject terrainTextureObject = new JsonObject();
        terrainTextureObject.addProperty("resource_pack_name", "Bedframe");
        terrainTextureObject.addProperty("texture_name", "atlas.terrain");

        JsonObject blocksJson = new JsonObject();
        blocksJson.addProperty("format_version", "1.21.40");

        JsonObject soundsJson = new JsonObject();
        JsonObject blockSoundsObject = new JsonObject();
        JsonObject interactiveSoundsObject = new JsonObject();

        JsonObject interactiveSoundsWrapper = new JsonObject();
        JsonObject textureDataObject = new JsonObject();

        forEachBlock((identifier, block) -> {
            Block realBlock = Registries.BLOCK.get(identifier);
            // Block names
            addTranslationKey("block." + identifier.getNamespace() + "." + identifier.getPath(), realBlock.getTranslationKey());

            NonVanillaCustomBlockData.Builder builder = NonVanillaCustomBlockData.builder()
                    .name(identifier.getPath())
                    .namespace(identifier.getNamespace())
                    .creativeGroup("itemGroup." + identifier.getNamespace() + ".blocks")
                    .creativeCategory(CreativeCategory.CONSTRUCTION)
                    .includedInCreativeInventory(true);

            // Properties
            populateProperties(builder, realBlock.getStateManager().getProperties());

            // Block states/permutations
            List<CustomBlockPermutation> permutations = new ArrayList<>();
            for (BlockState state : realBlock.getStateManager().getStates()) {
                CustomBlockComponents.Builder stateComponentBuilder = CustomBlockComponents.builder();

                // Hardness
                float hardness = state.getHardness(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                stateComponentBuilder.destructibleByMining(hardness);

                // Obtain model data from polymers internal api
                BlockState polymerBlockState = block.getPolymerBlockState(state, PacketContext.get());
                BlockResourceCreator creator = PolymerBlockResourceUtilsAccessor.getCREATOR();
                PolymerBlockModel[] polymerBlockModels = ((BlockResourceCreatorAccessor) (Object) creator).getModels().get(polymerBlockState);

                if (polymerBlockModels == null || polymerBlockModels.length == 0) {
                    LOGGER.warn("No model specified for blockstate {}", state);
                    continue;
                }

                PolymerBlockModel modelEntry = polymerBlockModels[0]; // TODO: java selects one by weight, does bedrock support this?

                // Rotation
                TransformationComponent rotationComponent = new TransformationComponent((360 - modelEntry.x()) % 360, (360 - modelEntry.y()) % 360, 0);
                stateComponentBuilder.transformation(rotationComponent);

                // Geometry
                String renderMethod = state.isOpaque() ? "opaque" : "blend"; // TODO: Hydraulic also faces this problem; Figure out when to use alpha_test
                Identifier blockModelId = modelEntry.model();
                Model blockModel = resolveModel(blockModelId);
                if (blockModel == null) {
                    LOGGER.warn("Couldn't load model for blockstate {}", state);
                    continue;
                }

                // Textures
                HashMap<String, ModelTexture> materials = new HashMap<>();
                Key modelParentKey = blockModel.parent();
                if (modelParentKey != null && parentFaceMap.containsKey(modelParentKey.value())) {
                    // Vanilla parent
                    boolean cross = modelParentKey.toString().equals("minecraft:block/cross");
                    String geometryIdentifier = cross ?  "minecraft:geometry.cross" : "minecraft:geometry.full_block";
                    if (cross) renderMethod = "alpha_test_single_sided";

                    GeometryComponent geometryComponent = GeometryComponent.builder().identifier(geometryIdentifier).build();
                    stateComponentBuilder.geometry(geometryComponent);

                    ModelTextures textures = blockModel.textures();
                    Map<String, ModelTexture> textureMap = textures.variables();
                    List<Pair<String, String>> faceMap = parentFaceMap.get(modelParentKey.value());

                    for (Pair<String, String> face : faceMap) {
                        String javaFaceName = face.getLeft();
                        String bedrockFaceName = face.getRight();
                        if (!textureMap.containsKey(javaFaceName)) continue;
                        materials.put(bedrockFaceName, textureMap.get(javaFaceName));
                    }
                } else {
                    // Custom model
                    ModelStitcher.Provider provider = key -> resolveModel(Identifier.of(key.asString()));
                    blockModel = new ModelStitcher(provider, blockModel).stitch(); // This resolves parent models (?)

                    Pair<String, ModelEntity> nameAndModel = JavaGeometryConverter.convert(blockModel);
                    if (nameAndModel == null) {
                        LOGGER.error("Couldn't convert model for blockstate {}", state);
                        continue;
                    }
                    String geometryId = nameAndModel.getLeft();
                    writeJsonToFile(nameAndModel.getRight(), blockModelsDir.resolve(geometryId + ".geo.json").toFile());

                    for (Map.Entry<String, ModelTexture> entry : blockModel.textures().variables().entrySet()) {
                        String key = entry.getKey();
                        ModelTexture texture = entry.getValue();
                        materials.put(key, texture);
                    }

                    GeometryComponent geometryComponent = GeometryComponent.builder().identifier(geometryId).build();
                    stateComponentBuilder.geometry(geometryComponent);
                }

                if (materials.isEmpty()) {
                    LOGGER.error("Couldn't generate materials for blockstate {}", state);
                    continue;
                }

                // Particles
                ModelTextures textures = blockModel.textures();
                if (!materials.containsKey("*")) {
                    ModelTexture texture = textures.particle() == null ? materials.values().iterator().next() : textures.particle();
                    materials.put("*", texture);
                }

                for (Map.Entry<String, ModelTexture> entry : materials.entrySet()) {
                    ModelTexture texture = entry.getValue();

                    while (texture.key() == null) {
                        String reference = texture.reference();
                        if (reference == null || !materials.containsKey(reference)) {
                            break;
                        }

                        texture = materials.get(reference);
                    }

                    if (texture.key() == null) {
                        LOGGER.warn("Texture for block {} on side {} is missing", identifier, entry.getKey());
                        continue;
                    }

                    String textureName = texture.key().asString();
                    if (!textureDataObject.has(textureName)) {
                        Identifier textureIdentifier = Identifier.of(textureName);

                        String texturePath = "textures/" + textureIdentifier.getPath();
                        String bedrockPath = ResourceHelper.javaToBedrockTexture(texturePath);

                        JsonObject thisTexture = new JsonObject();
                        thisTexture.addProperty("textures", bedrockPath);
                        textureDataObject.add(textureName, thisTexture);

                        ResourceHelper.copyResource(textureIdentifier.getNamespace(), texturePath + ".png", packRoot.resolve(bedrockPath + ".png"));
                    }

                    stateComponentBuilder.materialInstance(entry.getKey(), MaterialInstance.builder()
                            .renderMethod(renderMethod)
                            .texture(textureName)
                            .faceDimming(true)
                            .ambientOcclusion(blockModel.ambientOcclusion())
                            .build());
                }

                // Collision
                VoxelShape collisionBox = state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                stateComponentBuilder.collisionBox(voxelShapeToBoxComponent(collisionBox));

                VoxelShape outlineBox = state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                stateComponentBuilder.selectionBox(voxelShapeToBoxComponent(outlineBox));

                stateComponentBuilder.lightEmission(state.getLuminance());

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

                    conditions.add("q.block_property('%name%') == %value%"
                            .replace("%name%", property.getName())
                            .replace("%value%", propertyValue));
                }

                String stateCondition = String.join(" && ", conditions);
                permutations.add(new CustomBlockPermutation(stateComponents, stateCondition));
            }
            builder.permutations(permutations);

            // Sounds
            // blocks.json
            String blockAsString = identifier.toString();
            JsonObject thisBlockObject = new JsonObject();
            thisBlockObject.addProperty("sound", blockAsString);
            blocksJson.add(blockAsString, thisBlockObject);

            // sounds.json
            BlockSoundGroup soundGroup = realBlock.getDefaultState().getSoundGroup();
            // base sounds (break, hit, place)
            JsonObject baseSoundObject = new JsonObject();
            baseSoundObject.addProperty("pitch", soundGroup.getPitch());
            baseSoundObject.addProperty("volume", soundGroup.getVolume());

            JsonObject soundEventsObject = new JsonObject();
            soundEventsObject.addProperty("break", SoundUtils.translatePlaySound(soundGroup.getBreakSound().id().toString()));
            soundEventsObject.addProperty("hit", SoundUtils.translatePlaySound(soundGroup.getHitSound().id().toString()));
            soundEventsObject.addProperty("place", SoundUtils.translatePlaySound(soundGroup.getPlaceSound().id().toString()));
            baseSoundObject.add("events", soundEventsObject);

            blockSoundsObject.add(blockAsString, baseSoundObject);
            // interactive sounds
            JsonObject interactiveSoundObject = new JsonObject();
            interactiveSoundObject.addProperty("pitch", soundGroup.getPitch());
            interactiveSoundObject.addProperty("volume", soundGroup.getVolume() * .4); // The multiplier is arbitrary, its just too loud by default :(

            JsonObject interactiveEventsObject = new JsonObject();
            interactiveEventsObject.addProperty("fall", SoundUtils.translatePlaySound(soundGroup.getFallSound().id().toString()));
            interactiveEventsObject.addProperty("jump", SoundUtils.translatePlaySound(soundGroup.getStepSound().id().toString()));
            interactiveEventsObject.addProperty("step", SoundUtils.translatePlaySound(soundGroup.getStepSound().id().toString()));
            interactiveEventsObject.addProperty("land", SoundUtils.translatePlaySound(soundGroup.getFallSound().id().toString()));
            interactiveSoundObject.add("events", interactiveEventsObject);
            interactiveSoundsObject.add(blockAsString, interactiveSoundObject);

            // Registration
            NonVanillaCustomBlockData data = builder.build();
            event.register(data);
            registeredBlocks.add(block);

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
                JavaBlockState.Builder javaBlockState = JavaBlockState.builder();
                javaBlockState.blockHardness(state.getHardness(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));

                VoxelShape shape = state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                if (shape.isEmpty()) {
                    javaBlockState.collision(new JavaBoundingBox[0]);
                } else {
                    Box box = shape.getBoundingBox();
                    javaBlockState.collision(new JavaBoundingBox[]{
                        new JavaBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)
                    });
                }

                javaBlockState.javaId(Block.getRawIdFromState(state));
                javaBlockState.identifier(BlockArgumentParser.stringifyBlockState(state));
                javaBlockState.waterlogged(state.get(Properties.WATERLOGGED, false));
                if (realBlock.asItem() != null) javaBlockState.pickItem(Registries.ITEM.getId(realBlock.asItem()).toString());
                javaBlockState.canBreakWithHand(state.isToolRequired());

                PistonBehavior pistonBehavior = state.getPistonBehavior();
                javaBlockState.pistonBehavior(pistonBehavior == PistonBehavior.IGNORE ? "NORMAL" : pistonBehavior.name());

                event.registerOverride(javaBlockState.build(), customBlockState);
            }
        });

        terrainTextureObject.add("texture_data", textureDataObject);
        soundsJson.add("block_sounds", blockSoundsObject);
        interactiveSoundsWrapper.add("block_sounds", interactiveSoundsObject);
        soundsJson.add("interactive_sounds", interactiveSoundsWrapper);
        writeJsonToFile(terrainTextureObject, textureDir.resolve("terrain_texture.json").toFile());
        writeJsonToFile(blocksJson, packRoot.resolve("blocks.json").toFile());
        writeJsonToFile(soundsJson, packRoot.resolve("sounds.json").toFile());
        markResourcesProvided();
    }

    @Override
    public void register(EventBus<EventRegistrar> eventBus, Path packRoot) {
        eventBus.subscribe(this, GeyserDefineCustomBlocksEvent.class, event -> handle(event, packRoot));
    }
}
