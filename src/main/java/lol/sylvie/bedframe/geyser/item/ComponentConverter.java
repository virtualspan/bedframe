package lol.sylvie.bedframe.geyser.item;

import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;

/**
 * Placeholder for mapping Java components to Bedrock v2 components.
 * Extend as your dependencies expose more component builders.
 */
public final class ComponentConverter {
    private ComponentConverter() {}

    public static void setGeyserComponents(ComponentMap javaComponents,
                                           Item item,
                                           NonVanillaCustomItemDefinition.Builder def,
                                           CustomItemBedrockOptions.Builder opts) {
        // Intentionally minimal for now.
    }
}
