package lol.sylvie.bedframe.geyser.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.BlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.Chargeable;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserDataComponent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Registers non-vanilla items with Geyser as custom Bedrock items.
 * Skips vanilla namespace items (minecraft:*).
 * Ensures each item has an icon (real or placeholder).
 */
public class ItemPackModule {
    private static final Set<Item> registeredItems = new HashSet<>();

    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        for (Item item : Registries.ITEM) {
            Identifier mcId = Registries.ITEM.getId(item);

            // Skip vanilla namespace items (minecraft:*)
            if ("minecraft".equals(mcId.getNamespace())) {
                continue;
            }

            // Export texture or fallback to placeholder
            Optional<String> iconKeyOpt = ItemTextureConverter.exportItemTexture(mcId);
            String iconKey = iconKeyOpt.orElseGet(() -> ItemTextureConverter.ensurePlaceholder(mcId.getPath()));

            // Skip if icon wasn't exported
            if (!ItemTextureConverter.getExportedIconKeys().contains(iconKey)) {
                continue;
            }

            // Build custom item definition
            NonVanillaCustomItemDefinition.Builder def = NonVanillaCustomItemDefinition.builder(
                            org.geysermc.geyser.api.util.Identifier.of(mcId.toString()),
                            org.geysermc.geyser.api.util.Identifier.of(mcId.toString()),
                            Registries.ITEM.getRawId(item)
                    )
                    // Use resolved localized name instead of raw translation key
                    .displayName(item.getName().getString());

            // Bedrock options builder
            CustomItemBedrockOptions.Builder opts = CustomItemBedrockOptions.builder()
                    .allowOffhand(true)
                    .creativeGroup("itemGroup." + mcId.getNamespace() + ".items")
                    .icon(iconKey);

            // Map Java components to Bedrock components
            ComponentConverter.setGeyserComponents(item.getDefaultStack().getComponents(), item, def, opts);

            // Special handling for bows and crossbows
            if (item instanceof BowItem) {
                def.component(GeyserDataComponent.CHARGEABLE,
                        Chargeable.builder().maxDrawDuration(1f).chargeOnDraw(false));
            } else if (item instanceof CrossbowItem) {
                def.component(GeyserDataComponent.CHARGEABLE,
                        Chargeable.builder().maxDrawDuration(0f).chargeOnDraw(true));
            }

            // Special handling for block items
            if (item instanceof BlockItem blockItem) {
                def.component(GeyserDataComponent.BLOCK_PLACER,
                        BlockPlacer.of(org.geysermc.geyser.api.util.Identifier.of(
                                Registries.BLOCK.getId(blockItem.getBlock()).toString()), true));
                CreativeMappings.setupBlock(blockItem.getBlock(), opts);
            }

            // Finalize and register
            def.bedrockOptions(opts);
            event.register(def.build());
            registeredItems.add(item);
        }
    }

    public static boolean isRegistered(Item item) {
        return registeredItems.contains(item);
    }
}
