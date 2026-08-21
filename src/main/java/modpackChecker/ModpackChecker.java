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
import net.minecraft.util.Formatting;
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
                ConfigManager.returnReason result = ConfigManager.loadConfig();

                switch (result) {
                    case SUCCESS:
                        context.getSource().sendFeedback(
                                () -> Text.literal("Modpack Checker configuration reloaded."), true);
                    break;
                    case UNKNOWN:
                        context.getSource().sendError(
                                Text.literal("Modpack Checker configuration not reloaded: \n Check the server log for details."));
                    break;
                    case BROKEN_CONFIG:
                        context.getSource().sendError(
                                Text.literal("Modpack Checker configuration not reloaded: \n Config file is broken. Check the server log for details."));
                    break;
                    case MISSING_CONFIG:
                        context.getSource().sendFeedback(
                                () -> Text.literal("Modpack Checker configuration regenerated:").formatted(Formatting.YELLOW)
                                        .append("\n ")
                                        .append(Text.literal("Config file was missing. Check the server log for details.").formatted(Formatting.RED)),
                                true);
                    break;
                }
                return 0;
            })
        );
    }
}
