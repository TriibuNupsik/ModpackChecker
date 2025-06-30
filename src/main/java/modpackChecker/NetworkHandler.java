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
    public static final Identifier VERSION_CHECK_CHANNEL = new Identifier("modpack-checker", "version_check");

    public static void register() {
        LOGGER.info("Registering network handlers");
        registerVersionHandler();
    }

    private static void registerVersionHandler() {
        // Send version request to client during login - only if enabled
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (ConfigManager.enable) {
                String conInfo = handler.getConnectionInfo();
                String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(","));

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
                String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                LOGGER.debug("Client {} doesn't have the mod installed, disconnecting", name);
                handler.disconnect(Text.of(ConfigManager.noModMessage));
            } else {
                try {
                    String clientVersion = buf.readString(64);
                    String conInfo = handler.getConnectionInfo();
                    String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                    String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(","));
                    
                    if (!clientVersion.equals(ConfigManager.expectedVersion)) {
                        String message = ConfigManager.formatMessage(ConfigManager.wrongVersionMessage, ConfigManager.expectedVersion);
                        LOGGER.debug("Client {} has wrong version: {} (expected: {}), disconnecting", name, clientVersion, ConfigManager.expectedVersion);
                        handler.disconnect(Text.of(message));
                    } else {
                        LOGGER.debug("Version verified for {}: {} (version: {})", name, uuid, clientVersion);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to process version check", e);
                    String conInfo = handler.getConnectionInfo();
                    String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                    LOGGER.debug("Version check failed for {}, disconnecting", name);
                    handler.disconnect(Text.of(ConfigManager.serverErrorMessage));
                }
            }
        });
    }
}
