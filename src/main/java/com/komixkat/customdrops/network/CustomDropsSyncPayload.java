package com.komixkat.customdrops.network;

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

/**
 * Informational-only server-to-client sync. Never used to make the client alter any
 * loot table behavior itself; the client only stores and displays this for the player's
 * awareness of what a server is running. See VERSION_COMPATIBILITY.md and
 * IMPLEMENTATION_NOTES.md: this is the least-verified piece of the multiplayer phase,
 * since Fabric's payload/StreamCodec API shape has changed across versions and this
 * hasn't been checked against real decompiled MC 26.2 / fabric-api 0.158.0+26.2 source.
 */
public record CustomDropsSyncPayload(String summaryJson) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CustomDropsSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CustomDropsMod.MOD_ID, "sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomDropsSyncPayload> CODEC =
        ByteBufCodecs.STRING_UTF8.map(CustomDropsSyncPayload::new, CustomDropsSyncPayload::summaryJson).cast();

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        try {
            PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not register Custom Drops sync payload; "
                + "server-to-client config visibility will be unavailable this session. "
                + "This is a known unverified piece for MC 26.2 - see VERSION_COMPATIBILITY.md.", t);
        }
    }

    public static void sendTo(ServerPlayer player, CustomDropsConfig config) {
        if (player == null) return;
        try {
            ServerPlayNetworking.send(player, fromConfig(config));
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.debug("Could not send Custom Drops sync payload to a joining player", t);
        }
    }

    public static void broadcast(MinecraftServer server, CustomDropsConfig config) {
        CustomDropsSyncPayload payload = fromConfig(config);
        server.getPlayerList().getPlayers().forEach(player -> {
            try {
                ServerPlayNetworking.send(player, payload);
            } catch (Throwable t) {
                CustomDropsMod.LOGGER.debug("Could not send Custom Drops sync payload during broadcast", t);
            }
        });
    }

    private static CustomDropsSyncPayload fromConfig(CustomDropsConfig config) {
        String preset = config.activePreset() == null || config.activePreset().isBlank() ? "none" : config.activePreset();
        StringBuilder summary = new StringBuilder();
        summary.append("Active preset: ").append(preset).append('\n');
        appendCategoryLine(summary, "Mob drop rules", config.mobDrops().size(), config.mobDropsEnabled());
        appendCategoryLine(summary, "Block drop rules", config.blockDrops().size(), config.blockDropsEnabled());
        appendCategoryLine(summary, "Chest loot rules", config.chestLoot().size(), config.chestLootEnabled());
        appendCategoryLine(summary, "Fishing loot rules", config.fishingLoot().size(), config.fishingLootEnabled());
        appendCategoryLine(summary, "Equipment overrides", config.equipmentOverrides().size(), config.equipmentOverridesEnabled());
        return new CustomDropsSyncPayload(summary.toString());
    }

    private static void appendCategoryLine(StringBuilder summary, String label, int count, boolean enabled) {
        summary.append(label).append(": ").append(count)
            .append(enabled ? " (enabled)" : " (disabled)").append('\n');
    }
}
