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
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        LOGGER.info("ModpackChecker starting");

        // Initialize configuration
        ConfigManager.init();
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

    private void onServerStarted(MinecraftServer mcserver) {
        // Register reload listener
        server.getCommandManager().getDispatcher().register(
            net.minecraft.server.command.CommandManager.literal("reload")
                .executes(context -> {
                    // Reload our configuration when vanilla reload is called
                    ConfigManager.reload();
                    context.getSource().sendFeedback(() -> 
                        net.minecraft.text.Text.literal("ModpackChecker configuration reloaded"), false);
                    return 1;
                })
        );
    }
}
