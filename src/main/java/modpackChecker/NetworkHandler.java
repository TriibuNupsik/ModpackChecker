package modpackChecker;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;

import static modpackChecker.ModpackChecker.*;

public class NetworkHandler {
    public static final Identifier VERSION_CHECK_CHANNEL = new Identifier("modpackChecker", "version_check");

    public static void register() {
        ModpackChecker.LOGGER.info("Registering network handlers");
        registerVersionHandler();
    }

    private static void registerVersionHandler() {
        // Send version request to client during login
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (isModCheckEnabled) {
                String conInfo = handler.getConnectionInfo();
                String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(","));

                PacketByteBuf buf = PacketByteBufs.create();
                sender.sendPacket(VERSION_CHECK_CHANNEL, buf);

                ModpackChecker.LOGGER.info("Sending version request to {}: {}", name, uuid);
            }
        });

        // Handle version response from client
        ServerLoginNetworking.registerGlobalReceiver(VERSION_CHECK_CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (!understood) {
                // Client doesn't have the mod installed
                handler.disconnect(Text.of("❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft"));
            } else {
                if (isModCheckEnabled) {
                    try {
                        String clientVersion = buf.readString(64);
                        
                        // Check if expected version file exists
                        if (!Files.exists(VERSION_FILE_PATH)) {
                            LOGGER.warn("Expected version file not found. Creating default version file.");
                            Files.writeString(VERSION_FILE_PATH, "1.0.0");
                        }
                        
                        String expectedVersion = Files.readString(VERSION_FILE_PATH).trim();
                        
                        if (!clientVersion.equals(expectedVersion)) {
                            handler.disconnect(Text.of("❌ Please install modpack version " + expectedVersion + ": https://triibu.tech/minecraft"));
                        } else {
                            String conInfo = handler.getConnectionInfo();
                            String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                            String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(","));
                            
                            LOGGER.info("Version verified for {}: {} (version: {})", name, uuid, clientVersion);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Failed to read expected version file", e);
                        handler.disconnect(Text.of("❌ Server configuration error. Please contact an administrator."));
                    }
                }
            }
        });
    }
}
