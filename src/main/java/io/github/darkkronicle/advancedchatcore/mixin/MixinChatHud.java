/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.mixin;

import io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.AdvancedSleepingChatScreen;
import io.github.darkkronicle.advancedchatcore.chat.ChatHistoryProcessor;
import io.github.darkkronicle.advancedchatcore.chat.MessageDispatcher;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChatComponent.class, priority = 1050)
public class MixinChatHud {

    @Shadow @Final private Minecraft minecraft;

    // 26.2: Yarn ChatHud.addMessage(Text, MessageSignatureData, MessageIndicator) is now the
    // public ChatComponent.addPlayerMessage(Component, MessageSignature, GuiMessageTag).
    @Inject(
            method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void addMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator, CallbackInfo ci) {
        // Our own re-add of a processed message: let vanilla render it instead of re-dispatching
        // (which would recurse forever).
        if (ChatHistoryProcessor.FORWARDING_TO_HUD) {
            return;
        }
        // Pass forward messages to dispatcher
        MessageDispatcher.getInstance().handleText(message, signature, indicator);
        ci.cancel();
    }

    // 26.2: Yarn ChatHud.clear(Z) is now ChatComponent.clearMessages(Z).
    @Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
    private void clearMessages(boolean clearTextHistory, CallbackInfo ci) {
        if (!clearTextHistory) {
            // This only gets called if it is the keybind f3 + d
            return;
        }
        if (!ConfigStorage.General.CLEAR_ON_DISCONNECT.config.getBooleanValue()) {
            // Cancel clearing if it's turned off
            ci.cancel();
        }
    }

    @Inject(method = "isChatFocused", at = @At("HEAD"), cancellable = true)
    private void isChatFocused(CallbackInfoReturnable<Boolean> ci) {
        // If the chat is focused. 26.2: Minecraft has no `currentScreen`; the current screen is
        // reached through Gui (minecraft.gui.screen()).
        ci.setReturnValue(AdvancedChatScreen.PERMANENT_FOCUS || minecraft.gui.screen() instanceof AdvancedChatScreen);
    }

    // 26.2: the in-bed chat auto-opens through ChatComponent.openScreen(ChatMethod, ChatConstructor),
    // called directly from Gui.tick — NOT through Gui.openChatScreen (which MixinGui handles). Hook it
    // here so sleeping shows AdvancedChat's own screen (restoring the original behavior) instead of
    // vanilla's InBedChatScreen. openScreen only does gui.setScreen(createScreen(...)), so cancelling
    // at HEAD and opening our screen directly is side-effect-free.
    @Inject(
            method = "openScreen(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;Lnet/minecraft/client/gui/screens/ChatScreen$ChatConstructor;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void advancedchatcore$openSleepingScreen(CallbackInfo ci) {
        if (minecraft.player != null && minecraft.player.isSleeping()) {
            minecraft.setScreenAndShow(new AdvancedSleepingChatScreen());
            ci.cancel();
        }
    }
}
