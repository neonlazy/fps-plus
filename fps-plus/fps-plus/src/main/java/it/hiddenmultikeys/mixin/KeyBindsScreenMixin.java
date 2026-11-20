package it.hiddenmultikeys.mixin;

import it.hiddenmultikeys.ExtraBindingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aggancia il tasto K alla schermata Associazione tasti (nessun pulsante visibile). */
@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin extends Screen {
    @Shadow public KeyMapping selectedKey;

    protected KeyBindsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void mkb$openHiddenScreen(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        // Non intercettare K mentre si sta riassegnando un tasto o si scrive nella ricerca
        if (this.selectedKey != null) return;
        if (this.getFocused() instanceof EditBox) return;
        if (event.key() == GLFW.GLFW_KEY_K) {
            this.minecraft.setScreen(new ExtraBindingsScreen(this));
            cir.setReturnValue(true);
        }
    }
}
