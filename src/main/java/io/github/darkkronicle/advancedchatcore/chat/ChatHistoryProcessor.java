/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.chat;

import io.github.darkkronicle.advancedchatcore.AdvancedChatCore;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import io.github.darkkronicle.advancedchatcore.interfaces.IMessageProcessor;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.SearchUtils;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChatHistoryProcessor implements IMessageProcessor {

    /**
     * True while a processed message is being fed back into the vanilla chat HUD. MixinChatHud
     * checks this to avoid intercepting (and re-dispatching) our own re-add, which would otherwise
     * recurse infinitely through addPlayerMessage.
     */
    public static boolean FORWARDING_TO_HUD = false;

    private static boolean sendToHud(Component text, @Nullable MessageSignature signature, GuiMessageTag indicator) {
        if (AdvancedChatCore.FORWARD_TO_HUD) {
            // addPlayerMessage is public in 26.x, but it is exactly what MixinChatHud intercepts, so
            // guard against re-entry and let vanilla render this (already processed) message.
            FORWARDING_TO_HUD = true;
            try {
                Minecraft.getInstance().gui.getChat().addPlayerMessage(text, signature, indicator);
            } finally {
                FORWARDING_TO_HUD = false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean process(Component text, @Nullable Component unfiltered) {
        return process(text, unfiltered, null, GuiMessageTag.system());
    }

    @Override
    public boolean process(Component text, @Nullable Component unfiltered, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator) {
        if (unfiltered == null) {
            unfiltered = text;
        }

        // Put the time in
        LocalTime time = LocalTime.now();
        boolean showtime = ConfigStorage.General.SHOW_TIME.config.getBooleanValue();
        // Store original so we can get stuff without the time
        Component original = text.copy();
        if (showtime) {
            DateTimeFormatter format =
                    DateTimeFormatter.ofPattern(
                            ConfigStorage.General.TIME_FORMAT.config.getStringValue());
            String replaceFormat =
                    ConfigStorage.General.TIME_TEXT_FORMAT.config.getStringValue().replaceAll("&", "§");
            Color color = ConfigStorage.General.TIME_COLOR.config.get();
            Style style = Style.EMPTY;
            TextColor textColor = TextColor.fromRgb(color.color());
            style = style.withColor(textColor);
            text.getSiblings().add(0, Component.literal(replaceFormat.replaceAll("%TIME%", time.format(format))).withStyle(style));
        }

        int width = 0;
        // Find player
        MessageOwner player =
                SearchUtils.getAuthor(
                        Minecraft.getInstance().getConnection(), unfiltered.getString());
        ChatMessage line = ChatMessage.builder()
                .displayText(text)
                .originalText(original)
                .owner(player)
                .id(0)
                .width(width)
                .creationTick(Minecraft.getInstance().gui.getGuiTicks())
                .time(time)
                .backgroundColor(null)
                .build();
        if (ChatHistory.getInstance().add(line)) {
            sendToHud(line.getDisplayText(), line.getSignature(), line.getIndicator());
        }
        return true;
    }
}
