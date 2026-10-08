/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.chat;

import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import io.github.darkkronicle.advancedchatcore.util.StringMatch;
import io.github.darkkronicle.advancedchatcore.util.StyleFormatter;
import io.github.darkkronicle.advancedchatcore.util.TextBuilder;
import io.github.darkkronicle.advancedchatcore.util.TextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class AdvancedTextField extends EditBox {

    private final static int MAX_HISTORY = 50;

    /** Semi-transparent blue used to highlight selected text (replaces the old GL XOR fill). */
    private final static int SELECTION_COLOR = 0x80_0000FF;

    /**
     * Stores the last saved snapshot of the box. This ensures that not every character update is
     * put in, but instead groups.
     */
    private String lastSaved = "";

    /** Snapshots of chat box */
    private final List<String> history = new ArrayList<>();

    private int focusedTicks = 0;
    private List<Component> renderLines = new ArrayList<>();
    private Font textRenderer;
    private String suggestion = null;
    private int maxLength = 32;
    private int selectionEnd;
    private int selectionStart;
    // TODO Split?
    private BiFunction<String, Integer, FormattedCharSequence> renderTextProvider = (string, firstCharacterIndex) -> FormattedCharSequence.forward(string, Style.EMPTY);

    private int historyIndex = -1;

    public AdvancedTextField(Font textRenderer, int x, int y, int width, int height, Component text) {
        this(textRenderer, x, y, width, height, null, text);
    }

    public AdvancedTextField(
            Font textRenderer,
            int x,
            int y,
            int width,
            int height,
            @Nullable EditBox copyFrom,
            Component text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
        history.add("");
        this.textRenderer = textRenderer;
        updateRender();
    }

    /**
     * 26.2: EditBox no longer declares {@code tick()}, so this is no longer an override. It is
     * still driven manually from {@link AdvancedChatScreen#tick()}.
     */
    public void tick() {
        focusedTicks++;
    }

    /**
     * 26.2: {@code setRenderTextProvider} no longer exists on EditBox (the engine uses
     * {@code addFormatter(EditBox.TextFormatter)} instead). The provider is kept purely
     * internally so {@link #updateRender()} can style the wrapped lines.
     */
    public void setRenderTextProvider(BiFunction<String, Integer, FormattedCharSequence> renderTextProvider) {
        this.renderTextProvider = renderTextProvider;
    }

    @Override
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        super.setMaxLength(maxLength);
    }

    public static boolean isUndo(KeyEvent keyEvent) {
        // Undo (Ctrl + Z). 26.2: modifier helpers moved from static Screen.* onto the
        // InputWithModifiers event (KeyEvent#hasControlDown / #hasAltDown).
        return keyEvent.input() == InputConstants.KEY_Z && keyEvent.hasControlDown() && !keyEvent.hasAltDown();
    }

    /** Triggers undo for the text box */
    public void undo() {
        // Save the current snapshot if it's been edited
        if (!this.lastSaved.equals(this.getValue()) && historyIndex < 0) {
            addToHistory(getValue());
        }
        // History index < 0 means not in the middle of undoing
        if (historyIndex < 0) {
            historyIndex = history.size() - 1;
        }
        // Check that we're not at index 0
        if (historyIndex != 0) {
            historyIndex--;
        }
        // Set the text but don't update
        setText(history.get(historyIndex), false);
    }

    public void redo() {
        if (historyIndex < 0 || historyIndex >= history.size() - 1) {
            // No stuff to redo...
            return;
        }
        historyIndex++;
        setText(history.get(historyIndex), false);
    }

    @Override
    public void insertText(String text) {
        super.insertText(text);
        updateHistory();
        updateRender();
    }

    // Override the common deletion funnel, not deleteChars: EditBox routes BOTH deleteChars (Backspace)
    // and deleteWords (Ctrl+Backspace) — plus selection deletes — through deleteCharsToPos. Hooking only
    // deleteChars missed Ctrl+Backspace, so the value changed but renderLines (and thus the drawn text)
    // was not rebuilt until the next keystroke.
    @Override
    public void deleteCharsToPos(int position) {
        super.deleteCharsToPos(position);
        updateHistory();
        updateRender();
    }

    @Override
    public void setSuggestion(@Nullable String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int renderY = getY() - (renderLines.size() - 1) * (textRenderer.lineHeight + 2);
        if (mouseY < renderY - 2 || mouseY > getY() + height + 2 || mouseX < getX() - 2 || mouseX > getX() + width + 4) {
            return false;
        }
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
        GuiContext ctx = GuiContext.fromGuiGraphics(gg);
        // 26.x text colors are ARGB; without the alpha byte the text renders fully transparent
        // (leaving only the shadow, which looks like near-black text).
        int color = 0xFFE0E0E0;
        int cursor = getCursorPosition();
        int cursorRow = renderLines.size() - 1;
        boolean renderCursor = this.isFocused() && focusedTicks / 6 % 2 == 0;
        int renderY = getY() - (renderLines.size() - 1) * (textRenderer.lineHeight + 2);
        int endX = 0;
        int charCount = 0;
        int cursorX = -1;
        boolean selection = selectionStart != selectionEnd;
        boolean started = false;
        boolean ended = false;
        int selStart;
        int selEnd;
        if (this.selectionStart < this.selectionEnd) {
            selStart = this.selectionStart;
            selEnd = this.selectionEnd;
        } else {
            selStart = this.selectionEnd;
            selEnd = this.selectionStart;
        }
        int x = getX();
        int y = getY();
        // Background fill
        RenderUtils.drawRect(ctx, getX() - 2, renderY - 2, width + 6, (getY() + height + 4) - (renderY - 2),
                ConfigStorage.ChatScreen.COLOR.config.get().color());
        for (int line = 0; line < renderLines.size(); line++) {
            Component text = renderLines.get(line);
            if (cursor >= charCount && cursor < text.getString().length() + charCount) {
                cursorX = textRenderer.width(text.getString().substring(0, cursor - charCount));
                cursorRow = line;
            }
            // Draw this wrapped line (with shadow) and remember where it ended
            ctx.drawString(textRenderer, text, x, renderY, color, true);
            endX = x + textRenderer.width(text);
            if (selection) {
                if (!started && selStart >= charCount && selStart <= text.getString().length() + charCount) {
                    started = true;
                    int startX = textRenderer.width(TextUtil.truncate(text, new StringMatch("", 0, selStart - charCount)));
                    if (selEnd > charCount && selEnd <= text.getString().length() + charCount) {
                        ended = true;
                        int sEndX = textRenderer.width(TextUtil.truncate(text, new StringMatch("", 0, selEnd - charCount)));
                        drawSelectionHighlight(ctx, x + startX, renderY - 1, x + sEndX, renderY + textRenderer.lineHeight);
                    } else {
                        int sEndX = textRenderer.width(text);
                        drawSelectionHighlight(ctx, x + startX, renderY - 1, x + sEndX, renderY + textRenderer.lineHeight);
                    }
                } else if (started && !ended) {
                    if (selEnd >= charCount && selEnd <= text.getString().length() + charCount) {
                        ended = true;
                        int sEndX = textRenderer.width(TextUtil.truncate(text, new StringMatch("", 0, selEnd - charCount)));
                        drawSelectionHighlight(ctx, x, renderY - 1, x + sEndX, renderY + textRenderer.lineHeight);
                    } else {
                        int sEndX = textRenderer.width(text);
                        drawSelectionHighlight(ctx, x, renderY - 1, x + sEndX, renderY + textRenderer.lineHeight);
                    }
                }
            }
            renderY += textRenderer.lineHeight + 2;
            charCount += text.getString().length();
        }
        if (cursorX < 0) {
            cursorX = endX;
        }
        boolean cursorAtEnd = getCursorPosition() == getValue().length();
        if (!cursorAtEnd && this.suggestion != null) {
            ctx.drawString(this.textRenderer, this.suggestion, endX - 1, y, -8355712, true);
        }
        if (renderCursor) {
            int cursorY = y - (renderLines.size() - 1 - cursorRow) * (textRenderer.lineHeight + 2);
            if (cursorAtEnd) {
                RenderUtils.drawRect(ctx, cursorX, cursorY - 1, 1, 2 + this.textRenderer.lineHeight, -3092272);
            } else {
                ctx.drawString(this.textRenderer, "_", x + cursorX, cursorY, color, true);
            }
        }
    }

    /**
     * Draws the selection highlight. 26.2 removed the GL XOR (Tessellator/BufferBuilder/logicOp)
     * path entirely, so this is now a plain semi-transparent fill rect, exactly like vanilla
     * EditBox does today.
     */
    private void drawSelectionHighlight(GuiContext ctx, int x1, int y1, int x2, int y2) {
        int x = getX();
        int i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }
        if (x2 > x + this.width) {
            x2 = x + this.width;
        }
        if (x1 > x + this.width) {
            x1 = x + this.width;
        }
        // x1/y1 are now the larger coordinates; normalize to (left, top, width, height)
        int left = Math.min(x1, x2);
        int top = Math.min(y1, y2);
        int w = Math.abs(x1 - x2);
        int h = Math.abs(y1 - y2);
        RenderUtils.drawRect(ctx, left, top, w, h, SELECTION_COLOR);
    }

    /**
     * 26.2: EditBox no longer exposes {@code setSelectionStart}/{@code setSelectionEnd} to
     * override. The selection is the span between the cursor ({@link #getCursorPosition()}) and
     * the highlight position. We mirror both ends here so the custom multi-line highlight render
     * keeps working.
     */
    @Override
    public void setCursorPosition(int cursor) {
        super.setCursorPosition(cursor);
        this.selectionStart = Mth.clamp(getCursorPosition(), 0, getValue().length());
    }

    @Override
    public void setHighlightPos(int position) {
        super.setHighlightPos(position);
        this.selectionEnd = Mth.clamp(position, 0, getValue().length());
    }

    /**
     * Sets the text for the text field
     *
     * @param text Component to set
     * @param update Updates the history
     */
    public void setText(String text, boolean update) {
        // Wrapper class for setText -> setValue
        super.setValue(text);
        // The EditBox super-constructor calls setValue(...) before this subclass' fields are
        // initialized; skip history/render bookkeeping until we are fully constructed.
        if (renderTextProvider == null || history == null) {
            return;
        }
        if (update) {
            updateHistory();
        }
        updateRender();
    }

    public void setText(String text) {
        setText(text, true);
    }

    @Override
    public void setValue(String text) {
        setText(text, true);
    }

    private void updateRender() {
        FormattedCharSequence formatted = renderTextProvider.apply(getValue(), 0);
        renderLines = StyleFormatter.wrapText(textRenderer, getInnerWidth(), new TextBuilder().append(formatted).build());
    }

    private void updateHistory() {
        if (historyIndex >= 0) {
            // Remove all history after what has gone back
            pruneHistory(historyIndex + 1);
            historyIndex = -1;
        }
        // Check to see if it should log
        int dif = getValue().length() - lastSaved.length();
        double sim = TextUtil.similarity(getValue(), lastSaved);
        if (sim >= .3 && (dif < 5 && dif * -1 < 5) || (sim >= .9)) {
            return;
        }
        addToHistory(getValue());
    }

    private void addToHistory(String text) {
        this.lastSaved = text;
        this.history.add(text);
        while (this.history.size() > MAX_HISTORY) {
            this.history.remove(0);
        }
    }

    /**
     * Remove's all history past a certain index
     *
     * @param index Index to prune from. Non-inclusive.
     */
    private void pruneHistory(int index) {
        if (index == 0) {
            history.clear();
            return;
        }
        while (history.size() > index) {
            history.remove(history.size() - 1);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!this.isActive()) {
            return false;
        }
        if (!isUndo(keyEvent)) {
            return super.keyPressed(keyEvent);
        }
        if (keyEvent.hasShiftDown()) {
            redo();
        } else {
            undo();
        }
        return true;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput builder) {
        // Crashes here because Component is null
    }
}
