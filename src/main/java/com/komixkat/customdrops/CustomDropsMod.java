package com.komixkat.customdrops;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.komixkat.customdrops.config.ConfigLoader;
import com.komixkat.customdrops.config.ConfigProfiles;
import com.komixkat.customdrops.config.CustomDropsConfig;
import com.komixkat.customdrops.loot.LootTableInjector;
import com.komixkat.customdrops.network.OpenGuiPayload;
import com.komixkat.customdrops.network.SyncPayload;
import com.komixkat.customdrops.preset.PresetLoader;
import com.komixkat.customdrops.registry.VanillaLootTableRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

        ConfigProfiles.ensureGlobal();
        config = ConfigLoader.load(ConfigProfiles.activeDir());
        PresetLoader.applyActivePreset(config);
        warnOnUnknownLootTableIds(config);

        injector = new LootTableInjector(() -> config);
        injector.register();

        SyncPayload.register();
        OpenGuiPayload.register();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            SyncPayload.sendTo(handler.getPlayer(), config));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("customdrops")
                .then(Commands.literal("gui")
                    .requires(source -> source.isPlayer())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ServerPlayNetworking.send(player, new OpenGuiPayload());
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("reload")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        reloadConfig(server);
                        server.reloadResources(server.getPackRepository().getSelectedIds())
                            .thenRun(() -> {
                                context.getSource().sendSuccess(
                                    () -> Component.literal("Custom Drops config and loot tables reloaded."), true);
                                SyncPayload.broadcast(server, config);
                            })
                            .exceptionally(throwable -> {
                                LOGGER.error("Failed to reload resources after config change", throwable);
                                context.getSource().sendFailure(Component.literal(
                                    "Custom Drops config reloaded, but the resource reload failed; see server log."));
                                return null;
                            });
                        return Command.SINGLE_SUCCESS;
                    })));
        });
    }

    private void onServerStarted(MinecraftServer server) {
        config = ConfigLoader.loadWithWorldOverride(ConfigProfiles.activeDir(), worldConfigDir(server));
        PresetLoader.applyActivePreset(config);
        warnOnUnknownLootTableIds(config);
        LOGGER.info("Custom Drops applied world config for '{}': {} mob entries, {} block entries, {} chest entries, {} fishing entries, {} equipment overrides",
            server.getWorldData().getLevelName(), config.mobDrops().size(), config.blockDrops().size(),
            config.chestLoot().size(), config.fishingLoot().size(), config.equipmentOverrides().size());
    }

    private void onServerStopping(MinecraftServer server) {
        config = ConfigLoader.load(ConfigProfiles.activeDir());
        PresetLoader.applyActivePreset(config);
    }

    public static void reloadConfig(MinecraftServer server) {
        config = ConfigLoader.loadWithWorldOverride(ConfigProfiles.activeDir(), worldConfigDir(server));
        PresetLoader.applyActivePreset(config);
        warnOnUnknownLootTableIds(config);
        injector.reinject();
        LOGGER.info("Custom Drops config reloaded, {} mob entries, {} block entries, {} chest entries, {} fishing entries, {} equipment overrides",
            config.mobDrops().size(), config.blockDrops().size(), config.chestLoot().size(),
            config.fishingLoot().size(), config.equipmentOverrides().size());
    }

    public static java.nio.file.Path globalConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    }

    public static void saveGlobalConfig() {
        ConfigLoader.save(ConfigProfiles.activeDir(), config);
        CustomDropsMod.LOGGER.info("Custom Drops config saved to disk (profile \"{}\").", ConfigProfiles.active());
    }

    public static void reloadGlobalConfig() {
        reloadFromProfile();
    }

    public static void reloadFromProfile() {
        config = ConfigLoader.load(ConfigProfiles.activeDir());
        PresetLoader.applyActivePreset(config);
        warnOnUnknownLootTableIds(config);
        injector.reinject();
        LOGGER.info("Custom Drops active profile switched to \"{}\": {} mob, {} block, {} chest, {} fishing, {} equipment",
            ConfigProfiles.active(), config.mobDrops().size(), config.blockDrops().size(),
            config.chestLoot().size(), config.fishingLoot().size(), config.equipmentOverrides().size());
    }

    public static void reloadForRunningWorld() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            reloadGlobalConfig();
            return;
        }
        reloadConfig(server);
        server.reloadResources(server.getPackRepository().getSelectedIds())
            .thenRun(() -> SyncPayload.broadcast(server, config))
            .exceptionally(throwable -> {
                CustomDropsMod.LOGGER.error("Failed to reload resources after config save", throwable);
                return null;
            });
    }

    public static Path worldConfigDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID);
    }

    public static void linkActiveProfileToWorld(MinecraftServer server) {
        if (server == null) return;
        Path worldDir = worldConfigDir(server);
        Path srcDir = ConfigProfiles.activeDir();
        try {
            Files.createDirectories(worldDir);
        } catch (IOException e) {
            LOGGER.error("Could not create per-world config directory {}", worldDir, e);
            return;
        }
        for (String file : List.of("meta.json", "mob_drops.json", "block_drops.json",
            "chest_loot.json", "fishing_loot.json", "equipment_overrides.json")) {
            Path from = srcDir.resolve(file);
            if (!Files.exists(from)) continue;
            try {
                Files.copy(from, worldDir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.warn("Could not copy {} into world config dir", from, e);
            }
        }
    }

    public static void unlinkWorldConfig(MinecraftServer server) {
        if (server == null) return;
        Path worldDir = worldConfigDir(server);
        for (String file : List.of("meta.json", "mob_drops.json", "block_drops.json",
            "chest_loot.json", "fishing_loot.json", "equipment_overrides.json")) {
            try {
                Files.deleteIfExists(worldDir.resolve(file));
            } catch (IOException e) {
                LOGGER.warn("Could not delete world config file {}", worldDir.resolve(file), e);
            }
        }
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