/*
 * Copyright (C) 2026 flyingfinger1
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatcore.AdvancedChatCore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps config option display names and tooltips in the right language.
 *
 * <p>MaLiLib config options store the display name and comment that are passed to their constructor,
 * and the AdvancedChat modules pass already-translated strings. Those strings are captured when the
 * config class is first loaded — which, for modules referenced early during start-up (e.g. via a
 * mixin), can happen before the selected language's resources are loaded, freezing in the English
 * (or fallback) text even when the game runs in another language.
 *
 * <p>To fix this without restructuring every config, modules register a re-translator here, and the
 * config screen calls {@link #retranslateAll()} every time it opens. By then the language is loaded,
 * so names and tooltips come out correct — and it also follows an in-game language change.
 */
@Environment(EnvType.CLIENT)
public final class ConfigLang {

    private static final List<Runnable> RETRANSLATORS = new ArrayList<>();

    private ConfigLang() {}

    /** Registers a re-translator run whenever the config screen opens. */
    public static void registerRetranslator(Runnable retranslator) {
        RETRANSLATORS.add(retranslator);
    }

    /** Runs all registered re-translators. Called by the config screen on open. */
    public static void retranslateAll() {
        for (Runnable r : RETRANSLATORS) {
            try {
                r.run();
            } catch (Exception e) {
                AdvancedChatCore.LOGGER.warn("A config re-translator failed", e);
            }
        }
    }

    /**
     * Re-applies the current translations to a list of options. {@code prefix} is the translation-key
     * prefix the options were built with (e.g. {@code "advancedchat.config.general."}); the name key
     * is {@code prefix + key} and the tooltip key is {@code prefix + "info." + key}. Missing keys fall
     * back to the option's current text, so nothing ever shows a raw translation key.
     */
    public static void retranslate(List<SaveableConfig<? extends IConfigBase>> options, String prefix) {
        for (SaveableConfig<? extends IConfigBase> sc : options) {
            if (sc.config instanceof ConfigBase<?> cb) {
                cb.setPrettyName(StringUtils.getTranslatedOrFallback(prefix + sc.key, cb.getName()));
                cb.setComment(StringUtils.getTranslatedOrFallback(prefix + "info." + sc.key, cb.getComment()));
            }
        }
    }
}
