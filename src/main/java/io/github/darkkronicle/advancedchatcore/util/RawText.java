package io.github.darkkronicle.advancedchatcore.util;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.List;

public record RawText(String content, Style style) implements Component {

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public ComponentContents getContents() {
        return PlainTextContents.create(content);
    }

    @Override
    public String getString() {
        return content;
    }

    @Override
    public List<Component> getSiblings() {
        return new ArrayList<>();
    }

    @Override
    public FormattedCharSequence getVisualOrderText() {
        Language language = Language.getInstance();
        return language.getVisualOrder(this);
    }

    public RawText withString(String string) {
        return RawText.of(string, style);
    }

    public static RawText of(String string) {
        return RawText.of(string, Style.EMPTY);
    }

    public static RawText of(String string, Style style) {
        return new RawText(string, style);
    }
}