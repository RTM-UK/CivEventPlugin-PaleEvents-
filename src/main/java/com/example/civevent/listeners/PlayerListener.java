package com.RTM.civevent.listeners;

import com.RTM.civevent.service.NationService;
import com.RTM.civevent.service.SessionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final NationService nationService;
    private final SessionService sessionService;

    public PlayerListener(NationService nationService, SessionService sessionService) {
        this.nationService = nationService;
        this.sessionService = sessionService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        nationService.applyVisuals(event.getPlayer());
        sessionService.updateBossBars();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionService.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        sessionService.updateBossBars();
    }
}
