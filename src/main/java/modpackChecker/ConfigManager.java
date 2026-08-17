/*
 * Copyright (c) 2026. Triibunupsik
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
    // Config folder constants
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getGameDir().resolve("config");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("modpack-checker.toml");

    // Default configuration values
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String DEV_VERSION = "development";
    private static final boolean DEFAULT_ENABLE = true;
    private static final String DEFAULT_NO_MOD_MESSAGE = "Modpack not installed! \\n\\n Please install modpack version \\\"{version}\\\" \\n <your-modpack-link>";
    private static final String DEFAULT_WRONG_VERSION_MESSAGE = "Wrong modpack version installed! \\n\\n Please install modpack version \\\"{version}\\\" \\n <your-modpack-link>";
    private static final String DEFAULT_SERVER_ERROR_MESSAGE = "Server configuration error. Please contact an administrator.";

    // Server configuration
    public static boolean enable = DEFAULT_ENABLE;
    public static String version = DEFAULT_VERSION;
    public static String noModMessage = DEFAULT_NO_MOD_MESSAGE;
    public static String wrongVersionMessage = DEFAULT_WRONG_VERSION_MESSAGE;
    public static String serverErrorMessage = DEFAULT_SERVER_ERROR_MESSAGE;
    
    public static void init() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            } // Create config directory if it doesn't exist
            if (!Files.exists(CONFIG_PATH)) {
                copyDefaultConfig();
            } // Copy default config file if it doesn't exist

            loadConfig();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize configuration", e);
        }
    }
    
    private static void copyDefaultConfig() throws IOException {
        String defaultConfig = """
            # Modpack Checker Configuration
            # This file contains both server and client configuration
            
            # Modpack version of the server and client, that must match
            # version "development" always allows joining
            version = "%s"
            
            # Server Configuration
            [server]
            # Enable or disable modpack version checking
            enable = %s
            
            # Kick messages for different scenarios
            [server.messages]
            # Message shown when client doesn't have the mod installed
            no_mod = "%s"
            
            # Message shown when client has wrong version (use {version} as placeholder)
            wrong_version = "%s"
            
            # Message shown when there's a server configuration error
            server_error = "%s"
            """.formatted(
                DEFAULT_VERSION,
                DEFAULT_ENABLE,
                DEFAULT_NO_MOD_MESSAGE,
                DEFAULT_WRONG_VERSION_MESSAGE,
                DEFAULT_SERVER_ERROR_MESSAGE
            );
        Files.writeString(CONFIG_PATH, defaultConfig);
        LOGGER.info("Created default configuration file");
    }

    public static int loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            LOGGER.warn("Configuration file not found: {}", CONFIG_PATH);
            return 0;
        }
        
        try (FileConfig config = FileConfig.of(CONFIG_PATH, TomlFormat.instance())) {
            config.load();

            version = config.getOrElse("version", DEFAULT_VERSION);

            // Load server configuration
            Config serverConfig = config.get("server");
            if (serverConfig != null) {
                enable =  serverConfig.getOrElse("enable", DEFAULT_ENABLE);
                // Load server messages
                Config messages = serverConfig.get("messages");
                if (messages != null) {
                    noModMessage = messages.getOrElse("no_mod", DEFAULT_NO_MOD_MESSAGE);
                    wrongVersionMessage = messages.getOrElse("wrong_version", DEFAULT_WRONG_VERSION_MESSAGE);
                    serverErrorMessage = messages.getOrElse("server_error", DEFAULT_SERVER_ERROR_MESSAGE);
                }
            }
            LOGGER.info("[Debug] Configuration loaded - server enable: {}, version: {}", enable, version);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to load, configuration invalid", e);
            return 0;
        }
    }
    
    public static String formatMessage(String message, String version) {
        return message.replace("{version}", version);
    }

    // Check if a version is the dev version (always allows joining)
    public static boolean isDevVersion(String version) {
        return DEV_VERSION.equals(version);
    }

    // Check if versions are compatible (same version or client has dev version)
    public static boolean areVersionsCompatible(String clientVersion, String serverVersion) {
        return clientVersion.equals(serverVersion) || isDevVersion(clientVersion);
    }
}
