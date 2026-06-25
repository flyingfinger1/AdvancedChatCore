/*
 * Copyright (C) 2021-2022 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.interfaces;

import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** An interface for taking text and processing it. */
public interface IMessageProcessor extends IMessageFilter {
    /**
     * Processes text without the unfiltered text.
     *
     * <p>Deprecated because it won't return anything. If unfiltered doesn't exist, insert null into
     * process.
     *
     * @param text Component to modify
     * @return Empty
     */
    @Deprecated
    @Override
    default Optional<Component> filter(Component text) {
        process(text, null);
        return Optional.empty();
    }

    /**
     * Consumes text.
     *
     * @param text Final text to process
     * @param unfiltered Original text (if available)
     * @return If the processing was a success
     */
    boolean process(Component text, @Nullable Component unfiltered);

    default boolean process(Component text, @Nullable Component unfilterered, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator) {
        return process(text, unfilterered);
    }
}
