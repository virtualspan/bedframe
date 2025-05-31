package lol.sylvie.bedframe.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.populator.CustomItemRegistryPopulator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 * Geyser forces the `minecraft:icon` component onto *all* items, even when it is a block
 * Custom Item API v2 will add options that make this mixin obsolete, but for now this should fix invisible block items
 */
@Mixin(CustomItemRegistryPopulator.class)
public class CustomItemRegistryPopulatorMixin {
    @Inject(method = "createComponentNbt(Lorg/geysermc/geyser/api/item/custom/NonVanillaCustomItemData;Ljava/lang/String;IZZI)Lorg/cloudburstmc/nbt/NbtMapBuilder;", at = @At(value = "INVOKE", target = "Lorg/geysermc/geyser/registry/populator/CustomItemRegistryPopulator;computeBlockItemProperties(Ljava/lang/String;Lorg/cloudburstmc/nbt/NbtMapBuilder;)V", shift = At.Shift.AFTER), remap = false)
    private static void bedframe$removeBlockIconComponent(NonVanillaCustomItemData customItemData, String customItemName, int customItemId, boolean isHat, boolean displayHandheld, int protocolVersion, CallbackInfoReturnable<NbtMapBuilder> cir, @Local(ordinal = 1) NbtMapBuilder itemProperties) {
        if (customItemData.icon().isEmpty()) itemProperties.remove("minecraft:icon");
    }

    @Inject(method = "computeBlockItemProperties", at=@At("HEAD"), cancellable = true, remap = false)
    private static void bedframe$addBlockPlacerComponent(String blockItem, NbtMapBuilder componentBuilder, CallbackInfo ci) {
        componentBuilder.putCompound("minecraft:block_placer", NbtMap.builder()
                .putString("block", blockItem)
                .putBoolean("canUseBlockAsIcon", true)
                .build());
        ci.cancel();
    }
}
