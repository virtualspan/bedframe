package lol.sylvie.bedframe.mixin;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * Mixin to override Polymer block disguises for Bedrock players via Geyser.
 */
@Mixin(PolymerBlockUtils.class)
public class GlobalBlockUtilsBypassMixin {
    @Inject(method = "getBlockStateSafely", at = @At("RETURN"), cancellable = true)
    private static void bypassBlockSafely(PolymerBlock polymerBlock,
                                          BlockState blockState,
                                          int maxDistance,
                                          PacketContext context,
                                          CallbackInfoReturnable<BlockState> cir) {
        if (context instanceof PacketContext.NotNullWithPlayer ctx) {
            ServerPlayerEntity player = ctx.getPlayer();
            if (player != null) {
                GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUuid());
                if (connection != null) {
                    // Bedrock: bypass disguise, return original block state
                    cir.setReturnValue(blockState);
                }
            }
        }
    }
}
