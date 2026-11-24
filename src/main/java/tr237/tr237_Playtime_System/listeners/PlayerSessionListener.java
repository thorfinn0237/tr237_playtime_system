package tr237.tr237_Playtime_System.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tr237.tr237_Playtime_System.managers.PlaytimeManager;

public class PlayerSessionListener implements Listener {

    private final PlaytimeManager playtimeManager;

    public PlayerSessionListener(PlaytimeManager playtimeManager) {
        this.playtimeManager = playtimeManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        playtimeManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        playtimeManager.handlePlayerQuit(event.getPlayer());
    }
}