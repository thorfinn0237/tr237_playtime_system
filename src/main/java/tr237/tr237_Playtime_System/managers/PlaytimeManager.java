package tr237.tr237_Playtime_System.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import tr237.tr237_Playtime_System.database.DatabaseManager;
import tr237.tr237_Playtime_System.models.PlayerPlaytime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeManager {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerPlaytime> cache = new ConcurrentHashMap<>();
    private BukkitTask autoSaveTask;

    public PlaytimeManager(Plugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void loadAllData() {
        cache.clear();
        cache.putAll(databaseManager.loadAll());

        // Handle players who are already online (e.g. during a /reload)
        for (Player online : Bukkit.getOnlinePlayers()) {
            handlePlayerJoin(online);
        }
    }

    public void startAutoSaveTask() {
        stopAutoSaveTask();
        long intervalSeconds = Math.max(30L, plugin.getConfig().getLong("settings.auto-save-interval-seconds", 300L));
        long intervalTicks = intervalSeconds * 20L;

        this.autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            List<PlayerPlaytime> snapshot = new ArrayList<>(cache.values());
            databaseManager.saveBatch(snapshot, true);
        }, intervalTicks, intervalTicks);
    }

    public void stopAutoSaveTask() {
        if (this.autoSaveTask != null) {
            this.autoSaveTask.cancel();
            this.autoSaveTask = null;
        }
    }

    public void handlePlayerJoin(Player player) {
        PlayerPlaytime data = cache.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new PlayerPlaytime(uuid, player.getName(), 0L)
        );
        data.setUsername(player.getName());
        data.startSession();
    }

    public void handlePlayerQuit(Player player) {
        PlayerPlaytime data = cache.get(player.getUniqueId());
        if (data == null) {
            return;
        }
        data.setUsername(player.getName());
        data.flushSession(false);

        final UUID uuid = data.getUuid();
        final String name = data.getUsername();
        final long total = data.getStoredSeconds();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.savePlayer(uuid, name, total));
    }

    public PlayerPlaytime getOrCreate(UUID uuid, String username) {
        PlayerPlaytime data = cache.computeIfAbsent(
                uuid,
                id -> new PlayerPlaytime(id, username != null ? username : "Unknown", 0L)
        );
        if (username != null && !username.isBlank()) {
            data.setUsername(username);
        }
        return data;
    }

    /**
     * Resolves an online or offline player without blocking the main thread on Mojang HTTP calls.
     */
    public PlayerPlaytime resolvePlayerByName(String targetName) {
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            return getOrCreate(online.getUniqueId(), online.getName());
        }

        for (PlayerPlaytime entry : cache.values()) {
            if (entry.getUsername().equalsIgnoreCase(targetName)) {
                return entry;
            }
        }

        OfflinePlayer cachedOffline = Bukkit.getOfflinePlayerIfCached(targetName);
        if (cachedOffline != null) {
            String resolvedName = cachedOffline.getName() != null ? cachedOffline.getName() : targetName;
            return getOrCreate(cachedOffline.getUniqueId(), resolvedName);
        }

        return null;
    }

    public void setPlaytime(PlayerPlaytime target, long seconds) {
        boolean isOnline = Bukkit.getPlayer(target.getUuid()) != null;
        target.flushSession(isOnline);
        target.setStoredSeconds(seconds);
        saveSingleAsync(target);
    }

    public void addPlaytime(PlayerPlaytime target, long secondsToAdd) {
        boolean isOnline = Bukkit.getPlayer(target.getUuid()) != null;
        target.flushSession(isOnline);
        long updated;
        try {
            updated = Math.addExact(target.getStoredSeconds(), Math.max(0L, secondsToAdd));
        } catch (ArithmeticException e) {
            updated = Long.MAX_VALUE / 2;
        }
        target.setStoredSeconds(updated);
        saveSingleAsync(target);
    }

    public void removePlaytime(PlayerPlaytime target, long secondsToRemove) {
        boolean isOnline = Bukkit.getPlayer(target.getUuid()) != null;
        target.flushSession(isOnline);
        long updated = Math.max(0L, target.getStoredSeconds() - Math.max(0L, secondsToRemove));
        target.setStoredSeconds(updated);
        saveSingleAsync(target);
    }

    private void saveSingleAsync(PlayerPlaytime data) {
        final UUID uuid = data.getUuid();
        final String name = data.getUsername();
        final long stored = data.getStoredSeconds();

        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.savePlayer(uuid, name, stored));
        } else {
            databaseManager.savePlayer(uuid, name, stored);
        }
    }

    public List<PlayerPlaytime> getTopPlayers(int limit) {
        List<PlayerPlaytime> list = new ArrayList<>(cache.values());
        list.sort(Comparator.comparingLong(PlayerPlaytime::getTotalSeconds).reversed());
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    public void saveAllSyncOnShutdown() {
        stopAutoSaveTask();
        databaseManager.saveBatch(new ArrayList<>(cache.values()), false);
    }
}