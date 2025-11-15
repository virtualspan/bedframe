package lol.sylvie.bedframe.mixin;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.packettweaker.PacketContext;

/**
 * Mixin to override Polymer item disguises for Bedrock players via Geyser.
 */
@Mixin(PolymerItem.class)
public interface GlobalItemReplacementBypassMixin {
    @Inject(method = "getPolymerReplacement", at = @At("RETURN"), cancellable = true)
    private void bypassReplacementForBedrock(Item item,
                                             PacketContext context,
                                             CallbackInfoReturnable<Item> cir) {
        if (context instanceof PacketContext.NotNullWithPlayer ctx) {
            ServerPlayerEntity player = ctx.getPlayer();
            if (player != null) {
                GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUuid());
                if (connection != null) {
                    // Bedrock: bypass disguise, return original item
                    cir.setReturnValue(item);
                }
            }
        }
    }
}
