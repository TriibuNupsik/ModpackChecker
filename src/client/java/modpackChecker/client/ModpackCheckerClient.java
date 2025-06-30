package modpackChecker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModpackCheckerClient implements ClientModInitializer {
    public static final String MOD_ID = "ModpackCheckerClient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Version file path - clients should have this file in their modpack
    public static final Path VERSION_FILE_PATH = FabricLoader.getInstance().getGameDir().resolve("modpack_version.txt");

    @Override
    public void onInitializeClient() {
        registerModpackCheckResponder();
    }

    private static void registerModpackCheckResponder() {
        // For now, we'll just log that the client mod is loaded
        // The actual version checking will be handled by the server during login
        LOGGER.info("ModpackChecker client mod loaded");
        LOGGER.info("Expected version file location: {}", VERSION_FILE_PATH);
        
        // Check if version file exists and log its contents
        try {
            if (Files.exists(VERSION_FILE_PATH)) {
                String version = Files.readString(VERSION_FILE_PATH).trim();
                LOGGER.info("Found version file with version: {}", version);
            } else {
                LOGGER.warn("Version file not found at: {}", VERSION_FILE_PATH);
                LOGGER.info("Please create a file named 'modpack_version.txt' in your .minecraft folder with the current modpack version");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read version file", e);
        }
        
        // Note: The actual version checking is handled server-side during login
        // Clients without this mod will be disconnected by the server
    }
}
