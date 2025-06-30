/*
 * Copyright (c) 2025. Triibunupsik
 * SPDX-License-Identifier: Apache-2.0
 */

package modpackChecker.client;

import modpackChecker.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ModpackCheckerClient implements ClientModInitializer {
    public static final String MOD_ID = "ModpackCheckerClient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static final Identifier VERSION_CHECK_CHANNEL = new Identifier("modpack-checker", "version_check");

    @Override
    public void onInitializeClient() {
        registerModpackCheckResponder();
    }

    private static void registerModpackCheckResponder() {
        // Initialize client configuration
        ConfigManager.init(true);
        
        // Register client-side network handler to respond to version checks
        ClientLoginNetworking.registerGlobalReceiver(VERSION_CHECK_CHANNEL, ((client, handler, buf, listenerAdder) -> {
            LOGGER.debug("Received version check request from server");
            
            PacketByteBuf responseBuf = PacketByteBufs.create();
            responseBuf.writeString(ConfigManager.clientVersion, 64);
            
            LOGGER.debug("Sending version response: {}", ConfigManager.clientVersion);
            return CompletableFuture.completedFuture(responseBuf);
        }));
        
        LOGGER.info("ModpackChecker client mod loaded");
        LOGGER.debug("Client version: {}", ConfigManager.clientVersion);
    }
}
