package modpackChecker.client;

import modpackChecker.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModpackCheckerClient implements ClientModInitializer {
    public static final String MOD_ID = "ModpackCheckerClient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        registerModpackCheckResponder();
    }

    private static void registerModpackCheckResponder() {
        // Initialize client configuration
        ConfigManager.init();
        
        LOGGER.info("ModpackChecker client mod loaded");
        LOGGER.info("Client version: {}", ConfigManager.clientVersion);
        
        // Note: The actual version checking is handled server-side during login
        // Clients without this mod will be disconnected by the server
        // The client version is read from the configuration file
    }
}
