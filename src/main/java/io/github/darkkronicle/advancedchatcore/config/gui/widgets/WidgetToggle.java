/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.config.gui.widgets;

import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import net.minecraft.client.input.MouseButtonEvent;
import lombok.Getter;

public class WidgetToggle extends ButtonOnOff {

    @Getter private boolean currentlyOn;

    public WidgetToggle(
            int x,
            int y,
            int width,
            boolean rightAlign,
            String translationKey,
            boolean isCurrentlyOn,
            String... hoverStrings) {
        super(x, y, width, rightAlign, translationKey, isCurrentlyOn, hoverStrings);
        this.currentlyOn = isCurrentlyOn;
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        this.currentlyOn = !this.currentlyOn;
        this.updateDisplayString(this.currentlyOn);
        return super.onMouseClickedImpl(mouseButtonEvent, doubleClick);
    }

    public void setOn(boolean on) {
        this.currentlyOn = on;
        this.updateDisplayString(on);
    }
}
