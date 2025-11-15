package lol.sylvie.bedframe.polymc;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import lol.sylvie.bedframe.mixin.ServerCommonNetworkHandlerAccessor;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks Bedrock players via Geyser connections by UUID.
 */
public final class GeyserTracker {
    private GeyserTracker() {}

    private static final Map<ClientConnection, UUID> BEDROCK_CONN_TO_UUID = new ConcurrentHashMap<>();
    private static final Set<ClientConnection> PROVISIONAL_CONNECTIONS = ConcurrentHashMap.newKeySet();

    public static void register() {
        ServerPlayConnectionEvents.INIT.register((ServerPlayNetworkHandler handler, net.minecraft.server.MinecraftServer server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID uuid = player.getUuid();
            ClientConnection conn = ((ServerCommonNetworkHandlerAccessor)(Object)handler).getConnection();

            GeyserConnection connection = GeyserApi.api().connectionByUuid(uuid);
            if (connection != null) {
                BEDROCK_CONN_TO_UUID.put(conn, uuid);
            }
            PROVISIONAL_CONNECTIONS.remove(conn);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ClientConnection conn = ((ServerCommonNetworkHandlerAccessor)(Object)handler).getConnection();
            BEDROCK_CONN_TO_UUID.remove(conn);
            PROVISIONAL_CONNECTIONS.remove(conn);
        });
    }

    public static boolean isBedrockConnection(ClientConnection connection) {
        return BEDROCK_CONN_TO_UUID.containsKey(connection);
    }

    public static boolean isProvisional(ClientConnection connection) {
        return PROVISIONAL_CONNECTIONS.contains(connection);
    }

    public static void markProvisional(ClientConnection connection) {
        PROVISIONAL_CONNECTIONS.add(connection);
    }
}
