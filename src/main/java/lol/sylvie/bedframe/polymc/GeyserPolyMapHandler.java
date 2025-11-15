package lol.sylvie.bedframe.polymc;

import io.github.theepicblock.polymc.api.misc.PolyMapProvider;
import io.github.theepicblock.polymc.impl.NOPPolyMap;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Returns NOPPolyMap for Bedrock connections registered at INIT.
 * Provisional NOP is only applied for Geyser/Bedrock connections (thread/addr heuristics).
 */
public final class GeyserPolyMapHandler {
    private GeyserPolyMapHandler() {}

    public static void register(MinecraftServer server) {
        PolyMapProvider.EVENT.register((ClientConnection connection) -> {
            // Direct Bedrock connections tracked by GeyserTracker
            if (GeyserTracker.isBedrockConnection(connection)) {
                return NOPPolyMap.INSTANCE;
            }

            // Heuristic detection for provisional Geyser connections
            String thread = Thread.currentThread().getName();
            SocketAddress addr = connection.getAddress();

            boolean isGeyserThread = thread.contains("Geyser");
            boolean isGeyserAddr = false;
            if (addr instanceof InetSocketAddress inet) {
                isGeyserAddr = (inet.getPort() == 0) ||
                        (inet.getAddress() != null && inet.getAddress().isLoopbackAddress() && inet.getPort() == 0);
            }

            if (isGeyserThread || isGeyserAddr) {
                if (!GeyserTracker.isProvisional(connection)) {
                    GeyserTracker.markProvisional(connection);
                    return NOPPolyMap.INSTANCE;
                }
            }

            return null;
        });
    }
}
