package lol.sylvie.bedframe.mixin;

import eu.pb4.polymer.core.api.item.PolymerItem;
import lol.sylvie.bedframe.util.BedframeConstants;
import lol.sylvie.bedframe.util.GeyserHelper;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.packettweaker.PacketContext;

@Mixin(PolymerItem.class)
public interface PolymerItemMixin {
    // This mixin along with PolymerItemUtilsMixin tell Polymer to send Bedrock clients
    // the actual items rather than their Polymer representations
    @Inject(method = "getPolymerReplacement(Lnet/minecraft/item/Item;Lxyz/nucleoid/packettweaker/PacketContext;)Lnet/minecraft/item/Item;", at = @At("RETURN"), cancellable = true)
    private void bedframe$tellPolymerToAbstain(Item item, PacketContext context, CallbackInfoReturnable<Item> cir) {
        ServerPlayerEntity player = context.getPlayer();
        if (player != null && BedframeConstants.isGeyserLoaded && GeyserHelper.isBedrockPlayer(player))
            cir.setReturnValue(item);
    }
}
