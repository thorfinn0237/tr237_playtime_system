package tr237.tr237_Playtime_System.models;

import java.util.UUID;

public class PlayerPlaytime {

    private final UUID uuid;
    private String username;
    private long storedSeconds;
    private long sessionStartMillis;

    public PlayerPlaytime(UUID uuid, String username, long storedSeconds) {
        this.uuid = uuid;
        this.username = username;
        this.storedSeconds = Math.max(0L, storedSeconds);
        this.sessionStartMillis = 0L;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username != null && !username.isBlank()) {
            this.username = username;
        }
    }

    public synchronized long getStoredSeconds() {
        return storedSeconds;
    }

    public synchronized void setStoredSeconds(long seconds) {
        this.storedSeconds = Math.max(0L, seconds);
    }

    public synchronized void startSession() {
        if (this.sessionStartMillis == 0L) {
            this.sessionStartMillis = System.currentTimeMillis();
        }
    }

    public synchronized long getActiveSessionSeconds() {
        if (this.sessionStartMillis <= 0L) {
            return 0L;
        }
        return Math.max(0L, (System.currentTimeMillis() - this.sessionStartMillis) / 1000L);
    }

    public synchronized long getTotalSeconds() {
        return this.storedSeconds + getActiveSessionSeconds();
    }

    /**
     * Flushes elapsed session time into storedSeconds and resets the session timer
     * if the player is still online.
     */
    public synchronized void flushSession(boolean keepSessionActive) {
        if (this.sessionStartMillis > 0L) {
            long elapsed = Math.max(0L, (System.currentTimeMillis() - this.sessionStartMillis) / 1000L);
            this.storedSeconds += elapsed;
            this.sessionStartMillis = keepSessionActive ? System.currentTimeMillis() : 0L;
        }
    }
}