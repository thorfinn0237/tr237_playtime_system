package tr237.tr237_Playtime_System.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public final class MessageUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private MessageUtil() {
    }

    public static Component colorize(String text) {
        if (text == null) {
            return Component.empty();
        }
        return SERIALIZER.deserialize(text.replace('&', '§'));
    }

    public static void send(CommandSender sender, Plugin plugin, String path, String... placeholders) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String raw = plugin.getConfig().getString("messages." + path, "");
        if (raw.isEmpty()) {
            return;
        }

        String formatted = applyPlaceholders(raw, placeholders);
        sender.sendMessage(colorize(prefix + formatted));
    }

    public static void sendRaw(CommandSender sender, Plugin plugin, String path, String... placeholders) {
        String raw = plugin.getConfig().getString("messages." + path, "");
        if (raw.isEmpty()) {
            return;
        }

        String formatted = applyPlaceholders(raw, placeholders);
        sender.sendMessage(colorize(formatted));
    }

    private static String applyPlaceholders(String message, String... placeholders) {
        String result = message;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            result = result.replace(placeholders[i], placeholders[i + 1]);
        }
        return result;
    }
}