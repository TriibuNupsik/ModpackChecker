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
        LOGGER.debug("Registering network handlers");
        registerVersionHandler();
    }

    private static void registerVersionHandler() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (!(ConfigManager.enableServer && !server.isSingleplayer())
                    && !(ConfigManager.enableLan && server.isSingleplayer())) {
                LOGGER.debug("checking disabled - skipping version check");
                return;
            }
            String conInfo = handler.getConnectionInfo();
            String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
            String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(",", conInfo.indexOf("id=")+3));

            PacketByteBuf buf = PacketByteBufs.create();
            sender.sendPacket(VERSION_CHECK_CHANNEL, buf);

            LOGGER.debug("Requesting modpack version from {} ({})", name, uuid);
        });

        ServerLoginNetworking.registerGlobalReceiver(VERSION_CHECK_CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!(ConfigManager.enableServer && !server.isSingleplayer())
                    && !(ConfigManager.enableLan && server.isSingleplayer())) {
                LOGGER.debug("checking disabled - allowing connection");
                return;
            }

            String conInfo = handler.getConnectionInfo();
            String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
            String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(",", conInfo.indexOf("id=")+3));

            if (!understood) {
                // Client doesn't have the mod installed
                String message = ConfigManager.formatMessage(ConfigManager.noModMessage, ConfigManager.version);
                handler.disconnect(Text.of(message));
                LOGGER.info("Rejected {}: Modpack Checker is not installed", name);
                return;
            }

            try {
                String clientVersion = buf.readString(64);

                if (!ConfigManager.areVersionsCompatible(clientVersion, ConfigManager.version)) {
                    // Client has wrong version
                    String message = ConfigManager.formatMessage(ConfigManager.wrongVersionMessage, ConfigManager.version);
                    handler.disconnect(Text.of(message));
                    LOGGER.info("Rejected {}: modpack version {} is incompatible (server version: {})", name, clientVersion, ConfigManager.version);
                    return;
                }

                LOGGER.debug("Verified modpack version {} for {} ({})", clientVersion, name, uuid);
            } catch (Exception e) {
                LOGGER.error("Failed to process version check for {} ({}) ", name, uuid, e);
                handler.disconnect(Text.of(ConfigManager.serverErrorMessage));
            }
        });
    }
}
