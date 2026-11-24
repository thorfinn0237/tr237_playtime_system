package tr237.tr237_Playtime_System.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PlaytimeTabCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String label,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();

            if (sender.hasPermission("playtime.top") && "top".startsWith(prefix)) {
                suggestions.add("top");
            }
            if (sender.hasPermission("playtime.admin")) {
                for (String sub : List.of("set", "add", "remove")) {
                    if (sub.startsWith(prefix)) {
                        suggestions.add(sub);
                    }
                }
            }
            if (sender.hasPermission("playtime.reload") && "reload".startsWith(prefix)) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("playtime.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        suggestions.add(p.getName());
                    }
                }
            }
            return suggestions;
        }

        if (args.length == 2 && sender.hasPermission("playtime.admin")) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("set") || sub.equals("add") || sub.equals("remove")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        players.add(p.getName());
                    }
                }
                return players;
            }
        }

        if (args.length == 3 && sender.hasPermission("playtime.admin")) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("set") || sub.equals("add") || sub.equals("remove")) {
                return List.of("30m", "1h", "1d", "1d2h30m");
            }
        }

        return Collections.emptyList();
    }
}