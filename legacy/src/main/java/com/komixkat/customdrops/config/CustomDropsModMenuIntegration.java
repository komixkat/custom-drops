package com.komixkat.customdrops.config;

import com.komixkat.customdrops.client.gui.CustomDropsRootScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class CustomDropsModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CustomDropsRootScreen::new;
    }
}
