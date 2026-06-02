package com.example.civevent;

import com.example.civevent.commands.CivCommand;
import com.example.civevent.listeners.ChatListener;
import com.example.civevent.listeners.PlayerListener;
import com.example.civevent.service.EventService;
import com.example.civevent.service.NationService;
import com.example.civevent.service.SessionService;
import com.example.civevent.service.StateService;
import com.example.civevent.util.JsonLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;

public final class CivilisationEventPlugin extends JavaPlugin {
    private StateService stateService;
    private SessionService sessionService;
    private JsonLogger jsonLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        jsonLogger = new JsonLogger(this);
        stateService = new StateService(this);
        EventService eventService = new EventService(this, stateService);
        sessionService = new SessionService(this, jsonLogger);
        NationService nationService = new NationService(stateService);

        stateService.load();
        nationService.ensureTeams();
        nationService.applyAllVisuals();

        CivCommand commandHandler = new CivCommand(stateService, eventService, sessionService, nationService, jsonLogger);
        registerCommand("event", commandHandler);
        registerCommand("session", commandHandler);
        registerCommand("nations", commandHandler);
        registerCommand("nation", commandHandler);
        registerCommand("nationchat", commandHandler);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(nationService, sessionService), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this, stateService, nationService, jsonLogger), this);
        jsonLogger.log("plugin_enabled", Map.of("version", getPluginMeta().getVersion()));
    }

    @Override
    public void onDisable() {
        if (sessionService != null) {
            sessionService.shutdown();
        }
        if (stateService != null) {
            stateService.save();
        }
        if (jsonLogger != null) {
            jsonLogger.log("plugin_disabled", Map.of());
        }
    }

    private void registerCommand(String name, CivCommand commandHandler) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command in plugin.yml: " + name);
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);
    }
}
