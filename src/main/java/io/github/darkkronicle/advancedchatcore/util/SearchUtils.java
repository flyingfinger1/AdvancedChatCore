/*
 * Copyright (C) 2021 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatcore.util;

import io.github.darkkronicle.advancedchatcore.chat.MessageOwner;
import io.github.darkkronicle.advancedchatcore.config.ConfigStorage;
import io.github.darkkronicle.advancedchatcore.interfaces.IFinder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.experimental.UtilityClass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * A class used for helping filters find matches and act on them. Helps with Regular Expressions and
 * means that we don't need this in each class.
 */
@Environment(EnvType.CLIENT)
@UtilityClass
public class SearchUtils {

    /**
     * Method to see if there is a match somewhere with a string with an expression. Is similar to
     * {@link #findMatches(String, String, FindType)} just less expensive since it doesn't need to
     * find every match.
     *
     * @param input String to search.
     * @param toMatch Expression to find.
     * @param type How toMatch should be interpreted.
     * @return If a match is found.
     */
    public boolean isMatch(String input, String toMatch, FindType type) {
        IFinder finder = type.getFinder();
        if (finder == null) {
            return false;
        }
        return finder.isMatch(input, toMatch);
    }

    /**
     * Method to see if there is a match somewhere with a string with an expression. Is similar to
     * {@link #findMatches(String, String, FindType)} just less expensive since it doesn't need to
     * find every match.
     *
     * @param input String to search.
     * @param toMatch Expression to find.
     * @param type How toMatch should be interpreted.
     * @return If a match is found.
     */
    public boolean isMatch(Component input, String toMatch, FindType type) {
        IFinder finder = type.getFinder();
        if (finder == null) {
            return false;
        }
        return finder.isMatch(input, toMatch);
    }

    /**
     * Get's replacements for a string and matches following the format $\<number\>
     *
     * @param groups Matches that are found, will replace
     * @param input Input with group replacements
     * @return String with replaced groups
     */
    public String replaceGroups(List<StringMatch> groups, String input) {
        // Checks to make it so we don't always have to regex
        if (input.length() < 2 || !input.contains("$")) {
            return input;
        }
        Optional<List<StringMatch>> replace = findMatches(input, "\\$[0-9]", FindType.REGEX);
        if (replace.isEmpty()) {
            return input;
        }
        // Ensure sort
        TreeSet<StringMatch> replaceMatches = new TreeSet<>(replace.get());
        int last = 0;
        StringBuilder edited = new StringBuilder();
        for (StringMatch m : replaceMatches) {
            int digit = Integer.parseInt(m.match.substring(1, 2));
            if (digit == 0 || digit > groups.size()) {
                continue;
            }
            edited.append(input, last, m.start).append(groups.get(digit));
            last = m.end;
        }
        if (last != input.length()) {
            edited.append(input.substring(last));
        }
        return edited.toString();
    }

    /**
     * Method to find all matches within a string. Is similar to {@link #isMatch(String, String,
     * FindType)}}. This method just finds every match and returns it.
     *
     * @param input String to search.
     * @param toMatch Expression to find.
     * @param type How toMatch should be interpreted.
     * @return An Optional containing a list of {@link StringMatch}
     */
    public Optional<List<StringMatch>> findMatches(String input, String toMatch, FindType type) {
        IFinder finder = type.getFinder();
        if (finder == null) {
            return Optional.empty();
        }
        Set<StringMatch> matches = new TreeSet<>(finder.getMatches(input, toMatch));
        if (matches.size() != 0) {
            return Optional.of(new ArrayList<>(matches));
        }
        return Optional.empty();
    }

    /**
     * Method to find all matches within a text. Is similar to {@link #isMatch(Component, String,
     * FindType)}}. This method just finds every match and returns it.
     *
     * @param input Component to search.
     * @param toMatch Expression to find.
     * @param type How toMatch should be interpreted.
     * @return An Optional containing a list of {@link StringMatch}
     */
    public Optional<List<StringMatch>> findMatches(Component input, String toMatch, FindType type) {
        IFinder finder = type.getFinder();
        if (finder == null) {
            return Optional.empty();
        }
        Set<StringMatch> matches = new TreeSet<>(finder.getMatches(input, toMatch));
        if (matches.size() != 0) {
            return Optional.of(new ArrayList<>(matches));
        }
        return Optional.empty();
    }

    /**
     * Gets first match found based off of conditions
     *
     * @param input String to search
     * @param toMatch Search content
     * @param type {@link FindType} way to search
     * @return Optional of a {@link StringMatch} if found
     */
    public Optional<StringMatch> getMatch(String input, String toMatch, FindType type) {
        IFinder finder = type.getFinder();
        if (finder == null) {
            return Optional.empty();
        }
        // Use treeset to sort the matches
        Set<StringMatch> matches = new TreeSet<>(finder.getMatches(input, toMatch));
        // Add and sort matches
        if (matches.size() != 0) {
            return Optional.of(matches.toArray(new StringMatch[0])[0]);
        }
        return Optional.empty();
    }

    /**
     * Gets first match found based off of conditions
     *
     * @param input String to search
     * @param toMatch Search content
     * @param type {@link FindType} way to search
     * @return Optional of a {@link StringMatch} if found
     */
    public Optional<StringMatch> getMatch(Component input, String toMatch, FindType type) {
        IFinder finder = type.getFinder();
        if (finder == null) {
            return Optional.empty();
        }
        // Use treeset to sort the matches
        Set<StringMatch> matches = new TreeSet<>(finder.getMatches(input, toMatch));
        // Add and sort matches
        if (matches.size() != 0) {
            return Optional.of(matches.toArray(new StringMatch[0])[0]);
        }
        return Optional.empty();
    }

    /**
     * Get the author of a message using regex
     *
     * @param networkHandler Network handler to get player data
     * @param text Component to search
     * @return Owner of the message
     */
    public MessageOwner getAuthor(ClientPacketListener networkHandler, String text) {
        if (networkHandler == null) {
            return null;
        }
        Optional<List<StringMatch>> words =
                SearchUtils.findMatches(
                        stripColorCodes(text),
                        ConfigStorage.General.MESSAGE_OWNER_REGEX.config.getStringValue(),
                        FindType.REGEX);
        if (!words.isPresent()) {
            return null;
        }
        // Start by just checking names and such
        PlayerInfo player = null;
        StringMatch match = null;
        for (StringMatch m : words.get()) {
            if (player != null) {
                break;
            }
            for (PlayerInfo e : networkHandler.getOnlinePlayers()) {
                // Easy mode
                if ((e.getTabListDisplayName() != null
                                && m.match.equals(stripColorCodes(e.getTabListDisplayName().getString())))
                        || m.match.equals(e.getProfile().name())) {
                    player = e;
                    match = m;
                    break;
                }
            }
        }
        // Check for ***everything***
        HashMap<PlayerInfo, List<StringMatch>> entryMatches = new HashMap<>();
        for (PlayerInfo e : networkHandler.getOnlinePlayers()) {
            String name =
                    stripColorCodes(
                            e.getTabListDisplayName() == null
                                    ? e.getProfile().name()
                                    : e.getTabListDisplayName().getString());
            Optional<List<StringMatch>> nameWords =
                    SearchUtils.findMatches(
                            name,
                            ConfigStorage.General.MESSAGE_OWNER_REGEX.config.getStringValue(),
                            FindType.REGEX);
            if (!nameWords.isPresent()) {
                continue;
            }
            entryMatches.put(e, nameWords.get());
        }
        for (StringMatch m : words.get()) {
            for (Map.Entry<PlayerInfo, List<StringMatch>> entry : entryMatches.entrySet()) {
                for (StringMatch nm : entry.getValue()) {
                    if (nm.match.equals(m.match)) {
                        if (player != null && match.start <= m.start) {
                            return new MessageOwner(match.match, player);
                        }
                        return new MessageOwner(nm.match, entry.getKey());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Strip color codes from a String
     *
     * @param string String to strip
     * @return String stripped of colorcodes
     */
    public String stripColorCodes(String string) {
        return string.replaceAll("§.", "");
    }

    private final TreeMap<Integer, String> map = new TreeMap<>();

    static {
        map.put(1000, "M");
        map.put(900, "CM");
        map.put(500, "D");
        map.put(400, "CD");
        map.put(100, "C");
        map.put(90, "XC");
        map.put(50, "L");
        map.put(40, "XL");
        map.put(10, "X");
        map.put(9, "IX");
        map.put(5, "V");
        map.put(4, "IV");
        map.put(1, "I");
    }

    /**
     * Turns a number into a Roman Numeral.
     *
     * <p>Example: 4 -> IV
     *
     * <p>https://stackoverflow.com/questions/12967896/converting-integers-to-roman-numerals-java/12968022
     *
     * @param number Example to convert to
     * @return String or Roman Numeral
     */
    public String toRoman(int number) {
        boolean neg = false;
        if (number == 0) {
            return "O";
        }
        if (number < 0) {
            neg = true;
            number = -1 * number;
        }
        int l = map.floorKey(number);
        if (number == l) {
            return map.get(number);
        }
        if (neg) {
            return "-" + map.get(l) + toRoman(number - l);
        } else {
            return map.get(l) + toRoman(number - l);
        }
    }
}
