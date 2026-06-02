package com.example.civevent.util;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class TextUtil {
    private TextUtil() {
    }

    public static String key(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    public static String joinArgs(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    public static String emptyDefault(@Nullable String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String formatDuration(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
