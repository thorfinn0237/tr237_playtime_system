package tr237.tr237_Playtime_System.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeFormatter {

    private static final Pattern DURATION_TOKEN = Pattern.compile("(\\d+)([dhms])");
    private static final Pattern FULL_DURATION = Pattern.compile("^(?:\\d+[dhms])+$");

    private TimeFormatter() {
    }

    public static String formatSeconds(long totalSeconds) {
        long safe = Math.max(0L, totalSeconds);

        long days = safe / 86400L;
        long remainder = safe % 86400L;

        long hours = remainder / 3600L;
        remainder %= 3600L;

        long minutes = remainder / 60L;
        long seconds = remainder % 60L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public static long parseDurationToSeconds(String input) {
        if (input == null || input.isBlank()) {
            return -1L;
        }

        String cleaned = input.trim().toLowerCase(Locale.ROOT);

        if (cleaned.matches("^\\d+$")) {
            try {
                return Long.parseLong(cleaned);
            } catch (NumberFormatException e) {
                return -1L;
            }
        }

        if (!FULL_DURATION.matcher(cleaned).matches()) {
            return -1L;
        }

        Matcher matcher = DURATION_TOKEN.matcher(cleaned);
        long totalSeconds = 0L;

        while (matcher.find()) {
            long value;
            try {
                value = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1L;
            }

            char unit = matcher.group(2).charAt(0);
            long multiplier = switch (unit) {
                case 'd' -> 86400L;
                case 'h' -> 3600L;
                case 'm' -> 60L;
                case 's' -> 1L;
                default -> -1L;
            };

            if (multiplier < 0) {
                return -1L;
            }

            try {
                totalSeconds = Math.addExact(totalSeconds, Math.multiplyExact(value, multiplier));
            } catch (ArithmeticException e) {
                return -1L;
            }
        }

        return totalSeconds;
    }
}