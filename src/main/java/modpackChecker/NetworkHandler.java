/*
 * Copyright (c) 2025. Triibunupsik
 * SPDX-License-Identifier: Apache-2.0
 */

package modpackChecker;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static modpackChecker.ModpackChecker.*;

public class NetworkHandler {
    public static final Identifier VERSION_CHECK_CHANNEL = Identifier.of("modpack-checker", "version_check");

    public static void register() {
        LOGGER.info("Registering network handlers");
        registerVersionHandler();
    }

    /**
     * Safely parse connection info to extract player name and UUID
     */
    private static String[] parseConnectionInfo(String conInfo) {
        try {
            String name = "Unknown";
            String uuid = "Unknown";
            
            // Try to extract name
            int nameIndex = conInfo.indexOf("name=");
            if (nameIndex != -1) {
                int nameStart = nameIndex + 5;
                int nameEnd = conInfo.indexOf(",", nameStart);
                if (nameEnd == -1) {
                    nameEnd = conInfo.length();
                }
                if (nameStart < nameEnd && nameEnd <= conInfo.length()) {
                    name = conInfo.substring(nameStart, nameEnd);
                }
            }
            
            // Try to extract UUID
            int uuidIndex = conInfo.indexOf("id=");
            if (uuidIndex != -1) {
                int uuidStart = uuidIndex + 3;
                int uuidEnd = conInfo.indexOf(",", uuidStart);
                if (uuidEnd == -1) {
                    uuidEnd = conInfo.length();
                }
                if (uuidStart < uuidEnd && uuidEnd <= conInfo.length()) {
                    uuid = conInfo.substring(uuidStart, uuidEnd);
                }
            }
            
            return new String[]{name, uuid};
        } catch (Exception e) {
            LOGGER.warn("Failed to parse connection info: {}", conInfo, e);
            return new String[]{"Unknown", "Unknown"};
        }
    }

    private static void registerVersionHandler() {
        // Send version request to client during login - only if enabled
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (ConfigManager.enable) {
                String conInfo = handler.getConnectionInfo();
                String[] playerInfo = parseConnectionInfo(conInfo);
                String name = playerInfo[0];
                String uuid = playerInfo[1];

                PacketByteBuf buf = PacketByteBufs.create();
                sender.sendPacket(VERSION_CHECK_CHANNEL, buf);

                LOGGER.debug("Sending version request to {}: {}", name, uuid);
            } else {
                LOGGER.debug("Modpack checking is disabled, skipping version check");
            }
        });

        // Handle version response from client - only if enabled
        ServerLoginNetworking.registerGlobalReceiver(VERSION_CHECK_CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!ConfigManager.enable) {
                LOGGER.debug("Modpack checking is disabled, allowing connection");
                return;
            }
            
            if (!understood) {
                // Client doesn't have the mod installed
                String conInfo = handler.getConnectionInfo();
                String[] playerInfo = parseConnectionInfo(conInfo);
                String name = playerInfo[0];
                LOGGER.debug("Client {} doesn't have the mod installed, disconnecting", name);
                handler.disconnect(Text.of(ConfigManager.noModMessage));
            } else {
                try {
                    String clientVersion = buf.readString(64);
                    String conInfo = handler.getConnectionInfo();
                    String[] playerInfo = parseConnectionInfo(conInfo);
                    String name = playerInfo[0];
                    String uuid = playerInfo[1];
                    
                    if (!ConfigManager.areVersionsCompatible(clientVersion, ConfigManager.expectedVersion)) {
                        String message = ConfigManager.formatMessage(ConfigManager.wrongVersionMessage, ConfigManager.expectedVersion);
                        LOGGER.debug("Client {} has incompatible version: {} (expected: {}), disconnecting", name, clientVersion, ConfigManager.expectedVersion);
                        handler.disconnect(Text.of(message));
                    } else {
                        LOGGER.debug("Version verified for {}: {} (version: {})", name, uuid, clientVersion);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to process version check", e);
                    String conInfo = handler.getConnectionInfo();
                    String[] playerInfo = parseConnectionInfo(conInfo);
                    String name = playerInfo[0];
                    LOGGER.debug("Version check failed for {}, disconnecting", name);
                    handler.disconnect(Text.of(ConfigManager.serverErrorMessage));
                }
            }
        });
    }
}
