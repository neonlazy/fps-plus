package it.hiddenmultikeys.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import it.hiddenmultikeys.ExtraBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {

    /** Pressione singola: incrementa il contatore anche per le azioni che hanno questo tasto come extra. */
    @Inject(method = "click", at = @At("HEAD"))
    private static void mkb$click(InputConstants.Key key, CallbackInfo ci) {
        for (KeyMapping km : ExtraBindings.bindingsFor(key)) {
            KeyMappingAccessor a = (KeyMappingAccessor) km;
            if (a.mkb$getKey().equals(key)) continue; // già gestito da vanilla
            a.mkb$setClickCount(a.mkb$getClickCount() + 1);
        }
    }

    /** Tasto premuto/rilasciato: aggiorna lo stato "tenuto premuto" delle azioni collegate. */
    @Inject(method = "set", at = @At("HEAD"))
    private static void mkb$set(InputConstants.Key key, boolean held, CallbackInfo ci) {
        if (held) ExtraBindings.held().add(key);
        else ExtraBindings.held().remove(key);

        for (KeyMapping km : ExtraBindings.bindingsFor(key)) {
            KeyMappingAccessor a = (KeyMappingAccessor) km;
            if (a.mkb$getKey().equals(key)) continue;
            km.setDown(held || ExtraBindings.anyDown(km));
        }
    }

    /** Dopo la chiusura di una schermata vanilla rilegge lo stato fisico dei tasti. */
    @Inject(method = "setAll", at = @At("TAIL"))
    private static void mkb$setAll(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ExtraBindings.held().clear();
        for (KeyMapping km : ExtraBindings.withExtras()) {
            for (InputConstants.Key k : ExtraBindings.view(km)) {
                if (k.getType() == InputConstants.Type.KEYSYM
                        && InputConstants.isKeyDown(mc.getWindow(), k.getValue())) {
                    ExtraBindings.held().add(k);
                    km.setDown(true);
                }
            }
        }
    }
}
