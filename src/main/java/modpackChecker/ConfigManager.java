package modpackChecker;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModpackChecker-Config");
    
    // Use standard server config folder
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getGameDir().resolve("config");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("modpack-checker.toml");
    
    // Server configuration
    public static boolean enable = true;
    public static String expectedVersion = "1.2.3";
    public static String noModMessage = "❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft";
    public static String wrongVersionMessage = "❌ Please install modpack version {version}: https://triibu.tech/minecraft";
    public static String serverErrorMessage = "❌ Server configuration error. Please contact an administrator.";
    
    // Client configuration
    public static String clientVersion = "1.2.3";
    
    public static void init() {
        try {
            // Create config directory if it doesn't exist
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            
            // Copy default config file if it doesn't exist
            copyDefaultConfig();
            
            // Load configuration
            loadConfig();
            
            LOGGER.info("Configuration loaded successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to initialize configuration", e);
        }
    }
    
    public static void reload() {
        try {
            loadConfig();
            LOGGER.info("Configuration reloaded successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to reload configuration", e);
        }
    }
    
    private static void copyDefaultConfig() throws IOException {
        if (!Files.exists(CONFIG_PATH)) {
            String defaultConfig = """
                # Modpack Checker Configuration
                # This file contains both server and client configuration
                
                # Server Configuration
                [server]
                # Enable or disable modpack version checking
                enable = true
                
                # Expected modpack version that clients must have
                expected_version = "1.2.3"
                
                # Kick messages for different scenarios
                [server.messages]
                # Message shown when client doesn't have the mod installed
                no_mod = "❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft"
                
                # Message shown when client has wrong version (use {version} as placeholder)
                wrong_version = "❌ Please install modpack version {version}: https://triibu.tech/minecraft"
                
                # Message shown when there's a server configuration error
                server_error = "❌ Server configuration error. Please contact an administrator."
                
                # Client Configuration
                [client]
                # Current modpack version - this should match the server's expected version
                version = "1.2.3"
                """;
            Files.writeString(CONFIG_PATH, defaultConfig);
            LOGGER.info("Created default configuration file");
        }
    }
    
    private static void loadConfig() throws IOException {
        if (!Files.exists(CONFIG_PATH)) {
            LOGGER.warn("Configuration file not found, using defaults");
            return;
        }
        
        try (FileConfig config = FileConfig.of(CONFIG_PATH, TomlFormat.instance())) {
            config.load();
            
            // Load server configuration
            Config serverConfig = config.get("server");
            if (serverConfig != null) {
                enable = serverConfig.getOrElse("enable", true);
                expectedVersion = serverConfig.getOrElse("expected_version", "1.2.3");
                
                // Load server messages
                Config messages = serverConfig.get("messages");
                if (messages != null) {
                    noModMessage = messages.getOrElse("no_mod", noModMessage);
                    wrongVersionMessage = messages.getOrElse("wrong_version", wrongVersionMessage);
                    serverErrorMessage = messages.getOrElse("server_error", serverErrorMessage);
                }
            }
            
            // Load client configuration
            Config clientConfig = config.get("client");
            if (clientConfig != null) {
                clientVersion = clientConfig.getOrElse("version", "1.2.3");
            }
            
            LOGGER.info("Configuration loaded - server enable: {}, expected version: {}, client version: {}", 
                       enable, expectedVersion, clientVersion);
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration", e);
        }
    }
    
    public static String formatMessage(String message, String version) {
        return message.replace("{version}", version);
    }
} 