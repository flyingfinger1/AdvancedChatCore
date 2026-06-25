/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.mixin;

import io.github.darkkronicle.advancedchatcore.chat.ChatHistory;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MixinMinecraftClient {

    // 26.2: the single-arg Yarn disconnect(Screen) is gone. The real terminal implementation that
    // all overloads delegate to is disconnect(Screen, boolean, boolean); injecting at its RETURN
    // covers every disconnect path.
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("RETURN"))
    public void onDisconnect(Screen screen, boolean keepResourcePacks, boolean keepLoadingScreen, CallbackInfo ci) {
        // Clear data on disconnect
        if (ConfigStorage.General.CLEAR_ON_DISCONNECT.config.getBooleanValue()) {
            ChatHistory.getInstance().clearAll();
        }
    }

    // NOTE: chat-screen substitution (formerly intercepting Minecraft.openChatScreen(String)) moved to
    // MixinGui in 26.x, since the chat entry points now live on net.minecraft.client.gui.Gui.
}
