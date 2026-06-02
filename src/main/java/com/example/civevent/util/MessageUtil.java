package com.raffe.civevent.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    public static final ChatColor ACCENT = ChatColor.GOLD;
    public static final ChatColor TEXT = ChatColor.GRAY;
    public static final ChatColor VALUE = ChatColor.WHITE;

    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CivEvent" + ChatColor.DARK_GRAY + "] ";

    private MessageUtil() {
    }

    public static void success(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.GREEN + "+ " + TEXT + message);
    }

    public static void error(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.RED + "x " + TEXT + message);
    }

    public static void warn(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.YELLOW + "! " + TEXT + message);
    }

    public static void help(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ACCENT + message);
    }

    public static String highlight(String value) {
        return VALUE + value;
    }

    public static boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("civevent.admin")) return true;
        error(sender, "You do not have permission to use that command.");
        return false;
    }

    public static boolean usage(CommandSender sender, String usage) {
        help(sender, "Usage: " + usage);
        return true;
    }
}
