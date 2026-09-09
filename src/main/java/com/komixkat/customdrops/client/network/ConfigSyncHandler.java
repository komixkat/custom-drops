package com.komixkat.customdrops.client.network;

import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.network.SyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ConfigSyncHandler {

    private ConfigSyncHandler() {}

    public static void register() {
        try {
            ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE,
                (payload, context) -> {
                    ServerConfigCache.applyJson(payload.configJson(), payload.canEdit());
                    CustomDropsMod.LOGGER.info("Received config sync from server (canEdit={})",
                        payload.canEdit());
                });
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not register config sync handler", t);
        }
    }
}