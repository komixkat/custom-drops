package com.komixkat.customdrops.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class UiSfx {

    private UiSfx() {}

    public static void click() {
        play(1.0f);
    }

    public static void confirm() {
        play(0.8f);
    }

    public static void accept() {
        play(0.9f);
    }

    private static void play(float pitch) {
        try {
            Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        } catch (Throwable t) {
            // audio unavailable
        }
    }
}