package com.raffe.civevent.model;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

public enum Nation {
    RED("red", "civevent_red", "Red", ChatColor.RED, NamedTextColor.RED),
    BLUE("blue", "civevent_blue", "Blue", ChatColor.BLUE, NamedTextColor.BLUE);

    private final String configKey;
    private final String teamName;
    private final String displayName;
    private final ChatColor chatColor;
    private final NamedTextColor textColor;

    Nation(String configKey, String teamName, String displayName, ChatColor chatColor, NamedTextColor textColor) {
        this.configKey = configKey;
        this.teamName = teamName;
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.textColor = textColor;
    }

    public String configKey() {
        return configKey;
    }

    public String teamName() {
        return teamName;
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor chatColor() {
        return chatColor;
    }

    public NamedTextColor textColor() {
        return textColor;
    }

    public static @Nullable Nation from(@Nullable String value) {
        if (value == null) return null;
        for (Nation nation : values()) {
            if (nation.configKey.equalsIgnoreCase(value) || nation.displayName.equalsIgnoreCase(value)) {
                return nation;
            }
        }
        return null;
    }
}
