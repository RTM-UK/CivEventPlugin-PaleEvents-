package com.raffe.civevent.service;

import com.raffe.civevent.model.EventData;
import com.raffe.civevent.model.SessionData;
import com.raffe.civevent.util.JsonLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.raffe.civevent.util.MessageUtil.TEXT;
import static com.raffe.civevent.util.MessageUtil.highlight;
import static com.raffe.civevent.util.MessageUtil.success;
import static com.raffe.civevent.util.TextUtil.formatDuration;
import static com.raffe.civevent.util.TextUtil.key;

public final class SessionService {
    private final JavaPlugin plugin;
    private final JsonLogger logger;
    private final Map<String, SessionData> sessions = new LinkedHashMap<>();

    public SessionService(JavaPlugin plugin, JsonLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public Map<String, SessionData> sessions() {
        return sessions;
    }

    public boolean isRunning(String eventName) {
        return sessions.containsKey(key(eventName));
    }

    public SessionData remove(String eventName) {
        return sessions.remove(key(eventName));
    }

    public List<String> names() {
        return sessions.values().stream().map(SessionData::eventName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public void start(EventData event, World world, int minutes, String senderName) {
        long totalSeconds = minutes * 60L;
        BossBar bossBar = Bukkit.createBossBar(ChatColor.GOLD + event.name() + ChatColor.GRAY + " | " + ChatColor.GREEN + "Session starting", BarColor.GREEN, BarStyle.SEGMENTED_10);
        SessionData session = new SessionData(key(event.name()), event.name(), world.getName(), totalSeconds, bossBar);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(session), 0L, 20L);
        session.task(task);
        sessions.put(session.eventKey(), session);
        updateBossBarPlayers(session);
        logger.log("session_start", Map.of("event", event.name(), "world", world.getName(), "minutes", minutes, "sender", senderName));
    }

    public void end(SessionData session, String reason, boolean adventureMode) {
        session.cancel();
        session.bossBar().removeAll();
        World world = Bukkit.getWorld(session.worldName());
        if (adventureMode && world != null) {
            for (Player player : world.getPlayers()) {
                player.setGameMode(GameMode.ADVENTURE);
                success(player, highlight(session.eventName()) + TEXT + " has ended. You are now in Adventure Mode.");
            }
        }
        logger.log("session_end", Map.of("event", session.eventName(), "world", session.worldName(), "reason", reason));
    }

    public void updateBossBars() {
        for (SessionData session : sessions.values()) {
            updateBossBarPlayers(session);
        }
    }

    public void removePlayer(Player player) {
        for (SessionData session : sessions.values()) {
            session.bossBar().removePlayer(player);
        }
    }

    public void shutdown() {
        for (SessionData session : sessions.values()) {
            session.cancel();
        }
        sessions.clear();
    }

    private void tick(SessionData session) {
        if (!sessions.containsKey(session.eventKey())) return;
        session.decrementRemainingSeconds();
        updateBossBar(session);
        if (session.remainingSeconds() <= 0) {
            sessions.remove(session.eventKey());
            end(session, "timer", true);
        }
    }

    private void updateBossBar(SessionData session) {
        long remaining = Math.max(0L, session.remainingSeconds());
        double progress = Math.max(0.0, Math.min(1.0, (double) remaining / (double) session.totalSeconds()));
        session.bossBar().setProgress(progress);
        session.bossBar().setTitle(ChatColor.GOLD + session.eventName() + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + formatDuration(remaining) + ChatColor.GRAY + " remaining");
        updateBossBarPlayers(session);
    }

    private void updateBossBarPlayers(SessionData session) {
        session.bossBar().removeAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(session.worldName())) {
                session.bossBar().addPlayer(player);
            }
        }
    }
}
