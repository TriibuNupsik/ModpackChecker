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
    public enum returnReason {
        UNKNOWN,
        SUCCESS,
        MISSING_CONFIG,
        BROKEN_CONFIG
    }

    // Config folder constants
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getGameDir().resolve("config");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("modpack-checker.toml");

    // Default configuration values
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String DEV_VERSION = "development";
    private static final boolean DEFAULT_ENABLE_SERVER = true;
    private static final boolean DEFAULT_ENABLE_LAN = true;
    private static final String DEFAULT_NO_MOD_MESSAGE = "Modpack not installed! \\n\\n Please install the modpack, version \\\"{version}\\\" \\n <your-modpack-link>";
    private static final String DEFAULT_WRONG_VERSION_MESSAGE = "Wrong modpack version installed! \\n\\n Please install the modpack version \\\"{version}\\\" \\n <your-modpack-link>";
    private static final String DEFAULT_SERVER_ERROR_MESSAGE = "Server configuration error. Please contact an administrator.";

    // Server configuration
    public static boolean enableServer = DEFAULT_ENABLE_SERVER;
    public static boolean enableLan = DEFAULT_ENABLE_LAN;
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
            
            # Enable or disable modpack version checking
            enable_server = %s
            enable_lan = %s
            
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
                DEFAULT_ENABLE_SERVER,
                DEFAULT_ENABLE_LAN,
                DEFAULT_NO_MOD_MESSAGE,
                DEFAULT_WRONG_VERSION_MESSAGE,
                DEFAULT_SERVER_ERROR_MESSAGE
            );
        Files.writeString(CONFIG_PATH, defaultConfig);
        LOGGER.info("Created default configuration file");
    }

    public static returnReason loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            LOGGER.error("Configuration file not found: {}", CONFIG_PATH);
            init();
            return returnReason.MISSING_CONFIG;
        }
        try (FileConfig config = FileConfig.of(CONFIG_PATH, TomlFormat.instance())) {
            config.load();

            if (isConfigBroken(config, "root", "version", "enable_server", "enable_lan", "messages")) {
                return returnReason.BROKEN_CONFIG;
            }
            String loadedVersion = config.get("version");
            boolean loadedEnableServer = config.get("enable_server");
            boolean loadedEnableLan = config.get("enable_lan");

            Config messages = config.get("messages");
            if (isConfigBroken(messages, "[messages]", "no_mod", "wrong_version", "server_error")) {
                return returnReason.BROKEN_CONFIG;
            }
            String loadedNoModMessage = messages.get("no_mod");
            String loadedWrongVersionMessage = messages.get("wrong_version");
            String loadedServerErrorMessage = messages.get("server_error");

            version = loadedVersion;
            enableServer = loadedEnableServer;
            enableLan = loadedEnableLan;
            noModMessage = loadedNoModMessage;
            wrongVersionMessage = loadedWrongVersionMessage;
            serverErrorMessage = loadedServerErrorMessage;

            LOGGER.debug("Configuration loaded: checking enabled={}, version={}", enableServer, version);
            return returnReason.SUCCESS;
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            LOGGER.error("Failed to read configuration: {} Keeping the previous configuration.", reason);
            LOGGER.error("Configuration reload failure", e);
            return returnReason.UNKNOWN;
        }
    }

    private static boolean isConfigBroken(Config config, String section, String... keys) {
        Set<String> unknownKeys = new TreeSet<>(config.valueMap().keySet());
        unknownKeys.removeAll(Set.of(keys));
        if (!unknownKeys.isEmpty()) {
            LOGGER.error(
                    "Unknown configuration key(s) in {}: {}. Keeping the previous configuration.",
                    section,
                    String.join(", ", unknownKeys)
            );
        }
        for (String key : keys) {
            if (config.get(key) == null) {
                LOGGER.error("Missing required configuration value {} in {}. Keeping the previous configuration.", key, section);
                return true;
            }
        }
        return false;
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
