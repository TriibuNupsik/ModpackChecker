/*
 * Copyright (c) 2026. Triibunupsik
 * SPDX-License-Identifier: Apache-2.0
 */

package modpackChecker;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static modpackChecker.ConfigManager.CONFIG_DIR;
import static modpackChecker.ModpackChecker.LOGGER;

/** Converts the separate legacy client/server files to the unified configuration. */
public final class ConfigMigrator {
    private static final Path NEW_CONFIG = CONFIG_DIR.resolve("modpack-checker.toml");
    private static final Path CLIENT_CONFIG = CONFIG_DIR.resolve("modpack-checker-client.toml");
    private static final Path SERVER_CONFIG = CONFIG_DIR.resolve("modpack-checker-server.toml");

    private ConfigMigrator() {}

    public static boolean hasLegacyConfig() {
        return Files.exists(CLIENT_CONFIG) || Files.exists(SERVER_CONFIG);
    }

    /** @return true when migration completed, or no legacy files needed migration. */
    public static boolean migrateIfNeeded() {
        if (Files.exists(NEW_CONFIG) || !hasLegacyConfig()) {
            return true;
        }

        Path temporary = CONFIG_DIR.resolve("modpack-checker.toml.migrating");
        try {
            Config client = Files.exists(CLIENT_CONFIG) ? read(CLIENT_CONFIG) : null;
            Config server = Files.exists(SERVER_CONFIG) ? read(SERVER_CONFIG) : null;

            // The server's expected version is authoritative when both old files exist.
            String version = stringValue(server, "expected_version");
            if (version == null) {
                version = stringValue(client, "version");
            }
            if (version == null) {
                throw new IOException("Neither legacy config contains a version value");
            }

            Boolean enabled = server == null ? Boolean.TRUE : booleanValue(server, "enable");
            if (enabled == null) {
                throw new IOException("Legacy server config does not contain enable");
            }

            Config messages = server == null ? null : server.get("messages");
            String noMod = messageValue(messages, "no_mod", ConfigManager.DEFAULT_NO_MOD_MESSAGE);
            String wrongVersion = messageValue(messages, "wrong_version", ConfigManager.DEFAULT_WRONG_VERSION_MESSAGE);
            String serverError = messageValue(messages, "server_error", ConfigManager.DEFAULT_SERVER_ERROR_MESSAGE);

            String migrated = """
                    # Modpack Checker Configuration
                    # This file was migrated from server and client configuration, it contains both

                    version = %s
                    enable_server = %s
                    enable_lan = false

                    [messages]
                    no_mod = %s
                    wrong_version = %s
                    server_error = %s
                    """.formatted(
                        quote(version),
                        enabled,
                        quote(noMod),
                        quote(wrongVersion),
                        quote(serverError)
                    );

            Files.writeString(temporary, migrated);
            if (!isValid(temporary, version, enabled, noMod, wrongVersion, serverError)) {
                throw new IOException("Generated configuration did not pass validation");
            }
            try {
                Files.move(temporary, NEW_CONFIG, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, NEW_CONFIG);
            }
            if (!isValid(NEW_CONFIG, version, enabled, noMod, wrongVersion, serverError)) {
                Files.deleteIfExists(NEW_CONFIG);
                throw new IOException("Migrated configuration failed validation after being written");
            }

            Files.deleteIfExists(CLIENT_CONFIG);
            Files.deleteIfExists(SERVER_CONFIG);
            LOGGER.info("Migrated legacy Modpack Checker configuration to {}", NEW_CONFIG);
            return true;
        } catch (Exception e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            LOGGER.error("Could not migrate legacy Modpack Checker configuration. The legacy files were kept.", e);
            return false;
        }
    }

    private static Config read(Path path) {
        try (FileConfig config = FileConfig.of(path, TomlFormat.instance())) {
            config.load();
            return config;
        }
    }

    private static String stringValue(Config config, String key) throws IOException {
        if (config == null) return null;
        Object value = config.get(key);
        if (value == null) return null;
        if (value instanceof String string) return string;
        throw new IOException("Legacy option " + key + " must be a string");
    }

    private static Boolean booleanValue(Config config, String key) throws IOException {
        if (config == null) return null;
        Object value = config.get(key);
        if (value == null) return null;
        if (value instanceof Boolean bool) return bool;
        throw new IOException("Legacy option " + key + " must be a boolean");
    }

    private static String messageValue(Config messages, String key, String fallback) throws IOException {
        String value = stringValue(messages, key);
        return value == null ? fallback : value;
    }

    private static boolean isValid(Path path, String version, boolean enabled,
                                   String noMod, String wrongVersion, String serverError) {
        try (FileConfig config = FileConfig.of(path, TomlFormat.instance())) {
            config.load();
            Config messages = config.get("messages");
            return version.equals(config.get("version"))
                    && Boolean.valueOf(enabled).equals(config.get("enable_server"))
                    && Boolean.FALSE.equals(config.get("enable_lan"))
                    && messages != null
                    && noMod.equals(messages.get("no_mod"))
                    && wrongVersion.equals(messages.get("wrong_version"))
                    && serverError.equals(messages.get("server_error"));
        } catch (Exception e) {
            LOGGER.error("Unable to validate migrated configuration at {}", path, e);
            return false;
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
