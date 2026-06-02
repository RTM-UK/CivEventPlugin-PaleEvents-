package com.raffe.civevent.model;

import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitTask;

public final class SessionData {
    private final String eventKey;
    private final String eventName;
    private final String worldName;
    private final long totalSeconds;
    private final BossBar bossBar;
    private long remainingSeconds;
    private BukkitTask task;

    public SessionData(String eventKey, String eventName, String worldName, long totalSeconds, BossBar bossBar) {
        this.eventKey = eventKey;
        this.eventName = eventName;
        this.worldName = worldName;
        this.totalSeconds = totalSeconds;
        this.remainingSeconds = totalSeconds;
        this.bossBar = bossBar;
    }

    public String eventKey() {
        return eventKey;
    }

    public String eventName() {
        return eventName;
    }

    public String worldName() {
        return worldName;
    }

    public long totalSeconds() {
        return totalSeconds;
    }

    public BossBar bossBar() {
        return bossBar;
    }

    public long remainingSeconds() {
        return remainingSeconds;
    }

    public void decrementRemainingSeconds() {
        remainingSeconds--;
    }

    public void task(BukkitTask task) {
        this.task = task;
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
