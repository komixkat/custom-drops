package com.komixkat.customdrops;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.komixkat.customdrops.config.ConfigLoader;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.loot.LootTableInjector;
import com.komixkat.customdrops.network.CustomDropsSyncPayload;
import com.komixkat.customdrops.preset.PresetLoader;
import com.komixkat.customdrops.registry.VanillaLootTableRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class CustomDropsMod implements ModInitializer {

    public static final String MOD_ID = "customdrops";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static CustomDropsConfig config;
    private static LootTableInjector injector;

    @Override
    public void onInitialize() {
        LOGGER.info("Custom Drops starting with {} known real vanilla loot table identifiers bundled.", VanillaLootTableRegistry.knownCount());

        config = ConfigLoader.load(globalConfigDir());
        PresetLoader.applyActivePreset(config);
        warnOnUnknownLootTableIds(config);

        injector = new LootTableInjector(() -> config);
        injector.register();

        CustomDropsSyncPayload.register();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            CustomDropsSyncPayload.sendTo(handler.getPlayer(), config));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("customdrops")
                .then(Commands.literal("reload")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        reloadConfig(server);
                        server.reloadResources(server.getPackRepository().getSelectedIds())
                            .thenRun(() -> {
                                context.getSource().sendSuccess(
                                    () -> Component.literal("Custom Drops config and loot tables reloaded."), true);
                                CustomDropsSyncPayload.broadcast(server, config);
                            })
                            .exceptionally(throwable -> {
                                LOGGER.error("Failed to reload resources after config change", throwable);
                                context.getSource().sendFailure(Component.literal(
                                    "Custom Drops config reloaded, but the resource reload failed; see server log."));
                                return null;
                            });
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("search")
                    .then(Commands.argument("query", StringArgumentType.greedyString())
                        .executes(context -> {
                            String query = StringArgumentType.getString(context, "query");
                            List<String> matches = VanillaLootTableRegistry.search(query);
                            if (matches.isEmpty()) {
                                context.getSource().sendFailure(Component.literal(
                                    "No known loot tables match '" + query + "'."));
                                return 0;
                            }
                            int shown = Math.min(matches.size(), 30);
                            StringBuilder body = new StringBuilder();
                            for (int i = 0; i < shown; i++) {
                                body.append(matches.get(i));
                                if (i < shown - 1) body.append('\n');
                            }
                            if (matches.size() > shown) {
                                body.append("\n...and ").append(matches.size() - shown)
                                    .append(" more (narrow your search).");
                            }
                            String message = "Found " + matches.size() + " match(es) for '" + query + "':\n" + body;
                            context.getSource().sendSuccess(() -> Component.literal(message), false);
                            return matches.size();
                        })))));
    }

    private void onServerStarted(MinecraftServer server) {
        config = ConfigLoader.loadWithWorldOverride(globalConfigDir(), worldConfigDir(server));
        PresetLoader.applyActivePreset(config);
        warnOnUnknownLootTableIds(config);
        LOGGER.info("Custom Drops applied world config for '{}': {} mob entries, {} block entries, {} chest entries, {} fishing entries, {} equipment overrides",
            server.getWorldData().getLevelName(), config.mobDrops().size(), config.blockDrops().size(),
            config.chestLoot().size(), config.fishingLoot().size(), config.equipmentOverrides().size());
    }

    private void onServerStopping(MinecraftServer server) {
        config = ConfigLoader.load(globalConfigDir());
        PresetLoader.applyActivePreset(config);
    }

    public static void reloadConfig(MinecraftServer server) {
        config = ConfigLoader.loadWithWorldOverride(globalConfigDir(), worldConfigDir(server));
        PresetLoader.applyActivePreset(config);
        warnOnUnknownLootTableIds(config);
        injector.reinject();
        LOGGER.info("Custom Drops config reloaded, {} mob entries, {} block entries, {} chest entries, {} fishing entries, {} equipment overrides",
            config.mobDrops().size(), config.blockDrops().size(), config.chestLoot().size(),
            config.fishingLoot().size(), config.equipmentOverrides().size());
    }

    private static java.nio.file.Path globalConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    }

    private static java.nio.file.Path worldConfigDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID);
    }

    private static void warnOnUnknownLootTableIds(CustomDropsConfig config) {
        config.chestLoot().forEach(entry -> checkAndWarn(entry.targetLootTableId()));
        config.fishingLoot().forEach(entry -> checkAndWarn(entry.targetLootTableId()));
    }

    private static void checkAndWarn(String targetId) {
        if (targetId.endsWith("*")) return;
        if (VanillaLootTableRegistry.isKnownOrUnverifiable(targetId)) return;

        String leaf = targetId.contains("/") ? targetId.substring(targetId.lastIndexOf('/') + 1) : targetId;
        List<String> suggestions = VanillaLootTableRegistry.search(leaf);
        if (!suggestions.isEmpty()) {
            LOGGER.warn("'{}' isn't a known vanilla loot table id. Did you mean one of: {}",
                targetId, String.join(", ", suggestions.stream().limit(5).toList()));
        } else {
            LOGGER.warn("'{}' isn't a known vanilla loot table id. This is fine if it's from a datapack or another mod.", targetId);
        }
    }

    public static CustomDropsConfig config() {
        return config;
    }
}
