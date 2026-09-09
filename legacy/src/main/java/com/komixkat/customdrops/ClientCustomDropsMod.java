package com.komixkat.customdrops;

import com.komixkat.customdrops.network.CustomDropsSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-only entrypoint. This class MUST NEVER call any loot-table-modifying code -
 * its only job is silently receiving CustomDropsSyncPayload and storing it for later
 * display (e.g. a ModMenu "Server Info" screen), never announcing it in chat and never
 * acting on it. The actual drop-modification logic in LootTableInjector /
 * EquipmentDropMixin only ever runs on the logical server side (dedicated server or
 * integrated singleplayer server), never here.
 */
public final class ClientCustomDropsMod implements ClientModInitializer {

    private static volatile String lastReceivedSummary = null;

    @Override
    public void onInitializeClient() {
        try {
            ClientPlayNetworking.registerGlobalReceiver(CustomDropsSyncPayload.TYPE,
                (payload, context) -> lastReceivedSummary = payload.summaryJson());
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not register client-side Custom Drops sync receiver; "
                + "server config visibility will be unavailable. This never affects actual gameplay - "
                + "the client never modifies loot tables itself regardless of whether this registration succeeds.", t);
        }
    }

    public static String lastReceivedSummary() {
        return lastReceivedSummary;
    }

    public static boolean isConnectedToCustomDropsServer() {
        return lastReceivedSummary != null;
    }
}
