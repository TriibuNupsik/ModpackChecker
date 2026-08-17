/*
 * Copyright (c) 2026. Triibunupsik
 * SPDX-License-Identifier: Apache-2.0
 */

package modpackChecker;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModpackChecker implements ModInitializer {
    public static final String MOD_ID = "ModpackChecker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigManager.init();
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        CommandRegistrationCallback.EVENT.register(this::registerCommands);

        LOGGER.info("Started successfully!");
    }

    private void onServerStarting(MinecraftServer server) {
        NetworkHandler.register();
    }

    private void registerCommands(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(CommandManager.literal("modpackchecker-reload")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(context -> {
                int result = ConfigManager.loadConfig();

                if(result == 1) {
                    context.getSource().sendFeedback(
                            () -> Text.literal("Modpack Checker configuration reloaded."), true);
                } else {
                    context.getSource().sendError(
                            Text.literal("Modpack Checker configuration was not reloaded: \n Check the server log for details."));
                }

                return result;
            })
        );
    }
}
