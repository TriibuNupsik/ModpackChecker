package modpackChecker;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
            if (ConfigManager.enable) {
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
                handler.disconnect(Text.of(ConfigManager.noModMessage));
            } else {
                if (ConfigManager.enable) {
                    try {
                        String clientVersion = buf.readString(64);
                        
                        if (!clientVersion.equals(ConfigManager.expectedVersion)) {
                            String message = ConfigManager.formatMessage(ConfigManager.wrongVersionMessage, ConfigManager.expectedVersion);
                            handler.disconnect(Text.of(message));
                        } else {
                            String conInfo = handler.getConnectionInfo();
                            String name = conInfo.substring(conInfo.indexOf("name=")+5, conInfo.indexOf(",", conInfo.indexOf("name=")+5));
                            String uuid = conInfo.substring(conInfo.indexOf("id=")+3, conInfo.indexOf(","));
                            
                            LOGGER.info("Version verified for {}: {} (version: {})", name, uuid, clientVersion);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to process version check", e);
                        handler.disconnect(Text.of(ConfigManager.serverErrorMessage));
                    }
                }
            }
        });
    }
}
