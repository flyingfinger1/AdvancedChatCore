/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.mixin;

import io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.AdvancedSleepingChatScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Substitutes AdvancedChat's own chat screens for the vanilla ones.
 *
 * <p>In 1.19.4 this was done by intercepting {@code Minecraft.openChatScreen(String)}. In 26.x the
 * chat-screen entry points moved onto {@link Gui} ({@code openChatScreen}/{@code openChatAndAddText}),
 * so the hook lives here now.
 */
@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public class MixinGui {

    @Inject(
            method = "openChatScreen(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void advancedchatcore$openChatScreen(ChatComponent.ChatMethod method, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        // While sleeping, vanilla shows the in-bed chat screen; mirror that with our sleeping variant
        // so closing it still wakes the player.
        Screen screen = (mc.player != null && mc.player.isSleeping())
                ? new AdvancedSleepingChatScreen()
                : new AdvancedChatScreen(method.prefix());
        mc.gui.setScreen(screen);
        ci.cancel();
    }

    @Inject(
            method = "openChatAndAddText(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;Ljava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void advancedchatcore$openChatAndAddText(ChatComponent.ChatMethod method, String text, CallbackInfo ci) {
        Minecraft.getInstance().gui.setScreen(new AdvancedChatScreen(text));
        ci.cancel();
    }
}
