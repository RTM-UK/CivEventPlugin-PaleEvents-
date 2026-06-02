package com.example.civevent.service;

import com.example.civevent.model.EventData;
import com.example.civevent.model.Nation;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.example.civevent.util.TextUtil.key;

public final class StateService {
    private final JavaPlugin plugin;
    private final Map<String, EventData> events = new LinkedHashMap<>();
    private final Map<UUID, String> playerEvents = new LinkedHashMap<>();
    private final Map<UUID, Nation> playerNations = new LinkedHashMap<>();
    private final Set<UUID> nationChat = new HashSet<>();
    private final Map<Nation, Location> nationSpawns = new LinkedHashMap<>();

    public StateService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, EventData> events() {
        return events;
    }

    public Map<UUID, String> playerEvents() {
        return playerEvents;
    }

    public Map<UUID, Nation> playerNations() {
        return playerNations;
    }

    public Set<UUID> nationChat() {
        return nationChat;
    }

    public Map<Nation, Location> nationSpawns() {
        return nationSpawns;
    }

    public void load() {
        plugin.reloadConfig();
        events.clear();
        playerEvents.clear();
        playerNations.clear();
        nationChat.clear();
        nationSpawns.clear();

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection eventsSection = config.getConfigurationSection("events");
        if (eventsSection != null) {
            for (String eventKey : eventsSection.getKeys(false)) {
                String path = "events." + eventKey + ".";
                String name = config.getString(path + "name", eventKey);
                String description = config.getString(path + "description", "");
                String world = config.getString(path + "world", null);
                events.put(key(name), new EventData(name, description, world));
            }
        }

        for (Nation nation : Nation.values()) {
            Location spawn = config.getLocation("nations." + nation.configKey() + ".spawn");
            if (spawn != null) {
                nationSpawns.put(nation, spawn);
            }
        }

        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidText : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    String base = "players." + uuidText + ".";
                    String event = config.getString(base + "event", null);
                    String nationText = config.getString(base + "nation", null);
                    boolean chat = config.getBoolean(base + "nationChat", false);
                    if (event != null) playerEvents.put(uuid, key(event));
                    Nation nation = Nation.from(nationText);
                    if (nation != null) playerNations.put(uuid, nation);
                    if (chat) nationChat.add(uuid);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Skipping invalid UUID in config: " + uuidText);
                }
            }
        }
    }

    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.set("events", null);
        for (EventData event : events.values()) {
            String path = "events." + key(event.name()) + ".";
            config.set(path + "name", event.name());
            config.set(path + "description", event.description());
            config.set(path + "world", event.worldName());
        }

        for (Nation nation : Nation.values()) {
            config.set("nations." + nation.configKey() + ".spawn", nationSpawns.get(nation));
        }

        config.set("players", null);
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(playerEvents.keySet());
        allPlayers.addAll(playerNations.keySet());
        allPlayers.addAll(nationChat);
        for (UUID uuid : allPlayers) {
            String path = "players." + uuid + ".";
            config.set(path + "event", playerEvents.get(uuid));
            Nation nation = playerNations.get(uuid);
            config.set(path + "nation", nation == null ? null : nation.configKey());
            config.set(path + "nationChat", nationChat.contains(uuid));
        }
        plugin.saveConfig();
    }
}
