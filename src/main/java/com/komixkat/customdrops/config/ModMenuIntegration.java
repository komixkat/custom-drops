package com.komixkat.customdrops.config;

import com.komixkat.customdrops.client.gui.screen.RootScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return RootScreen::new;
    }
}