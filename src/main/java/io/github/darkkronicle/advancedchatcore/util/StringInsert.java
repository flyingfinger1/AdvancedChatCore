package io.github.darkkronicle.advancedchatcore.util;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

/**
 * An interface to provide a way to get the text that should be replaced based off of the
 * current {@link Component} and the current {@link StringMatch}
 */
public interface StringInsert {
    /**
     * Return's the {@link MutableComponent} that should be inserted.
     *
     * @param current The current {@link Component}
     * @param match The current {@link StringMatch}
     * @return
     */
    MutableComponent getText(Component current, StringMatch match);
}