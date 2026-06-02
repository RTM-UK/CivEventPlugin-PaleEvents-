package com.example.civevent.service;

import com.example.civevent.model.EventData;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.example.civevent.util.TextUtil.key;

public final class EventService {
    private final JavaPlugin plugin;
    private final StateService state;

    public EventService(JavaPlugin plugin, StateService state) {
        this.plugin = plugin;
        this.state = state;
    }

    public @Nullable EventData get(String name) {
        return state.events().get(key(name));
    }

    public boolean exists(String name) {
        return state.events().containsKey(key(name));
    }

    public void put(EventData event) {
        state.events().put(key(event.name()), event);
    }

    public @Nullable EventData remove(String name) {
        return state.events().remove(key(name));
    }

    public List<String> names() {
        return state.events().values().stream().map(EventData::name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public boolean isSafeWorldName(String name) {
        return name.matches("[A-Za-z0-9_-]+");
    }

    public @Nullable World createOrLoadWorld(String worldName) {
        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) return loaded;

        try {
            return Bukkit.createWorld(new WorldCreator(worldName));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Could not create event world " + worldName + ": " + ex.getMessage());
            return null;
        }
    }

    public @Nullable World loadWorld(EventData event) {
        String worldName = event.worldName() == null || event.worldName().isBlank() ? event.name() : event.worldName();
        if (!isSafeWorldName(worldName)) return null;
        World world = createOrLoadWorld(worldName);
        if (world != null && (event.worldName() == null || event.worldName().isBlank())) {
            event.worldName(world.getName());
            state.save();
        }
        return world;
    }
}
