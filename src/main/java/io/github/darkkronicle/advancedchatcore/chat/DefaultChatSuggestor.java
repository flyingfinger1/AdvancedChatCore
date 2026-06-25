/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.chat;

import io.github.darkkronicle.advancedchatcore.interfaces.AdvancedChatScreenSection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/** Handles the CommandSuggestor for the chat */
@Environment(EnvType.CLIENT)
public class DefaultChatSuggestor extends AdvancedChatScreenSection {

    private CommandSuggestions commandSuggestor;

    public DefaultChatSuggestor(AdvancedChatScreen screen) {
        super(screen);
    }

    @Override
    public void onChatFieldUpdate(String chatText, String text) {
        // 26.2: the old setWindowActive maps to setAllowSuggestions (which enables the suggestion
        // LIST popup), not showSuggestions (which only commits the current ones). This mirrors
        // vanilla ChatScreen#onEdited.
        this.commandSuggestor.setAllowSuggestions(!text.equals(getScreen().getOriginalChatText()));
        this.commandSuggestor.updateCommandInfo();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 26.2: CommandSuggestions#keyPressed now takes a KeyEvent record.
        return this.commandSuggestor.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 26.2: render(PoseStack,...) -> extractRenderState(GuiGraphicsExtractor, mouseX, mouseY).
        this.commandSuggestor.extractRenderState(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void setChatFromHistory(String hist) {
        this.commandSuggestor.setAllowSuggestions(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.commandSuggestor.mouseScrolled(amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 26.2: CommandSuggestions#mouseClicked now takes a MouseButtonEvent record.
        return this.commandSuggestor.mouseClicked(
                new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
    }

    @Override
    public void resize(int width, int height) {
        this.commandSuggestor.updateCommandInfo();
    }

    @Override
    public void initGui() {
        Minecraft client = Minecraft.getInstance();
        AdvancedChatScreen screen = getScreen();
        this.commandSuggestor =
                new CommandSuggestions(
                        client,
                        screen,
                        screen.chatField,
                        client.font,
                        false,
                        false,
                        1,
                        10,
                        true,
                        -805306368);
        this.commandSuggestor.updateCommandInfo();
    }
}
