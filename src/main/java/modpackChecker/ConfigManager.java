/*
 * Copyright (c) 2025. Triibunupsik
 * SPDX-License-Identifier: Apache-2.0
 */

package modpackChecker;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static modpackChecker.ModpackChecker.LOGGER;

public class ConfigManager {
    // Default configuration values
    private static final String DEFAULT_VERSION = "1.2.3";
    
    // Use standard server config folder
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getGameDir().resolve("config");
    private static final Path SERVER_CONFIG_PATH = CONFIG_DIR.resolve("modpack-checker-server.toml");
    private static final Path CLIENT_CONFIG_PATH = CONFIG_DIR.resolve("modpack-checker-client.toml");
    
    // Environment detection
    private static boolean isClientEnvironment = false;
    
    // Server configuration
    public static boolean enable = true;
    public static String expectedVersion = DEFAULT_VERSION;
    public static String noModMessage = "❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft";
    public static String wrongVersionMessage = "❌ Please install modpack version {version}: https://triibu.tech/minecraft";
    public static String serverErrorMessage = "❌ Server configuration error. Please contact an administrator.";
    
    // Client configuration
    public static String clientVersion = DEFAULT_VERSION;
    
    public static void init(boolean isClient) {
        isClientEnvironment = isClient;
        
        try {
            // Create config directory if it doesn't exist
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            
            // Copy default config files if they don't exist (only appropriate ones for current environment)
            copyDefaultConfigs();
            
            // Load configurations (only appropriate ones for current environment)
            if (isClientEnvironment) {
                loadClientConfig();
                LOGGER.debug("Client configuration loaded successfully");
            } else {
                loadServerConfig();
                LOGGER.debug("Server configuration loaded successfully");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to initialize configuration", e);
        }
    }
    
    public static void reload() {
        try {
            if (isClientEnvironment) {
                loadClientConfig();
                LOGGER.debug("Client configuration reloaded successfully");
            } else {
                loadServerConfig();
                LOGGER.debug("Server configuration reloaded successfully");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to reload configuration", e);
        }
    }
    
    private static void copyDefaultConfigs() throws IOException {
        if (isClientEnvironment) {
            // Only create client config on client
            if (!Files.exists(CLIENT_CONFIG_PATH)) {
                String defaultClientConfig = """
                    # Modpack Checker Client Configuration
                    
                    # Current modpack version - this should match the server's expected version
                    version = "%s"
                    """.formatted(DEFAULT_VERSION);
                Files.writeString(CLIENT_CONFIG_PATH, defaultClientConfig);
                LOGGER.info("Created default client configuration file");
            }
        } else {
            // Only create server config on server
            if (!Files.exists(SERVER_CONFIG_PATH)) {
                String defaultServerConfig = """
                    # Modpack Checker Server Configuration
                    
                    # Enable or disable modpack version checking
                    enable = true
                    
                    # Expected modpack version that clients must have
                    expected_version = "%s"
                    
                    # Kick messages for different scenarios
                    [messages]
                    # Message shown when client doesn't have the mod installed
                    no_mod = "❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft"
                    
                    # Message shown when client has wrong version (use {version} as placeholder)
                    wrong_version = "❌ Please install modpack version {version}: https://triibu.tech/minecraft"
                    
                    # Message shown when there's a server configuration error
                    server_error = "❌ Server configuration error. Please contact an administrator."
                    """.formatted(DEFAULT_VERSION);
                Files.writeString(SERVER_CONFIG_PATH, defaultServerConfig);
                LOGGER.info("Created default server configuration file");
            }
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
            expectedVersion = config.getOrElse("expected_version", DEFAULT_VERSION);
            
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
            clientVersion = config.getOrElse("version", DEFAULT_VERSION);
            LOGGER.info("Client configuration loaded - version: {}", clientVersion);
        } catch (Exception e) {
            LOGGER.error("Failed to load client configuration", e);
        }
    }
    
    public static String formatMessage(String message, String version) {
        return message.replace("{version}", version);
    }
} 