package com.twisted.smp.core;

import com.twisted.smp.TwistedSMP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class DatabaseManager {

    private final TwistedSMP plugin;
    private java.sql.Connection connection;
    private final ConcurrentHashMap<UUID, Long> lastUpdate = new ConcurrentHashMap<>();

    public DatabaseManager(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
    }

    public synchronized boolean initialize() {
        try {
            String dbPath = new File(plugin.getDataFolder(), "twisted-data.db").getAbsolutePath();
            Class.forName("org.sqlite.JDBC");
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
}
