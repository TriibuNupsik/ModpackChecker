/*
 * Copyright (c) 2026. Triibunupsik
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
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        LOGGER.info("[Debug] Registering network handlers");
        registerVersionHandler();
    }

    private static void registerVersionHandler() {
        // Send version request to client during login - only if enabled
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (ConfigManager.enable) {
                String conInfo = handler.getConnectionInfo();
                String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(",", conInfo.indexOf("id=")+3));

                PacketByteBuf buf = PacketByteBufs.create();
                sender.sendPacket(VERSION_CHECK_CHANNEL, buf);

                LOGGER.info("[Debug] Sending version request to {}: {}", name, uuid);
            } else {
                LOGGER.info("[Debug] Modpack checking is disabled, skipping version check");
            }
        });

        // Handle version response from client - only if enabled
        ServerLoginNetworking.registerGlobalReceiver(VERSION_CHECK_CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!ConfigManager.enable) {
                LOGGER.info("[Debug] Modpack checking is disabled, allowing connection");
                return;
            }

            String conInfo = handler.getConnectionInfo();
            String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
            String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(",", conInfo.indexOf("id=")+3));

            if (!understood) {
                // Client doesn't have the mod installed
                LOGGER.info("[Debug] Client {} doesn't have the mod installed, disconnecting", name);
                String message = ConfigManager.formatMessage(ConfigManager.noModMessage, ConfigManager.version);
                handler.disconnect(Text.of(message));
            } else {
                try {
                    String clientVersion = buf.readString(64);
                    if (!ConfigManager.areVersionsCompatible(clientVersion, ConfigManager.version)) {
                        String message = ConfigManager.formatMessage(ConfigManager.wrongVersionMessage, ConfigManager.version);
                        LOGGER.info("[Debug] Client {} has incompatible version: {} (expected: {}), disconnecting", name, clientVersion, ConfigManager.version);
                        handler.disconnect(Text.of(message));
                    } else {
                        LOGGER.info("[Debug] Version verified for {}: {} (version: {})", name, uuid, clientVersion);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to process version check", e);
                    LOGGER.info("[Debug] Version check failed for {}, disconnecting", name);
                    handler.disconnect(Text.of(ConfigManager.serverErrorMessage));
                }
            }
        });
    }
}
