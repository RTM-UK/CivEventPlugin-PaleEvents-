package com.example.civevent.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class JsonLogger {
    private final JavaPlugin plugin;
    private final File logFile;

    public JsonLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "event-log.jsonl");
    }

    public void log(String type, Map<String, ?> fields) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) return;

        Map<String, Object> all = new HashMap<>();
        all.put("time", Instant.now().toString());
        all.put("type", type);
        all.putAll(fields);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(toJson(all));
            writer.newLine();
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write JSON log: " + ex.getMessage());
        }
    }

    private String toJson(Map<String, ?> fields) {
        return fields.entrySet().stream()
            .map(entry -> quote(entry.getKey()) + ":" + jsonValue(entry.getValue()))
            .collect(Collectors.joining(",", "{", "}"));
    }

    private String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return quote(String.valueOf(value));
    }

    private String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
