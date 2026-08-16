/*
 * Copyright (c) 2026. Triibunupsik
 * SPDX-License-Identifier: Apache-2.0
 */

package modpackChecker.client.mixin;

import modpackChecker.ModpackChecker;
import modpackChecker.NetworkHandler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(method = "openToLan", at = @At("RETURN"))
    private void modpackchecker$onOpenToLan(
            GameMode gameMode,
            boolean cheatsAllowed,
            int port,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) {
            NetworkHandler.register();
            ModpackChecker.LOGGER.info("Detected LAN environment, ModpackChecker enabled");
        }
    }
}