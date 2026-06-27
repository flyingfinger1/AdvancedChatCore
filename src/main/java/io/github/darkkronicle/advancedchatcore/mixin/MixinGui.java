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
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Substitutes AdvancedChat's own chat screens for the vanilla ones.
 *
 * <p>In 1.19.4 this was done by intercepting {@code Minecraft.openChatScreen(String)}. The chat-screen
 * entry point is {@code openChatScreen(ChatComponent.ChatMethod)}: in 26.1 it lives on
 * {@link Minecraft}, whereas in 26.2 it moved onto {@code net.minecraft.client.gui.Gui}. This 26.1
 * backport therefore targets {@code Minecraft}.
 *
 * <p>26.1 has no {@code openChatAndAddText} entry point (that was added in 26.2); in 26.1 prefilled
 * chat text is restored through {@code ChatComponent.Draft} inside the screen, so there is no second
 * hook here.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
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
        mc.setScreenAndShow(screen);
        ci.cancel();
    }
}
