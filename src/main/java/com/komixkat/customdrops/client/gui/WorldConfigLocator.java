package com.komixkat.customdrops.client.gui;

import com.komixkat.customdrops.CustomDropsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

/**
 * Client-side-only helper for locating the currently active singleplayer/hosted world's
 * per-world config directory, confirmed via decompiled PauseScreen.java against real MC 26.2:
 * Minecraft.getSingleplayerServer() returns the IntegratedServer (or null if connected to a
 * remote server, or not in a world at all), and IntegratedServer inherits getWorldPath(...)
 * from MinecraftServer, the same method already used server-side in CustomDropsMod.
 */
public final class WorldConfigLocator {

    private WorldConfigLocator() {}

    public static Path activeWorldConfigDirOrNull() {
        IntegratedServer integratedServer = Minecraft.getInstance().getSingleplayerServer();
        if (integratedServer == null) return null;
        return integratedServer.getWorldPath(LevelResource.ROOT).resolve(CustomDropsMod.MOD_ID);
    }
}
