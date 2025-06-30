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
    private static final Path SERVER_CONFIG_PATH = CONFIG_DIR.resolve("modpack-checker-server.toml");
    private static final Path CLIENT_CONFIG_PATH = CONFIG_DIR.resolve("modpack-checker-client.toml");
    
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
            
            // Copy default config files if they don't exist
            copyDefaultConfigs();
            
            // Load configurations
            loadServerConfig();
            loadClientConfig();
            
            LOGGER.info("Configuration loaded successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to initialize configuration", e);
        }
    }
    
    public static void reload() {
        try {
            loadServerConfig();
            loadClientConfig();
            LOGGER.info("Configuration reloaded successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to reload configuration", e);
        }
    }
    
    private static void copyDefaultConfigs() throws IOException {
        // Copy server config
        if (!Files.exists(SERVER_CONFIG_PATH)) {
            String defaultServerConfig = """
                # Modpack Checker Server Configuration
                
                # Enable or disable modpack version checking
                enable = true
                
                # Expected modpack version that clients must have
                expected_version = "1.2.3"
                
                # Kick messages for different scenarios
                [messages]
                # Message shown when client doesn't have the mod installed
                no_mod = "❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft"
                
                # Message shown when client has wrong version (use {version} as placeholder)
                wrong_version = "❌ Please install modpack version {version}: https://triibu.tech/minecraft"
                
                # Message shown when there's a server configuration error
                server_error = "❌ Server configuration error. Please contact an administrator."
                """;
            Files.writeString(SERVER_CONFIG_PATH, defaultServerConfig);
            LOGGER.info("Created default server configuration file");
        }
        
        // Copy client config
        if (!Files.exists(CLIENT_CONFIG_PATH)) {
            String defaultClientConfig = """
                # Modpack Checker Client Configuration
                
                # Current modpack version - this should match the server's expected version
                version = "1.2.3"
                """;
            Files.writeString(CLIENT_CONFIG_PATH, defaultClientConfig);
            LOGGER.info("Created default client configuration file");
        }
    }
    
    private static void loadServerConfig() throws IOException {
        if (!Files.exists(SERVER_CONFIG_PATH)) {
            LOGGER.warn("Server configuration file not found, using defaults");
            return;
        }
        
        try (FileConfig config = FileConfig.of(SERVER_CONFIG_PATH, TomlFormat.instance())) {
            config.load();
            
            // Load server configuration values
            enable = config.getOrElse("enable", true);
            expectedVersion = config.getOrElse("expected_version", "1.2.3");
            
            // Load messages
            Config messages = config.get("messages");
            if (messages != null) {
                noModMessage = messages.getOrElse("no_mod", noModMessage);
                wrongVersionMessage = messages.getOrElse("wrong_version", wrongVersionMessage);
                serverErrorMessage = messages.getOrElse("server_error", serverErrorMessage);
            }
            
            LOGGER.info("Server configuration loaded - enable: {}, expected version: {}", enable, expectedVersion);
        } catch (Exception e) {
            LOGGER.error("Failed to load server configuration", e);
        }
    }
    
    private static void loadClientConfig() throws IOException {
        if (!Files.exists(CLIENT_CONFIG_PATH)) {
            LOGGER.warn("Client configuration file not found, using defaults");
            return;
        }
        
        try (FileConfig config = FileConfig.of(CLIENT_CONFIG_PATH, TomlFormat.instance())) {
            config.load();
            clientVersion = config.getOrElse("version", "1.2.3");
            LOGGER.info("Client configuration loaded - version: {}", clientVersion);
        } catch (Exception e) {
            LOGGER.error("Failed to load client configuration", e);
        }
    }
    
    public static String formatMessage(String message, String version) {
        return message.replace("{version}", version);
    }
} 