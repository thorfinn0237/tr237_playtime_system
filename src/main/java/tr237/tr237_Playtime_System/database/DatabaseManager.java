package tr237.tr237_Playtime_System.database;

import org.bukkit.plugin.Plugin;
import tr237.tr237_Playtime_System.models.PlayerPlaytime;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    private final Plugin plugin;
    private final String jdbcUrl;
    private Connection connection;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        File dbFile = new File(plugin.getDataFolder(), "playtime.db");
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public synchronized void initialize() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data directory.");
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found.", e);
        }

        this.connection = DriverManager.getConnection(jdbcUrl);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_playtime (
                        uuid VARCHAR(36) PRIMARY KEY,
                        username VARCHAR(16) NOT NULL,
                        playtime_seconds BIGINT NOT NULL DEFAULT 0
                    );
                    """);
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(jdbcUrl);
        }
        return connection;
    }

    public synchronized Map<UUID, PlayerPlaytime> loadAll() {
        Map<UUID, PlayerPlaytime> loaded = new HashMap<>();
        String sql = "SELECT uuid, username, playtime_seconds FROM player_playtime;";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String username = rs.getString("username");
                    long seconds = rs.getLong("playtime_seconds");
                    loaded.put(uuid, new PlayerPlaytime(uuid, username, seconds));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load playtime records: " + e.getMessage());
        }

        return loaded;
    }

    public synchronized void savePlayer(UUID uuid, String username, long totalSeconds) {
        String sql = """
                INSERT INTO player_playtime (uuid, username, playtime_seconds)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    username = excluded.username,
                    playtime_seconds = excluded.playtime_seconds;
                """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setLong(3, Math.max(0L, totalSeconds));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save playtime for " + username + ": " + e.getMessage());
        }
    }

    public synchronized void saveBatch(Collection<PlayerPlaytime> records, boolean keepSessionActive) {
        if (records.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO player_playtime (uuid, username, playtime_seconds)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    username = excluded.username,
                    playtime_seconds = excluded.playtime_seconds;
                """;

        try {
            Connection conn = getConnection();
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (PlayerPlaytime data : records) {
                    data.flushSession(keepSessionActive);
                    ps.setString(1, data.getUuid().toString());
                    ps.setString(2, data.getUsername());
                    ps.setLong(3, data.getStoredSeconds());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to batch save playtime records: " + e.getMessage());
        }
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing SQLite connection: " + e.getMessage());
            }
        }
    }
}