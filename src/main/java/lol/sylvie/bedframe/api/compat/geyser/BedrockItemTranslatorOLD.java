package lol.sylvie.bedframe.api.compat.geyser;

import lol.sylvie.bedframe.BedframeInitializer;
import lol.sylvie.bedframe.api.Bedframe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;

public class BedrockItemTranslatorOLD {
    public static void register(Bedframe bedframe, GeyserDefineCustomItemsEvent event) {
        BedframeInitializer.LOGGER.warn("Item declaration is not implemented");
    }
}
