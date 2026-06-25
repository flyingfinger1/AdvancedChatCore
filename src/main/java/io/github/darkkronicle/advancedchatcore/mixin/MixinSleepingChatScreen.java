/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.mixin;

import io.github.darkkronicle.advancedchatcore.chat.AdvancedChatScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(InBedChatScreen.class)
public class MixinSleepingChatScreen extends ChatScreen {

    public MixinSleepingChatScreen() {
        // 26.2: ChatScreen no longer has a (String) constructor; the simplest is (String, boolean).
        super("", false);
    }

    // 26.2: Yarn SleepingChatScreen.closeChatIfEmpty() is now InBedChatScreen.onPlayerWokeUp().
    // That method has two Gui.setScreen(Screen) calls: ordinal 0 = setScreen(null) (empty input),
    // ordinal 1 = setScreen(new ChatScreen(input, false)) (non-empty input). We swap the ordinal-1
    // argument for our AdvancedChatScreen, matching the original intent. The setScreen call is now on
    // net.minecraft.client.gui.Gui (was MinecraftClient).
    @ModifyArg(method = "onPlayerWokeUp",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 1))
    public Screen openSleepingChatScreen(@Nullable Screen screen) {
        // 26.2: Yarn chatField (TextFieldWidget) is now the inherited `input` (EditBox);
        // getText() -> getValue().
        return new AdvancedChatScreen(this.input.getValue());
    }
}
