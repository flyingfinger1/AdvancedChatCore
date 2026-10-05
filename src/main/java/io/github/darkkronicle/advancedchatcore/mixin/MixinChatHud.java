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

    // 26.3: the private addMessage(Component, MessageSignature, GuiMessageSource, GuiMessageTag) funnel
    // that 26.2 intercepted was removed. The three public entry points are now hooked individually; each
    // routes the message to the dispatcher and cancels vanilla. AdvancedChat re-adds the processed
    // message via addPlayerMessage (see ChatHistoryProcessor), guarded by FORWARDING_TO_HUD so the
    // re-add renders normally instead of recursing. (addPlayerMessage is 3-arg on 26.3 — no GuiMessageSource.)
    @Inject(
            method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void advancedchatcore$onPlayerMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator, CallbackInfo ci) {
        if (ChatHistoryProcessor.FORWARDING_TO_HUD) {
            return;
        }
        MessageDispatcher.getInstance().handleText(message, signature, indicator);
        ci.cancel();
    }

    @Inject(method = "addClientSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void advancedchatcore$onClientSystemMessage(Component message, CallbackInfo ci) {
        if (ChatHistoryProcessor.FORWARDING_TO_HUD) {
            return;
        }
        MessageDispatcher.getInstance().handleText(message, null, null);
        ci.cancel();
    }

    @Inject(method = "addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void advancedchatcore$onServerSystemMessage(Component message, CallbackInfo ci) {
        if (ChatHistoryProcessor.FORWARDING_TO_HUD) {
            return;
        }
        MessageDispatcher.getInstance().handleText(message, null, null);
        ci.cancel();
    }

    // Yarn ChatHud.clear(Z) is ChatComponent.clearMessages(Z).
    @Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
    private void clearMessages(boolean clearTextHistory, CallbackInfo ci) {
        if (!clearTextHistory) {
            // F3+D "clear chat": let vanilla clear the display. 26.2 also cleared AdvancedChat's own
            // history here (via MixinKeyboard/handleDebugKeys, removed in 26.3), but it must NOT be done
            // from inside clearMessages: ChatHistory.clearAll() re-enters this method through HUD's
            // WindowManager.clear -> ChatComponent.clear -> clearMessages, causing infinite recursion
            // (StackOverflow). So F3+D no longer clears AdvancedChat's stored history.
            return;
        }
        if (!ConfigStorage.General.CLEAR_ON_DISCONNECT.config.getBooleanValue()) {
            // Cancel clearing if it's turned off
            ci.cancel();
        }
    }

    @Inject(method = "isChatFocused", at = @At("HEAD"), cancellable = true)
    private void isChatFocused(CallbackInfoReturnable<Boolean> ci) {
        // If the chat is focused. The current screen is reached through Gui (minecraft.gui.screen()).
        ci.setReturnValue(AdvancedChatScreen.PERMANENT_FOCUS || minecraft.gui.screen() instanceof AdvancedChatScreen);
    }

    // The in-bed chat auto-opens through ChatComponent.openScreen(ChatMethod, ChatConstructor), called
    // directly from Gui.tick — NOT through Gui.openChatScreen (which MixinGui handles). Hook it here so
    // sleeping shows AdvancedChat's own screen instead of vanilla's InBedChatScreen.
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
