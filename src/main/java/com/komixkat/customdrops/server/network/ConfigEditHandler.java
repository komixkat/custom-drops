package com.komixkat.customdrops.server.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.ConfigLoader;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.network.SyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public final class ConfigEditHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigEditHandler() {}

    public static void register() {
        try {
            ServerPlayNetworking.registerGlobalReceiver(SyncPayload.ConfigEditPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    if (!SyncPayload.hasOpPermission(player)) {
                        CustomDropsMod.LOGGER.warn("Player {} attempted config edit without permission", player.getName().getString());
                        return;
                    }

                    try {
                        CustomDropsConfig incoming = GSON.fromJson(payload.configJson(), CustomDropsConfig.class);
                        if (incoming == null) return;

                        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(CustomDropsMod.MOD_ID);
                        ConfigLoader.save(configDir, incoming);
                        CustomDropsMod.reloadConfig(context.server());
                        SyncPayload.broadcast(context.server(), incoming);
                        CustomDropsMod.LOGGER.info("Config edited by OP {}", player.getName().getString());
                    } catch (Exception e) {
                        CustomDropsMod.LOGGER.error("Failed to apply config edit from OP {}", player.getName().getString(), e);
                    }
                });
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not register config edit handler", t);
        }
    }
}
