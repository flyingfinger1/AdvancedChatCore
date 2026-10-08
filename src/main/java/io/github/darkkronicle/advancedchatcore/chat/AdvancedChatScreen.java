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
import io.github.darkkronicle.advancedchatcore.config.gui.GuiConfigHandler;
import io.github.darkkronicle.advancedchatcore.gui.CleanButton;
import io.github.darkkronicle.advancedchatcore.gui.IconButton;
import io.github.darkkronicle.advancedchatcore.interfaces.AdvancedChatScreenSection;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.RowList;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Migrated from GuiBase to Screen for modern Fabric/Minecraft versions.
 * This keeps the original logic but uses Screen APIs.
 */
public class AdvancedChatScreen extends Screen {

    public static boolean PERMANENT_FOCUS = false;

    private String finalHistory = "";
    private int messageHistorySize = -1;
    private int startHistory = -1;
    private boolean passEvents = false;

    /** Chat field at the bottom of the screen */
    @Getter protected AdvancedTextField chatField;

    /** What the chat box started out with */
    @Getter private String originalChatText = "";

    private static String last = "";
    private final List<AdvancedChatScreenSection> sections = new ArrayList<>();

    @Getter
    private final RowList<CleanButton> rightSideButtons = new RowList<>();

    @Getter
    private final RowList<CleanButton> leftSideButtons = new RowList<>();

    public AdvancedChatScreen() {
        // Title required by Screen constructor
        super(Component.literal("Advanced Chat"));
        setupSections();
    }

    public AdvancedChatScreen(boolean passEvents) {
        this();
        this.passEvents = passEvents;
    }

    public AdvancedChatScreen(int indexOfLast) {
        this();
        startHistory = indexOfLast;
    }

    public AdvancedChatScreen(String originalChatText) {
        this();
        this.originalChatText = originalChatText;
    }

    private void setupSections() {
        for (Function<AdvancedChatScreen, AdvancedChatScreenSection> supplier : ChatScreenSectionHolder.getInstance().getSectionSuppliers()) {
            AdvancedChatScreenSection section = supplier.apply(this);
            if (section != null) {
                sections.add(section);
            }
        }
    }

    private Color getColor() {
        return ConfigStorage.ChatScreen.COLOR.config.get();
    }

    public void resetCurrentMessage() {
        this.messageHistorySize = this.minecraft.gui.getChat().getRecentChat().size();
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (passEvents) {
            return true;
        }
        return super.charTyped(characterEvent);
    }

    /**
     * init is called by Minecraft when the screen is (re)created.
     */
    @Override
    protected void init() {
        this.rightSideButtons.clear();
        this.leftSideButtons.clear();
        resetCurrentMessage();

        // Create chat field. AdvancedTextField should be compatible with EditBox.
        this.chatField =
                new AdvancedTextField(
                        this.font,
                        4,
                        this.height - 12,
                        this.width - 10,
                        12,
                        Component.translatable("chat.editBox")) {
                    protected MutableComponent getNarrationMessage() {
                        return null;
                    }
                };

        if (ConfigStorage.ChatScreen.MORE_TEXT.config.getBooleanValue()) {
            this.chatField.setMaxLength(64000);
        } else {
            this.chatField.setMaxLength(256);
        }
        this.chatField.setBordered(false);

        if (!this.originalChatText.equals("")) {
            this.chatField.setText(this.originalChatText);
        } else if (ConfigStorage.ChatScreen.PERSISTENT_TEXT.config.getBooleanValue()
                && !last.equals("")) {
            this.chatField.setText(last);
        }
        this.chatField.setResponder(this::onChatFieldUpdate);

        // Add settings button. IconButton is a MaLiLib widget (not a vanilla Button), so it is
        // laid out, rendered and click-dispatched manually below rather than via
        // addRenderableWidget (which requires a vanilla Renderable).
        IconButton settingsBtn = new IconButton(0, 0, 14, 64, Identifier.fromNamespaceAndPath(AdvancedChatCore.MOD_ID, "textures/gui/settings.png"), (button) -> {
            Minecraft.getInstance().setScreen(GuiConfigHandler.getInstance().getDefaultScreen());
        });
        rightSideButtons.add("settings", settingsBtn);

        // Add the chat field as a renderable widget so it both receives focus/keyboard events
        // AND is drawn via its extractWidgetRenderState in the 26.x render-state model.
        this.addRenderableWidget(this.chatField);
        this.setFocused(this.chatField);
        this.chatField.setFocused(true);

        for (AdvancedChatScreenSection section : sections) {
            section.initGui();
        }

        // Layout right side buttons
        int originalX = this.minecraft.getWindow().getGuiScaledWidth() - 1;
        int y = this.minecraft.getWindow().getGuiScaledHeight() - 30;
        for (int i = 0; i < rightSideButtons.rowSize(); i++) {
            List<CleanButton> buttonList = rightSideButtons.get(i);
            int maxHeight = 0;
            int x = originalX;
            for (CleanButton button : buttonList) {
                maxHeight = Math.max(maxHeight, button.getHeight());
                x -= button.getWidth() + 1;
                button.setPosition(x, y);
            }
            y -= maxHeight + 1;
        }

        // Layout left side buttons
        originalX = 1;
        y = this.minecraft.getWindow().getGuiScaledHeight() - 30;
        for (int i = 0; i < leftSideButtons.rowSize(); i++) {
            List<CleanButton> buttonList = leftSideButtons.get(i);
            int maxHeight = 0;
            int x = originalX;
            for (CleanButton button : buttonList) {
                maxHeight = Math.max(maxHeight, button.getHeight());
                button.setPosition(x, y);
                x += button.getWidth() + 1;
            }
            y -= maxHeight + 1;
        }

        if (startHistory >= 0) {
            setChatFromHistory(-startHistory - 1);
        }
    }

    @Override
    public void removed() {
        for (AdvancedChatScreenSection section : sections) {
            section.removed();
        }
    }

    @Override
    public void tick() {
        if (this.chatField != null) {
            this.chatField.tick();
        }
    }

    private void onChatFieldUpdate(String chatText) {
        String string = this.chatField.getValue();
        for (AdvancedChatScreenSection section : sections) {
            section.onChatFieldUpdate(chatText, string);
        }
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        if (passEvents) {
            InputConstants.Key key = InputConstants.getKey(keyEvent);
            KeyMapping.set(key, false);
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        int scanCode = keyEvent.scancode();
        int modifiers = keyEvent.modifiers();
        if (!passEvents) {
            for (AdvancedChatScreenSection section : sections) {
                if (section.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
            if (super.keyPressed(keyEvent)) {
                return true;
            }
        }

        // Map legacy KeyCodes to GLFW codes
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            // Exit out
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            String string = this.chatField.getValue().trim();
            // Strip message and send
            MessageSender.getInstance().sendMessage(string);
            this.chatField.setText("");
            last = "";
            // Exit
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            // Go through previous history
            this.setChatFromHistory(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            // Go through previous history
            this.setChatFromHistory(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            // Scroll
            this.minecraft.gui.getChat()
                    .scrollChat(this.minecraft.gui.getChat().getLinesPerPage() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            // Scroll
            this.minecraft.gui.getChat()
                    .scrollChat(-this.minecraft.gui.getChat().getLinesPerPage() + 1);
            return true;
        }
        if (passEvents) {
            this.chatField.setText("");
            InputConstants.Key key = InputConstants.getKey(keyEvent);
            KeyMapping.set(key, true);
            KeyMapping.click(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double amount = verticalAmount;
        if (amount > 1.0D) {
            amount = 1.0D;
        }
        if (amount < -1.0D) {
            amount = -1.0D;
        }

        for (AdvancedChatScreenSection section : sections) {
            if (section.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        // 26.2: static Screen.hasShiftDown() is gone; query the window directly.
        boolean shift = InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (!shift) {
            amount *= 7.0D;
        }

        // Send to hud to scroll
        this.minecraft.gui.getChat().scrollChat((int) amount);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int button = mouseButtonEvent.button();
        for (AdvancedChatScreenSection section : sections) {
            if (section.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        // Dispatch clicks to the manually-managed MaLiLib icon buttons.
        for (int i = 0; i < rightSideButtons.rowSize(); i++) {
            for (CleanButton b : rightSideButtons.get(i)) {
                if (b.isMouseOver((int) mouseX, (int) mouseY)
                        && b.onMouseClicked(mouseButtonEvent, doubleClick)) {
                    return true;
                }
            }
        }
        for (int i = 0; i < leftSideButtons.rowSize(); i++) {
            for (CleanButton b : leftSideButtons.get(i)) {
                if (b.isMouseOver((int) mouseX, (int) mouseY)
                        && b.onMouseClicked(mouseButtonEvent, doubleClick)) {
                    return true;
                }
            }
        }
        // Clickable chat text (links, run/suggest command, copy-to-clipboard, shift-click insert)
        // via the 26.x ActiveTextCollector model that replaced ChatComponent.getTextStyleAt.
        if (button == 0) {
            boolean insertionMode =
                    InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                            || InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            int scaledHeight = this.minecraft.getWindow().getGuiScaledHeight();
            ActiveTextCollector.ClickableStyleFinder finder =
                    new ActiveTextCollector.ClickableStyleFinder(this.font, (int) mouseX, (int) mouseY)
                            .includeInsertions(insertionMode);
            this.minecraft.gui.getChat().captureClickableText(
                    finder, scaledHeight, this.minecraft.gui.getGuiTicks(),
                    ChatComponent.DisplayMode.FOREGROUND);
            Style style = finder.result();
            if (style != null) {
                if (insertionMode && style.getInsertion() != null) {
                    this.chatField.insertText(style.getInsertion());
                    return true;
                }
                var clickEvent = style.getClickEvent();
                if (clickEvent != null) {
                    defaultHandleClickEvent(clickEvent, this.minecraft, this);
                    return true;
                }
            }
        }

        return (this.chatField.mouseClicked(mouseButtonEvent, doubleClick)
                || super.mouseClicked(mouseButtonEvent, doubleClick));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int mouseButton = mouseButtonEvent.button();
        for (AdvancedChatScreenSection section : sections) {
            if (section.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int button = mouseButtonEvent.button();
        for (AdvancedChatScreenSection section : sections) {
            if (section.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
    }

    public void insertText(String text, boolean override) {
        if (override) {
            this.chatField.setText(text);
        } else {
            this.chatField.insertText(text);
        }
    }

    public void setChatFromHistory(int i) {
        int targetIndex = this.messageHistorySize + i;
        int maxIndex = this.minecraft.gui.getChat().getRecentChat().size();
        targetIndex = Mth.clamp(targetIndex, 0, maxIndex);
        if (targetIndex != this.messageHistorySize) {
            if (targetIndex == maxIndex) {
                this.messageHistorySize = maxIndex;
                this.chatField.setText(this.finalHistory);
            } else {
                if (this.messageHistorySize == maxIndex) {
                    this.finalHistory = this.chatField.getValue();
                }

                String hist = this.minecraft.gui.getChat().getRecentChat().get(targetIndex);
                this.chatField.setText(hist);
                for (AdvancedChatScreenSection section : sections) {
                    section.setChatFromHistory(hist);
                }
                this.messageHistorySize = targetIndex;
            }
        }
    }

    /**
     * 26.2: the imperative {@code render(PoseStack, ...)} path is gone. Custom drawing happens
     * during render-state extraction. The chat field and registered buttons added via
     * {@code addSelectableChild}/{@code addDrawableChild} are extracted by the super call.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.setFocused(this.chatField);
        this.chatField.setFocused(true);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        // Render the chat history. In 26.x the in-game HUD (and thus the chat) is not drawn behind
        // an open Screen, so — like vanilla ChatScreen — we render the ChatComponent ourselves in
        // FOREGROUND mode, otherwise the history would be invisible while typing.
        ChatComponent chat = this.minecraft.gui.getChat();
        guiGraphics.nextStratum();
        chat.extractRenderState(guiGraphics, this.font, this.minecraft.gui.getGuiTicks(),
                mouseX, mouseY, ChatComponent.DisplayMode.FOREGROUND, false);

        // Render the manually-managed MaLiLib icon buttons through the MaLiLib GuiContext.
        fi.dy.masa.malilib.render.GuiContext ctx = fi.dy.masa.malilib.render.GuiContext.fromGuiGraphics(guiGraphics);
        for (int i = 0; i < rightSideButtons.rowSize(); i++) {
            for (CleanButton b : rightSideButtons.get(i)) {
                b.render(ctx, mouseX, mouseY, false);
            }
        }
        for (int i = 0; i < leftSideButtons.rowSize(); i++) {
            for (CleanButton b : leftSideButtons.get(i)) {
                b.render(ctx, mouseX, mouseY, false);
            }
        }

        // Let module-provided sections render through the extraction model.
        for (AdvancedChatScreenSection section : sections) {
            section.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Hover tooltips on chat text (links, player entities, ...) are rendered by vanilla's
        // chat.extractRenderState above, so nothing extra is needed here.
    }

    /**
     * The chat overlay must stay transparent — it must not blur or darken the world behind it.
     * 26.1's default {@code Screen.extractBackground} extracts a blurred in-world menu background
     * (which would render over the chat window); vanilla {@code ChatScreen} suppresses that by
     * overriding this to extract nothing, so mirror it. (On 26.2 the default is already transparent
     * in this context, so this is harmless there too.)
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: no blur, no menu background.
    }

    /**
     * The chat is an in-game overlay, not a menu — it must NOT pause the game. {@code Screen}'s default
     * {@code isPauseScreen()} returns true, which makes the client treat this as a pause screen: it
     * pauses singleplayer and runs the pause-screen render path, which flashes one black frame when the
     * screen opens. Vanilla {@code ChatScreen} overrides this to false; mirror it.
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (ConfigStorage.ChatScreen.PERSISTENT_TEXT.config.getBooleanValue()) {
            last = (this.chatField != null) ? this.chatField.getValue() : "";
        }
        super.onClose();
    }

    /**
     * Keeps the chat screen open while the player is inside a portal. Replaces the old
     * MixinClientPlayerEntity hack: 26.x added this official Screen hook, so a mixin that
     * cancels the portal-triggered screen close is no longer needed.
     */
    @Override
    public boolean isAllowedInPortal() {
        return true;
    }

    private void setText(String text) {
        if (this.chatField != null) {
            this.chatField.setText(text);
        }
    }
}
