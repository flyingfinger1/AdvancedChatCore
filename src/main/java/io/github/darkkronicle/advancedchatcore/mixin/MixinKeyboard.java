/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.mixin;

import io.github.darkkronicle.advancedchatcore.chat.ChatHistory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardHandler.class)
public class MixinKeyboard {

    // 26.2: Yarn Keyboard.processF3(int) is gone; the F3+D "clear chat" path now lives in
    // KeyboardHandler.handleDebugKeys(KeyEvent), which calls
    // ChatComponent.clearMessages(Z) (was ChatHud.clear(Z)).
    @Inject(
            method = "handleDebugKeys",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"))
    public void processF3Chat(KeyEvent key, CallbackInfoReturnable<Boolean> ci) {
        // Make it so that history can still be cleared
        ChatHistory.getInstance().clearAll();
    }
}
