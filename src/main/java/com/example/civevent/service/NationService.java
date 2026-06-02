package com.raffe.civevent.service;

import com.raffe.civevent.model.Nation;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class NationService {
    private final StateService state;
    private Scoreboard scoreboard;

    public NationService(StateService state) {
        this.state = state;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public Optional<Nation> nation(Player player) {
        return Optional.ofNullable(state.playerNations().get(player.getUniqueId()));
    }

    public void setNation(Player player, Nation nation) {
        state.playerNations().put(player.getUniqueId(), nation);
        applyVisuals(player);
    }

    public boolean hasNation(Player player) {
        return state.playerNations().containsKey(player.getUniqueId());
    }

    public Set<UUID> nationChat() {
        return state.nationChat();
    }

    public void ensureTeams() {
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        }
        if (scoreboard == null) return;
        for (Nation nation : Nation.values()) {
            Team team = scoreboard.getTeam(nation.teamName());
            if (team == null) {
                team = scoreboard.registerNewTeam(nation.teamName());
            }
            team.setColor(nation.chatColor());
            team.setPrefix(nation.chatColor().toString());
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
    }

    public void applyAllVisuals() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyVisuals(player);
        }
    }

    public void applyVisuals(Player player) {
        if (scoreboard == null) return;
        ensureTeams();

        for (Nation nation : Nation.values()) {
            Team team = scoreboard.getTeam(nation.teamName());
            if (team != null) {
                team.removeEntry(player.getName());
            }
        }

        Nation nation = state.playerNations().get(player.getUniqueId());
        if (nation == null) {
            player.displayName(Component.text(player.getName()));
            player.playerListName(Component.text(player.getName()));
            return;
        }

        Team team = scoreboard.getTeam(nation.teamName());
        if (team != null) {
            team.addEntry(player.getName());
            player.setScoreboard(scoreboard);
        }
        Component coloredName = Component.text(player.getName(), nation.textColor());
        player.displayName(coloredName);
        player.playerListName(coloredName);
    }
}
