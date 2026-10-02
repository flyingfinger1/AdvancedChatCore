/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.chat;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.input.KeyCodes;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public class AdvancedSleepingChatScreen extends AdvancedChatScreen {

    public AdvancedSleepingChatScreen() {
        super("");
    }

    @Override
    protected void init() {
        super.init();
        // A vanilla Button mounts fine on the Screen render path (unlike the old MaLiLib
        // ButtonGeneric), so restore the on-screen "Stop sleeping" button, matching vanilla
        // InBedChatScreen's placement.
        this.addRenderableWidget(
                Button.builder(Component.translatable("multiplayer.stopSleeping"), btn -> this.stopSleeping())
                        .bounds(this.width / 2 - 100, this.height - 40, 200, 20)
                        .build());
    }

    @Override
    public void onClose() {
        this.stopSleeping();
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        if (keyCode == KeyCodes.KEY_ESCAPE) {
            this.stopSleeping();
        } else if (keyCode == KeyCodes.KEY_RETURN || keyCode == KeyCodes.KEY_KP_ENTER) {
            String string = this.chatField.getValue().trim();
            if (!string.isEmpty()) {
                MessageSender.getInstance().sendMessage(string);
            }

            this.chatField.setText("");
            this.minecraft.gui.hud.getChat().resetChatScroll();
            // Prevents really weird interactions with chat history
            resetCurrentMessage();
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    private void stopSleeping() {
        ClientPacketListener clientPlayNetworkHandler = this.minecraft.player.connection;
        // 26.2: STOP_SLEEPING moved from ServerboundClientCommandPacket to
        // ServerboundPlayerCommandPacket(Entity, Action.STOP_SLEEPING).
        clientPlayNetworkHandler.send(
                new ServerboundPlayerCommandPacket(
                        this.minecraft.player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
        GuiBase.openGui(null);
    }
}
