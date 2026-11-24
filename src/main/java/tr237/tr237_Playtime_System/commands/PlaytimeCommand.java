package tr237.tr237_Playtime_System.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tr237.tr237_Playtime_System.Tr237_Playtime_System;
import tr237.tr237_Playtime_System.managers.PlaytimeManager;
import tr237.tr237_Playtime_System.models.PlayerPlaytime;
import tr237.tr237_Playtime_System.utils.MessageUtil;
import tr237.tr237_Playtime_System.utils.TimeFormatter;

import java.util.List;
import java.util.Locale;

public class PlaytimeCommand implements CommandExecutor {

    private final Tr237_Playtime_System plugin;
    private final PlaytimeManager playtimeManager;

    public PlaytimeCommand(Tr237_Playtime_System plugin, PlaytimeManager playtimeManager) {
        this.plugin = plugin;
        this.playtimeManager = playtimeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("playtime.use")) {
                MessageUtil.send(sender, plugin, "no-permission");
                return true;
            }
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, plugin, "only-players");
                return true;
            }

            PlayerPlaytime self = playtimeManager.getOrCreate(player.getUniqueId(), player.getName());
            String formatted = TimeFormatter.formatSeconds(self.getTotalSeconds());
            MessageUtil.send(sender, plugin, "self-playtime", "%time%", formatted);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "top" -> {
                if (!sender.hasPermission("playtime.top")) {
                    MessageUtil.send(sender, plugin, "no-permission");
                    return true;
                }
                handleLeaderboard(sender);
            }
            case "reload" -> {
                if (!sender.hasPermission("playtime.reload")) {
                    MessageUtil.send(sender, plugin, "no-permission");
                    return true;
                }
                plugin.reloadConfig();
                playtimeManager.startAutoSaveTask();
                MessageUtil.send(sender, plugin, "reloaded");
            }
            case "set", "add", "remove" -> {
                if (!sender.hasPermission("playtime.admin")) {
                    MessageUtil.send(sender, plugin, "no-permission");
                    return true;
                }
                handleAdminModify(sender, sub, args);
            }
            default -> {
                if (args.length != 1) {
                    MessageUtil.send(sender, plugin, "usage");
                    return true;
                }
                if (!sender.hasPermission("playtime.others")) {
                    MessageUtil.send(sender, plugin, "no-permission");
                    return true;
                }
                handleOtherLookup(sender, args[0]);
            }
        }

        return true;
    }

    private void handleLeaderboard(CommandSender sender) {
        int limit = Math.max(1, plugin.getConfig().getInt("settings.leaderboard-limit", 10));
        List<PlayerPlaytime> top = playtimeManager.getTopPlayers(limit);

        MessageUtil.sendRaw(sender, plugin, "top-header", "%limit%", String.valueOf(limit));
        if (top.isEmpty()) {
            MessageUtil.sendRaw(sender, plugin, "top-empty");
        } else {
            for (int i = 0; i < top.size(); i++) {
                PlayerPlaytime entry = top.get(i);
                MessageUtil.sendRaw(
                        sender,
                        plugin,
                        "top-entry",
                        "%rank%", String.valueOf(i + 1),
                        "%player%", entry.getUsername(),
                        "%time%", TimeFormatter.formatSeconds(entry.getTotalSeconds())
                );
            }
        }
        MessageUtil.sendRaw(sender, plugin, "top-footer");
    }

    private void handleOtherLookup(CommandSender sender, String targetName) {
        PlayerPlaytime target = playtimeManager.resolvePlayerByName(targetName);
        if (target == null) {
            MessageUtil.send(sender, plugin, "player-not-found", "%player%", targetName);
            return;
        }

        String formatted = TimeFormatter.formatSeconds(target.getTotalSeconds());
        MessageUtil.send(
                sender,
                plugin,
                "other-playtime",
                "%player%", target.getUsername(),
                "%time%", formatted
        );
    }

    private void handleAdminModify(CommandSender sender, String action, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, plugin, "usage");
            return;
        }

        String targetName = args[1];
        PlayerPlaytime target = playtimeManager.resolvePlayerByName(targetName);
        if (target == null) {
            MessageUtil.send(sender, plugin, "player-not-found", "%player%", targetName);
            return;
        }

        long durationSeconds = TimeFormatter.parseDurationToSeconds(args[2]);
        if (durationSeconds < 0L) {
            MessageUtil.send(sender, plugin, "invalid-duration");
            return;
        }

        String amountFormatted = TimeFormatter.formatSeconds(durationSeconds);

        switch (action) {
            case "set" -> {
                playtimeManager.setPlaytime(target, durationSeconds);
                MessageUtil.send(
                        sender,
                        plugin,
                        "admin-set",
                        "%player%", target.getUsername(),
                        "%time%", TimeFormatter.formatSeconds(target.getTotalSeconds())
                );
            }
            case "add" -> {
                playtimeManager.addPlaytime(target, durationSeconds);
                MessageUtil.send(
                        sender,
                        plugin,
                        "admin-add",
                        "%player%", target.getUsername(),
                        "%amount%", amountFormatted,
                        "%time%", TimeFormatter.formatSeconds(target.getTotalSeconds())
                );
            }
            case "remove" -> {
                playtimeManager.removePlaytime(target, durationSeconds);
                MessageUtil.send(
                        sender,
                        plugin,
                        "admin-remove",
                        "%player%", target.getUsername(),
                        "%amount%", amountFormatted,
                        "%time%", TimeFormatter.formatSeconds(target.getTotalSeconds())
                );
            }
        }
    }
}