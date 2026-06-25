/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.config.gui.widgets;

import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatcore.util.Color;
import io.github.darkkronicle.advancedchatcore.util.Colors;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class WidgetColor extends GuiTextFieldGeneric {

    private int colorX;
    private Color currentColor;

    public WidgetColor(
            int x, int y, int width, int height, Color color, Font textRenderer) {
        super(x, y, width - 22, height, textRenderer);
        this.colorX = x + width - 20;
        this.currentColor = color;
        setValue(String.format("#%08X", this.currentColor.color()));
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTicks) {
        super.extractWidgetRenderState(gg, mouseX, mouseY, partialTicks);
        GuiContext ctx = GuiContext.fromGuiGraphics(gg);
        int y = this.y;
        RenderUtils.drawRect(ctx, this.colorX, y, 19, 19, 0xFFFFFFFF);
        RenderUtils.drawRect(ctx, this.colorX + 1, y + 1, 17, 17, 0xFF000000);
        RenderUtils.drawRect(ctx, this.colorX + 2, y + 2, 15, 15, this.currentColor.color());
    }

    @Override
    public void insertText(String text) {
        super.insertText(text);
        getAndRefreshColor4f();
    }

    @Override
    public int getWidth() {
        return super.getWidth() + 22;
    }

    public Color getAndRefreshColor4f() {
        Optional<Color> color = Colors.getInstance().getColor(getValue());
        if (color.isPresent()) {
            this.currentColor = color.get();
            return this.currentColor;
        }
        this.currentColor = new Color(StringUtils.getColor(getValue(), 0));
        return this.currentColor;
    }
}
