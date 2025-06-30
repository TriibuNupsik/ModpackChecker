package modpackChecker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

// Stuff for commands
import static net.minecraft.server.command.CommandManager.*;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

public class ModpackChecker implements ModInitializer {
    public static final String MOD_ID = "ModpackChecker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MinecraftServer server;
    public static boolean isSingleplayer;
    public static boolean isModCheckEnabled = true;
    
    // Version file path
    public static final Path VERSION_FILE_PATH = FabricLoader.getInstance().getGameDir().resolve("expected_version.txt");

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        LOGGER.info("ModpackChecker starting");

        registerCommands();
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

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Command to turn mod checking off
            dispatcher.register(literal("modcheck_off")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        if (isModCheckEnabled) {
                            isModCheckEnabled = false;
                          context.getSource().sendFeedback(()->
                              Text.literal("ModCheck temporarily disabled!"), false);
                        } else {
                            context.getSource().sendFeedback(()->
                            Text.literal("ModCheck is already disabled!"), false);
                        }
                      return 1;
                    })
            );

            // Command to turn mod checking on
            dispatcher.register(literal("modcheck_on")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        if (!isModCheckEnabled) {
                            isModCheckEnabled = true;
                          context.getSource().sendFeedback(()->
                              Text.literal("ModCheck enabled!"), false);
                        } else {
                            context.getSource().sendFeedback(()->
                            Text.literal("ModCheck is already enabled!"), false);
                        }
                      return 1;
                    })
            );

            // Command to set expected version
            dispatcher.register(literal("modcheck_setversion")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(argument("version", string())
                    .executes(context -> {
                        String version = getString(context, "version");
                        try {
                            Files.writeString(VERSION_FILE_PATH, version);
                            context.getSource().sendFeedback(()->
                                Text.literal("Expected version set to: " + version), false);
                        } catch (IOException e) {
                            context.getSource().sendFeedback(()->
                                Text.literal("Failed to write version file: " + e.getMessage()), false);
                        }
                        return 1;
                    }))
            );

        });
    }
}
