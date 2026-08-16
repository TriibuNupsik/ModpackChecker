/*
 * Copyright (c) 2026. Triibunupsik
 * SPDX-License-Identifier: Apache-2.0
 */

package modpackChecker.client;

import modpackChecker.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

import static modpackChecker.ModpackChecker.LOGGER;

public class ModpackCheckerClient implements ClientModInitializer {
    private static final Identifier VERSION_CHECK_CHANNEL = new Identifier("modpack-checker", "version_check");

    @Override
    public void onInitializeClient() {
        // Register client-side network handler to respond to version checks
        ClientLoginNetworking.registerGlobalReceiver(VERSION_CHECK_CHANNEL, ((client, handler, buf, listenerAdder) -> {
            LOGGER.info("[Debug] Received version check request from server");

            PacketByteBuf responseBuf = PacketByteBufs.create();
            responseBuf.writeString(ConfigManager.clientVersion, 64);

            LOGGER.info("[Debug] Sending version response: {}", ConfigManager.clientVersion);
            return CompletableFuture.completedFuture(responseBuf);
        }));

        LOGGER.info("Client loaded");
        LOGGER.info("[Debug] Client version: {}", ConfigManager.clientVersion);
    }
}
