package com.komixkat.customdrops.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.komixkat.customdrops.CustomDropsMod;
import com.komixkat.customdrops.config.CustomDropsConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

public record SyncPayload(String configJson, boolean canEdit) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().create();

    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CustomDropsMod.MOD_ID, "sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC =
        StreamCodec.composite(
            GzipStringCodec.INSTANCE, SyncPayload::configJson,
            ByteBufCodecs.BOOL, SyncPayload::canEdit,
            SyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        try {
            PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
            PayloadTypeRegistry.serverboundPlay().register(ConfigEditPayload.TYPE, ConfigEditPayload.CODEC);
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not register Custom Drops payloads.", t);
        }
    }

    public static void sendTo(ServerPlayer player, CustomDropsConfig config) {
        if (player == null) return;
        try {
            ServerPlayNetworking.send(player, fromConfig(config, hasOpPermission(player)));
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.debug("Could not send sync payload to player", t);
        }
    }

    public static void broadcast(MinecraftServer server, CustomDropsConfig config) {
        server.getPlayerList().getPlayers().forEach(player -> {
            try {
                ServerPlayNetworking.send(player, fromConfig(config, hasOpPermission(player)));
            } catch (Throwable t) {
                CustomDropsMod.LOGGER.debug("Could not broadcast sync payload", t);
            }
        });
    }

    public static void rebroadcast(MinecraftServer server) {
        broadcast(server, CustomDropsMod.config());
    }

    private static SyncPayload fromConfig(CustomDropsConfig config, boolean canEdit) {
        return new SyncPayload(GSON.toJson(config), canEdit);
    }

    public static boolean hasOpPermission(ServerPlayer player) {
        if (player == null) return false;
        var perms = player.permissions();
        if (perms instanceof LevelBasedPermissionSet lbs) {
            return lbs.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
        }
        return false;
    }

    public record ConfigEditPayload(String configJson) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ConfigEditPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CustomDropsMod.MOD_ID, "config_edit"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigEditPayload> CODEC =
            GzipStringCodec.INSTANCE.map(ConfigEditPayload::new, ConfigEditPayload::configJson).cast();

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}