package lol.sylvie.bedframe.geyser.translator;

import com.google.gson.JsonObject;
import lol.sylvie.bedframe.api.Bedframe;
import lol.sylvie.bedframe.api.BedframeItem;
import lol.sylvie.bedframe.geyser.Translator;
import lol.sylvie.bedframe.util.ResourceHelper;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import xyz.nucleoid.packettweaker.PacketContext;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;

import static lol.sylvie.bedframe.util.PathHelper.createDirectoryOrThrow;

public class ItemTranslator extends Translator {
    public ItemTranslator(Bedframe bedframe) {
        super(bedframe);
    }

    private void forEachItem(BiConsumer<Identifier, BedframeItem> function) {
        for (Map.Entry<Identifier, BedframeItem> entry : this.bedframe.getItems().entrySet()) {
            function.accept(entry.getKey(), entry.getValue());
        }
    }

    private void handle(GeyserDefineCustomItemsEvent event, Path packRoot) {
        Path textureDir = createDirectoryOrThrow(packRoot.resolve("textures"));
        createDirectoryOrThrow(textureDir.resolve("items"));

        JsonObject itemTextureObject = new JsonObject();
        itemTextureObject.addProperty("resource_pack_name", bedframe.getModId());
        itemTextureObject.addProperty("texture_name", "atlas.items");

        JsonObject textureDataObject = new JsonObject();

        forEachItem((identifier, item) -> {
            Item realItem = item.getItem();
            ItemStack realDefaultItemStack = realItem.getDefaultStack();

            // I know there is item.getPolymerItemModel but some developers (cough me cough) just override the itemstack model
            ItemStack itemStack = item.getPolymerItemStack(realItem.getDefaultStack(), TooltipType.BASIC, PacketContext.get());
            Identifier model = itemStack.get(DataComponentTypes.ITEM_MODEL);

            if (model == null || model.getNamespace().equals("minecraft")) return;

            CustomItemOptions.Builder itemOptions = CustomItemOptions.builder();

            NonVanillaCustomItemData.Builder itemBuilder = NonVanillaCustomItemData.builder()
                    .name(identifier.toString())
                    .identifier(identifier.toString())
                    .displayName(Text.translatable(realItem.getTranslationKey()).getString())
                    .allowOffhand(true);

            ComponentMap components = realDefaultItemStack.getComponents();

            // Food
            FoodComponent foodComponent = components.get(DataComponentTypes.FOOD);
            if (foodComponent != null) {
                itemBuilder.edible(true);
                itemBuilder.canAlwaysEat(foodComponent.canAlwaysEat());
            }

            // Glint
            itemBuilder.foil(components.getOrDefault(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false));

            // Damage
            itemOptions.unbreakable(realDefaultItemStack.isDamageable());

            // Bows
            itemBuilder.chargeable(realItem instanceof CrossbowItem || realItem instanceof BowItem);

            itemBuilder.customItemOptions(itemOptions.build());
            itemBuilder.javaId(Registries.ITEM.getRawIdOrThrow(realItem));
            JsonObject itemDescription = ResourceHelper.readJsonResource(model.getNamespace(), "items/" + model.getPath() + ".json");
            Identifier modelId = Identifier.of(itemDescription.get("model").getAsJsonObject().get("model").getAsString());

            JsonObject modelObject = ResourceHelper.readJsonResource(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
            String modelType = modelObject.get("parent").getAsString();

            if (modelType.equals("minecraft:item/generated")) {
                Identifier textureId = Identifier.of(modelObject.get("textures").getAsJsonObject().get("layer0").getAsString());

                String texturePath = "textures/" + textureId.getPath();
                String bedrockPath = ResourceHelper.javaToBedrockTexture(texturePath);
                String textureName = identifier.toString();

                JsonObject textureObject = new JsonObject();
                textureObject.addProperty("textures", bedrockPath);

                textureDataObject.add(textureName, textureObject);
                ResourceHelper.copyResource(textureId.getNamespace(), texturePath + ".png", packRoot.resolve(bedrockPath + ".png"));

                itemBuilder.icon(textureName);
            } else {
                itemBuilder.displayHandheld(true);
            }

            if (realItem instanceof BlockItem blockItem) {
                String blockId = Registries.BLOCK.getEntry(blockItem.getBlock()).getIdAsString();
                itemBuilder.block(blockId);
                //itemBuilder.translationString("tile." + blockId + ".name");
            } else {
                // Item names
                String bedrockKey = "item." + identifier + ".name";
                addTranslationKey(bedrockKey, realItem.getTranslationKey());
                itemBuilder.translationString(bedrockKey);
            }

            event.register(itemBuilder.build());
        });

        itemTextureObject.add("texture_data", textureDataObject);
        writeJsonToFile(itemTextureObject, textureDir.resolve("item_texture.json").toFile());
        markResourcesProvided();
    }

    @Override
    public void register(EventBus<EventRegistrar> eventBus, Path packRoot) {
        eventBus.subscribe(this, GeyserDefineCustomItemsEvent.class, event -> {
            handle(event, packRoot);
        });
    }
}
