package com.komixkat.customdrops.client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.network.SyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ServerConfigCache {

    private static final Gson GSON = new GsonBuilder().create();

    private static final CustomDropsConfig MIRROR = new CustomDropsConfig();
    private static volatile boolean canEdit = false;
    private static volatile boolean connected = false;
    private static volatile long receivedAt = 0;

    private ServerConfigCache() {}

    public static void reset() {
        synchronized (MIRROR) {
            CustomDropsMod.config().copyInto(MIRROR);
            MIRROR.setActivePreset("");
        }
        canEdit = false;
        connected = false;
        receivedAt = 0;
    }

    public static void applyIncoming(CustomDropsConfig incoming, boolean editable) {
        if (incoming == null) return;
        synchronized (MIRROR) {
            incoming.copyInto(MIRROR);
        }
        canEdit = editable;
        connected = true;
        receivedAt = System.currentTimeMillis();
    }

    public static void applyJson(String json, boolean editable) {
        try {
            CustomDropsConfig incoming = json == null || json.isBlank() ? null : GSON.fromJson(json, CustomDropsConfig.class);
            applyIncoming(incoming, editable);
        } catch (Exception e) {
            CustomDropsMod.LOGGER.error("Failed to parse server config, keeping previous view", e);
        }
    }

    public static void mutateToggles(Runnable change) {
        if (!canEdit || !connected) return;
        synchronized (MIRROR) {
            change.run();
        }
    }

    public static void sendToServer() {
        if (!canEdit || !connected) return;
        SyncPayload.ConfigEditPayload payload = new SyncPayload.ConfigEditPayload(GSON.toJson(MIRROR));
        try {
            ClientPlayNetworking.send(payload);
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not send config edit to server", t);
        }
    }

    public static CustomDropsConfig config() {
        return MIRROR;
    }

    public static boolean canEdit() {
        return canEdit;
    }

    public static boolean connected() {
        return connected;
    }

    public static long receivedAt() {
        return receivedAt;
    }
}