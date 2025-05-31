package lol.sylvie.bedframe.util;

import net.minecraft.server.network.ServerPlayerEntity;
import org.geysermc.geyser.api.GeyserApi;

public class GeyserHelper {
    public static boolean isBedrockPlayer(ServerPlayerEntity player) {
        return GeyserApi.api().isBedrockPlayer(player.getUuid());
    }
}
