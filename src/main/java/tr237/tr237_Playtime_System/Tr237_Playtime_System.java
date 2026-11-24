package tr237.tr237_Playtime_System;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import tr237.tr237_Playtime_System.commands.PlaytimeCommand;
import tr237.tr237_Playtime_System.commands.PlaytimeTabCompleter;
import tr237.tr237_Playtime_System.database.DatabaseManager;
import tr237.tr237_Playtime_System.listeners.PlayerSessionListener;
import tr237.tr237_Playtime_System.managers.PlaytimeManager;

import java.sql.SQLException;

public class Tr237_Playtime_System extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlaytimeManager playtimeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        try {
            this.databaseManager.initialize();
        } catch (SQLException e) {
            getLogger().severe("Could not initialize SQLite database! Disabling plugin...");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.playtimeManager = new PlaytimeManager(this, databaseManager);
        this.playtimeManager.loadAllData();
        this.playtimeManager.startAutoSaveTask();

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(playtimeManager), this);

        PluginCommand ptCommand = getCommand("playtime");
        if (ptCommand != null) {
            ptCommand.setExecutor(new PlaytimeCommand(this, playtimeManager));
            ptCommand.setTabCompleter(new PlaytimeTabCompleter());
        }

        getLogger().info("tr237_Playtime_System enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (playtimeManager != null) {
            playtimeManager.saveAllSyncOnShutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("tr237_Playtime_System disabled safely.");
    }
}