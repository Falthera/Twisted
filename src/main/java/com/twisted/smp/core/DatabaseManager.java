package com.twisted.smp.core;

import com.twisted.smp.TwistedSMP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class DatabaseManager {

    private final TwistedSMP plugin;
    private java.sql.Connection connection;
    private final ConcurrentHashMap<UUID, Long> lastUpdate = new ConcurrentHashMap<>();

    public DatabaseManager(TwistedSMP plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean initialize() {
        try {
            String dbPath = new File(plugin.getDataFolder(), "twisted-data.db").getAbsolutePath();
            String url = "jdbc:sqlite:" + dbPath;
            connection = java.sql.DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            try (java.sql.Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        uuid TEXT PRIMARY KEY,
                        twist TEXT NOT NULL DEFAULT 'VOID',
                        energy REAL NOT NULL DEFAULT 0,
                        essence REAL NOT NULL DEFAULT 0,
                        instability REAL NOT NULL DEFAULT 0,
                        evolution_stage INTEGER NOT NULL DEFAULT 1,
                        kills INTEGER NOT NULL DEFAULT 0,
                        deaths INTEGER NOT NULL DEFAULT 0,
                        ability_cooldowns TEXT,
                        last_save REAL NOT NULL,
                        first_join REAL NOT NULL,
                        twist_selected INTEGER NOT NULL DEFAULT 0
                    )
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS ability_cooldowns (
                        uuid TEXT NOT NULL,
                        ability TEXT NOT NULL,
                        cooldown_end REAL NOT NULL,
                        PRIMARY KEY (uuid, ability)
                    )
                """);
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS anti_abuse_data (
                        uuid TEXT PRIMARY KEY,
                        last_kill_uuid TEXT,
                        last_kill_time REAL DEFAULT 0,
                        same_player_kills INTEGER DEFAULT 0,
                        last_ip TEXT,
                        playtime_minutes REAL DEFAULT 0,
                        combat_tag_end REAL DEFAULT 0
                    )
                """);
            }
            plugin.getLogger().info("Database initialized successfully.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized java.sql.Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                String dbPath = new File(plugin.getDataFolder(), "twisted-data.db").getAbsolutePath();
                String url = "jdbc:sqlite:" + dbPath;
                connection = java.sql.DriverManager.getConnection(url);
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to get database connection: " + e.getMessage());
        }
        return connection;
    }

    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    public void updateLastSave(UUID uuid) {
        lastUpdate.put(uuid, System.currentTimeMillis());
    }

    public synchronized void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                plugin.getLogger().warning("Error closing database: " + e.getMessage());
            }
        }
    }

    public void saveAntiAbuseData(UUID uuid, UUID lastKillUuid, long lastKillTime, int samePlayerKills, String lastIp, double playtimeMinutes, double combatTagEnd) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR REPLACE INTO anti_abuse_data (uuid, last_kill_uuid, last_kill_time, same_player_kills, last_ip, playtime_minutes, combat_tag_end)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, lastKillUuid != null ? lastKillUuid.toString() : null);
                ps.setDouble(3, lastKillTime);
                ps.setInt(4, samePlayerKills);
                ps.setString(5, lastIp);
                ps.setDouble(6, playtimeMinutes);
                ps.setDouble(7, combatTagEnd);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save anti-abuse data: " + e.getMessage());
        }
    }

    public void loadAntiAbuseData(UUID uuid, java.util.function.Consumer<java.util.Map<String, Object>> consumer) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM anti_abuse_data WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        data.put("lastKillUuid", rs.getString("last_kill_uuid"));
                        data.put("lastKillTime", rs.getDouble("last_kill_time"));
                        data.put("samePlayerKills", rs.getInt("same_player_kills"));
                        data.put("lastIp", rs.getString("last_ip"));
                        data.put("playtimeMinutes", rs.getDouble("playtime_minutes"));
                        data.put("combatTagEnd", rs.getDouble("combat_tag_end"));
                        consumer.accept(data);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
