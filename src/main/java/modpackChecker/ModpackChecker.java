/*
 * Copyright (c) 2025. Triibunupsik
 * SPDX-License-Identifier: Apache-2.0
 */

package modpackChecker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModpackChecker implements ModInitializer {
    public static final String MOD_ID = "ModpackChecker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MinecraftServer server;
    public static boolean isSingleplayer;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (server, resourceManager, success) -> {
                    ConfigManager.reload(); // I always reload the config for the mod because datapacks don't affect it
                }
        );
        LOGGER.info("ModpackChecker starting");

        // Initialize configuration
        ConfigManager.init(false);
        LOGGER.info("ModpackChecker started");
    }

    private void onServerStarting(MinecraftServer mcserver) {
        server = mcserver;
        isSingleplayer = server.isSingleplayer();
        if (isSingleplayer) {
            // don't register events
            LOGGER.info("Detected SinglePlayer environment, ModpackChecker disabled");
        } else {
            NetworkHandler.register();
            LOGGER.info("ModpackChecker network handlers registered");
        }
    }
}
