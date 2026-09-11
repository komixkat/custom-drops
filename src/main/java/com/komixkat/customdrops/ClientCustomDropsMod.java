package com.komixkat.customdrops;

import com.komixkat.customdrops.client.gui.screen.RootScreen;
import com.komixkat.customdrops.client.network.ConfigSyncHandler;
import com.komixkat.customdrops.client.network.ServerConfigCache;
import com.komixkat.customdrops.network.OpenGuiPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientCustomDropsMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ConfigSyncHandler.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerConfigCache.reset());

        try {
            ClientPlayNetworking.registerGlobalReceiver(OpenGuiPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                    context.client().gui.setScreen(new RootScreen(null))));
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not register open-gui handler", t);
        }
    }

    public static boolean isConnectedToCustomDropsServer() {
        return ServerConfigCache.connected();
    }
}