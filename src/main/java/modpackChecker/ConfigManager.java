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
import java.util.Set;
import java.util.TreeSet;

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
            [messages]
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

            requireOnlyKeys(config, "root", "version", "server", "messages");
            String loadedVersion = requireValue(config, "version");

            Config serverConfig = requireValue(config, "server");
            requireOnlyKeys(serverConfig, "[server]", "enable");
            boolean loadedEnable = requireValue(serverConfig, "enable");

            Config messages = requireValue(config, "messages");
            requireOnlyKeys(messages, "[messages]", "no_mod", "wrong_version", "server_error");
            String loadedNoModMessage = requireValue(messages, "no_mod");
            String loadedWrongVersionMessage = requireValue(messages, "wrong_version");
            String loadedServerErrorMessage = requireValue(messages, "server_error");

            version = loadedVersion;
            enable = loadedEnable;
            noModMessage = loadedNoModMessage;
            wrongVersionMessage = loadedWrongVersionMessage;
            serverErrorMessage = loadedServerErrorMessage;

            LOGGER.debug("Configuration loaded: checking enabled={}, version={}", enable, version);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration", e);
            return 0;
        }
    }

    private static <T> T requireValue(Config config, String path) {
        T value = config.get(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing required configuration value: " + path);
        }
        return value;
    }

    private static void requireOnlyKeys(Config config, String section, String... allowedKeys) {
        Set<String> unknownKeys = new TreeSet<>(config.valueMap().keySet());
        unknownKeys.removeAll(Set.of(allowedKeys));
        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException(
                "Unknown configuration key(s) in " + section + ": " + String.join(", ", unknownKeys)
            );
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
