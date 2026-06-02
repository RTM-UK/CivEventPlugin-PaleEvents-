package com.raffe.civevent.listeners;

import com.raffe.civevent.model.Nation;
import com.raffe.civevent.service.NationService;
import com.raffe.civevent.service.StateService;
import com.raffe.civevent.util.JsonLogger;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

import static com.raffe.civevent.util.MessageUtil.error;

public final class ChatListener implements Listener {
    private final JavaPlugin plugin;
    private final StateService state;
    private final NationService nationService;
    private final JsonLogger logger;

    public ChatListener(JavaPlugin plugin, StateService state, NationService nationService, JsonLogger logger) {
        this.plugin = plugin;
        this.state = state;
        this.nationService = nationService;
        this.logger = logger;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        Nation nation = state.playerNations().get(sender.getUniqueId());
        if (state.nationChat().contains(sender.getUniqueId()) && nation == null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                state.nationChat().remove(sender.getUniqueId());
                error(sender, "Nation chat was disabled because you are not in a nation.");
                state.save();
            });
            return;
        }
        if (nation == null) return;

        event.setCancelled(true);
        Component message = Component.text(sender.getName() + ": ", nation.textColor()).append(event.message());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (state.nationChat().contains(sender.getUniqueId())) {
                Component nationMessage = Component.text("[Nation] ", nation.textColor()).append(message);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (nationService.nation(player).filter(nation::equals).isPresent()) {
                        player.sendMessage(nationMessage);
                    }
                }
                plugin.getLogger().info("[NationChat:" + nation.displayName() + "] " + sender.getName());
                logger.log("nation_chat", Map.of("nation", nation.displayName(), "player", sender.getName()));
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
            plugin.getLogger().info("[Chat:" + nation.displayName() + "] " + sender.getName());
            logger.log("nation_colored_chat", Map.of("nation", nation.displayName(), "player", sender.getName()));
        });
    }
}
