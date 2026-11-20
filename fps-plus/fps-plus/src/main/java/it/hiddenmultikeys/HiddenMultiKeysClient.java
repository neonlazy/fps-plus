package it.hiddenmultikeys;

import net.fabricmc.api.ClientModInitializer;

public class HiddenMultiKeysClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ExtraBindings.load();
    }
}
