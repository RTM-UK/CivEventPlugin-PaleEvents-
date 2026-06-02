package com.example.civevent.commands;

import com.example.civevent.model.EventData;
import com.example.civevent.model.Nation;
import com.example.civevent.model.SessionData;
import com.example.civevent.service.EventService;
import com.example.civevent.service.NationService;
import com.example.civevent.service.SessionService;
import com.example.civevent.service.StateService;
import com.example.civevent.util.JsonLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.example.civevent.util.MessageUtil.ACCENT;
import static com.example.civevent.util.MessageUtil.TEXT;
import static com.example.civevent.util.MessageUtil.VALUE;
import static com.example.civevent.util.MessageUtil.error;
import static com.example.civevent.util.MessageUtil.help;
import static com.example.civevent.util.MessageUtil.highlight;
import static com.example.civevent.util.MessageUtil.requireAdmin;
import static com.example.civevent.util.MessageUtil.success;
import static com.example.civevent.util.MessageUtil.usage;
import static com.example.civevent.util.MessageUtil.warn;
import static com.example.civevent.util.TextUtil.emptyDefault;
import static com.example.civevent.util.TextUtil.joinArgs;
import static com.example.civevent.util.TextUtil.key;

public final class CivCommand implements CommandExecutor, TabCompleter {
    private final StateService state;
    private final EventService eventService;
    private final SessionService sessionService;
    private final NationService nationService;
    private final JsonLogger logger;

    public CivCommand(StateService state, EventService eventService, SessionService sessionService, NationService nationService, JsonLogger logger) {
        this.state = state;
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.nationService = nationService;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "event" -> handleEvent(sender, args);
            case "session" -> handleSession(sender, args);
            case "nations" -> handleNations(sender, args);
            case "nation" -> handleNation(sender, args);
            case "nationchat" -> handleNationChat(sender);
            default -> false;
        };
    }

    private boolean handleEvent(CommandSender sender, String[] args) {
        if (args.length == 0) {
            help(sender, "Event commands: /event <create|delete|setinfo|setworld|join|leave|info>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> createEvent(sender, args);
            case "delete" -> deleteEvent(sender, args);
            case "setinfo" -> setEventInfo(sender, args);
            case "setworld" -> setEventWorld(sender, args);
            case "join" -> joinEvent(sender, args);
            case "leave" -> leaveEvent(sender, args);
            case "info" -> eventInfo(sender, args);
            default -> help(sender, "Event commands: /event <create|delete|setinfo|setworld|join|leave|info>");
        }
        return true;
    }

    private void createEvent(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            usage(sender, "/event create <name>");
            return;
        }
        if (eventService.exists(args[1])) {
            error(sender, "An event called " + args[1] + " already exists.");
            return;
        }
        if (!eventService.isSafeWorldName(args[1])) {
            error(sender, "Event names can only use letters, numbers, underscores, and hyphens.");
            return;
        }
        World world = eventService.createOrLoadWorld(args[1]);
        if (world == null) {
            error(sender, "Could not create world " + args[1] + ".");
            return;
        }
        eventService.put(new EventData(args[1], "", world.getName()));
        state.save();
        success(sender, "Created event " + highlight(args[1]) + TEXT + " and world " + highlight(world.getName()) + TEXT + ".");
        logger.log("event_create", Map.of("event", args[1], "world", world.getName(), "sender", sender.getName()));
    }

    private void deleteEvent(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 2) {
            usage(sender, "/event delete <name>");
            return;
        }
        String eventKey = key(args[1]);
        EventData removed = eventService.remove(args[1]);
        SessionData session = sessionService.remove(args[1]);
        if (session != null) {
            sessionService.end(session, "deleted", true);
        }
        state.playerEvents().entrySet().removeIf(entry -> entry.getValue().equals(eventKey));
        if (removed == null) {
            error(sender, "No event named " + args[1] + ".");
            return;
        }
        state.save();
        success(sender, "Deleted event " + highlight(removed.name()) + TEXT + ".");
        logger.log("event_delete", Map.of("event", removed.name(), "sender", sender.getName()));
    }

    private void setEventInfo(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (args.length < 3) {
            usage(sender, "/event setinfo <event> <description>");
            return;
        }
        EventData event = eventService.get(args[1]);
        if (event == null) {
            missingEvent(sender, args[1]);
            return;
        }
        event.description(joinArgs(args, 2));
        state.save();
        success(sender, "Updated info for " + highlight(event.name()) + TEXT + ".");
        logger.log("event_setinfo", Map.of("event", event.name(), "sender", sender.getName()));
    }

    private void setEventWorld(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (!(sender instanceof Player player)) {
            error(sender, "Only an in-game player can set an event world.");
            return;
        }
        if (args.length < 2) {
            usage(sender, "/event setworld <event>");
            return;
        }
        EventData event = eventService.get(args[1]);
        if (event == null) {
            missingEvent(sender, args[1]);
            return;
        }
        event.worldName(player.getWorld().getName());
        state.save();
        success(sender, "Set " + highlight(event.name()) + TEXT + " world to " + highlight(event.worldName()) + TEXT + ".");
        logger.log("event_setworld", Map.of("event", event.name(), "world", event.worldName(), "sender", sender.getName()));
    }

    private void joinEvent(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            error(sender, "Only players can join events.");
            return;
        }
        if (args.length < 2) {
            usage(sender, "/event join <event>");
            return;
        }
        EventData event = eventService.get(args[1]);
        if (event == null) {
            missingEvent(sender, args[1]);
            return;
        }
        World eventWorld = eventService.loadWorld(event);
        if (eventWorld == null) {
            error(sender, "That event does not have a usable world yet.");
            return;
        }
        state.playerEvents().put(player.getUniqueId(), key(event.name()));
        player.teleport(eventWorld.getSpawnLocation());
        state.save();
        success(sender, "You joined " + highlight(event.name()) + TEXT + " and were sent to " + highlight(eventWorld.getName()) + TEXT + ".");
        logger.log("event_join", Map.of("event", event.name(), "player", player.getName()));
    }

    private void leaveEvent(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            error(sender, "Only players can leave events.");
            return;
        }
        if (args.length < 2) {
            usage(sender, "/event leave <event>");
            return;
        }
        String eventKey = key(args[1]);
        String current = state.playerEvents().get(player.getUniqueId());
        if (!eventKey.equals(current)) {
            error(sender, "You are not currently in " + args[1] + ".");
            return;
        }
        state.playerEvents().remove(player.getUniqueId());
        state.save();
        success(sender, "You left " + highlight(args[1]) + TEXT + ".");
        logger.log("event_leave", Map.of("event", args[1], "player", player.getName()));
    }

    private void eventInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/event info <event>");
            return;
        }
        EventData event = eventService.get(args[1]);
        if (event == null) {
            missingEvent(sender, args[1]);
            return;
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "------ " + ACCENT + event.name() + ChatColor.DARK_GRAY + " ------");
        sender.sendMessage(ACCENT + "Info: " + VALUE + emptyDefault(event.description(), "No description set."));
        sender.sendMessage(ACCENT + "World: " + VALUE + emptyDefault(event.worldName(), "Not set"));
        sender.sendMessage(ACCENT + "Session: " + VALUE + (sessionService.isRunning(event.name()) ? ChatColor.GREEN + "Running" : ChatColor.GRAY + "Not running"));
    }

    private boolean handleSession(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length == 0) return usage(sender, "/session <start|stop|over>");
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("start")) {
            startSession(sender, args);
            return true;
        }
        if (sub.equals("stop") || sub.equals("over")) {
            endSession(sender, args, sub);
            return true;
        }
        return usage(sender, "/session <start|stop|over>");
    }

    private void startSession(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            error(sender, "Only an in-game player can start a world-scoped session.");
            return;
        }
        if (args.length < 3) {
            usage(sender, "/session start <event> <durationMinutes>");
            return;
        }
        EventData event = eventService.get(args[1]);
        if (event == null) {
            missingEvent(sender, args[1]);
            return;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            error(sender, "Duration must be a whole number of minutes.");
            return;
        }
        if (minutes <= 0) {
            error(sender, "Duration must be greater than 0.");
            return;
        }
        if (sessionService.isRunning(event.name())) {
            error(sender, "That event already has a running session.");
            return;
        }
        sessionService.start(event, player.getWorld(), minutes, sender.getName());
        success(sender, "Started " + highlight(event.name()) + TEXT + " for " + highlight(minutes + "m") + TEXT + " in " + highlight(player.getWorld().getName()) + TEXT + ".");
    }

    private void endSession(CommandSender sender, String[] args, String reason) {
        if (args.length < 2) {
            usage(sender, "/session " + reason + " <event>");
            return;
        }
        SessionData session = sessionService.remove(args[1]);
        if (session == null) {
            error(sender, "There is no running session for " + args[1] + ".");
            return;
        }
        sessionService.end(session, reason, true);
        success(sender, "Ended session " + highlight(session.eventName()) + TEXT + ".");
    }

    private boolean handleNations(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length != 1 || !args[0].equalsIgnoreCase("create")) {
            return usage(sender, "/nations create");
        }
        Location redSpawn = state.nationSpawns().get(Nation.RED);
        Location blueSpawn = state.nationSpawns().get(Nation.BLUE);
        if (redSpawn == null || blueSpawn == null) {
            error(sender, "Set both nation spawns first: /nation red setspawn and /nation blue setspawn.");
            return true;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Nation nation = i % 2 == 0 ? Nation.RED : Nation.BLUE;
            nationService.setNation(player, nation);
            player.teleport(nation == Nation.RED ? redSpawn : blueSpawn);
            success(player, "You are now fighting for " + nation.chatColor() + nation.displayName() + TEXT + ".");
        }
        state.save();
        success(sender, "Split " + highlight(players.size() + " player(s)") + TEXT + " into " + ChatColor.RED + "Red" + TEXT + " and " + ChatColor.BLUE + "Blue" + TEXT + ".");
        logger.log("nations_create", Map.of("sender", sender.getName(), "players", players.size()));
        return true;
    }

    private boolean handleNation(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (!(sender instanceof Player player)) {
            error(sender, "Only an in-game player can set nation spawns.");
            return true;
        }
        if (args.length != 2 || !args[1].equalsIgnoreCase("setspawn")) {
            return usage(sender, "/nation <blue|red> setspawn");
        }
        Nation nation = Nation.from(args[0]);
        if (nation == null) return usage(sender, "/nation <blue|red> setspawn");
        state.nationSpawns().put(nation, player.getLocation());
        state.save();
        success(sender, "Set " + nation.chatColor() + nation.displayName() + TEXT + " spawn at your location.");
        logger.log("nation_setspawn", Map.of("nation", nation.displayName(), "world", player.getWorld().getName(), "sender", sender.getName()));
        return true;
    }

    private boolean handleNationChat(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            error(sender, "Only players can use nation chat.");
            return true;
        }
        if (!nationService.hasNation(player)) {
            error(sender, "You are not in a nation yet.");
            return true;
        }
        if (!state.nationChat().add(player.getUniqueId())) {
            state.nationChat().remove(player.getUniqueId());
            warn(sender, "Nation chat disabled. Your messages are public again.");
        } else {
            success(sender, "Nation chat enabled. Only your nation can hear you.");
        }
        state.save();
        return true;
    }

    private void missingEvent(CommandSender sender, String name) {
        error(sender, "No event named " + name + ".");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("event")) {
            if (args.length == 1) return filter(List.of("create", "delete", "setinfo", "setworld", "join", "leave", "info"), args[0]);
            if (args.length == 2 && List.of("delete", "setinfo", "setworld", "join", "leave", "info").contains(args[0].toLowerCase(Locale.ROOT))) {
                return filter(eventService.names(), args[1]);
            }
        }
        if (name.equals("session")) {
            if (args.length == 1) return filter(List.of("start", "stop", "over"), args[0]);
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("start")) return filter(eventService.names(), args[1]);
                if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("over")) return filter(sessionService.names(), args[1]);
            }
        }
        if (name.equals("nations") && args.length == 1) return filter(List.of("create"), args[0]);
        if (name.equals("nation")) {
            if (args.length == 1) return filter(List.of("blue", "red"), args[0]);
            if (args.length == 2) return filter(List.of("setspawn"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
