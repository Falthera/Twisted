package com.twisted.smp.core;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.twists.Twist;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final TwistedSMP plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> saving = ConcurrentHashMap.newKeySet();

    public DataManager(TwistedSMP plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    public PlayerData loadPlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            return data;
        }

        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_data WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String twistName = rs.getString("twist");
                        Twist twist = Twist.valueOf(twistName.toUpperCase());
                        double energy = rs.getDouble("energy");
                        double essence = rs.getDouble("essence");
                        double instability = rs.getDouble("instability");
                        int evoStage = rs.getInt("evolution_stage");
                        int kills = rs.getInt("kills");
                        int deaths = rs.getInt("deaths");
                        boolean twistSelected = rs.getInt("twist_selected") == 1;
                        long firstJoin = rs.getLong("first_join");

                        Map<String, Long> cooldowns = new HashMap<>();
                        String cooldownsBlob = rs.getString("ability_cooldowns");
                        if (cooldownsBlob != null && !cooldownsBlob.isEmpty()) {
                            for (String part : cooldownsBlob.split(";")) {
                                String[] kv = part.split("=");
                                if (kv.length == 2) {
                                    try {
                                        cooldowns.put(kv[0], Long.parseLong(kv[1]));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }
                        data = new PlayerData(uuid, twist, energy, essence, instability, evoStage, kills, deaths, cooldowns, twistSelected, firstJoin);
                        data.setMaxEnergy(configManager.getConfig().getDouble("energy.max-energy", 200));
                    } else {
                        data = createNewPlayerData(uuid);
                        savePlayerData(data, false);
                    }
                }
            }
            cache.put(uuid, data);
            return data;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load player data for " + uuid + ": " + e.getMessage());
            if (!cache.containsKey(uuid)) {
                data = createNewPlayerData(uuid);
                cache.put(uuid, data);
            }
            return data;
        }
    }

    private PlayerData createNewPlayerData(UUID uuid) {
        long firstJoin = System.currentTimeMillis();
        double startingEnergy = configManager.getConfig().getDouble("energy.starting-energy", 0);
        double startingEssence = configManager.getConfig().getDouble("essence.starting-essence", 0);
        double maxEnergy = configManager.getConfig().getDouble("energy.max-energy", 200);
        PlayerData data = new PlayerData(uuid, Twist.VOID, startingEnergy, startingEssence, 0, 1, 0, 0, new HashMap<>(), false, firstJoin, maxEnergy);
        data.setTwistSelected(false);
        return data;
    }

    public void savePlayerData(PlayerData data, boolean async) {
        if (saving.contains(data.getUuid())) {
            return;
        }
        saving.add(data.getUuid());
        UUID copyUuid = data.getUuid();
        PlayerData snapshot = data.snapshot();

        if (async) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    savePlayerDataSync(snapshot);
                } finally {
                    saving.remove(copyUuid);
                }
            });
        } else {
            try {
                savePlayerDataSync(snapshot);
            } finally {
                saving.remove(copyUuid);
            }
        }
    }

    private void savePlayerDataSync(PlayerData data) {
        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR REPLACE INTO player_data (uuid, twist, energy, essence, instability, evolution_stage, kills, deaths, ability_cooldowns, last_save, first_join, twist_selected)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
                ps.setString(1, data.getUuid().toString());
                ps.setString(2, data.getTwist().name());
                ps.setDouble(3, data.getEnergy());
                ps.setDouble(4, data.getEssence());
                ps.setDouble(5, data.getInstability());
                ps.setInt(6, data.getEvolutionStage());
                ps.setInt(7, data.getKills());
                ps.setInt(8, data.getDeaths());
                StringBuilder cooldownsBuilder = new StringBuilder();
                for (Map.Entry<String, Long> entry : data.getAbilityCooldowns().entrySet()) {
                    if (cooldownsBuilder.length() > 0) cooldownsBuilder.append(";");
                    cooldownsBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                }
                ps.setString(9, cooldownsBuilder.toString());
                ps.setLong(10, System.currentTimeMillis());
                ps.setLong(11, data.getFirstJoin());
                ps.setInt(12, data.isTwistSelected() ? 1 : 0);
                ps.executeUpdate();
            }
            databaseManager.updateLastSave(data.getUuid());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save player data for " + data.getUuid() + ": " + e.getMessage());
        }
    }

    public void saveCooldown(UUID uuid, String ability, long cooldownEnd) {
        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR REPLACE INTO ability_cooldowns (uuid, ability, cooldown_end) VALUES (?, ?, ?)
            """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, ability);
                ps.setLong(3, cooldownEnd);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save cooldown: " + e.getMessage());
        }
    }

    public void loadCooldown(UUID uuid, String ability, java.util.function.Consumer<Long> consumer) {
        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT cooldown_end FROM ability_cooldowns WHERE uuid = ? AND ability = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, ability);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        consumer.accept(rs.getLong("cooldown_end"));
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public void removeCooldown(UUID uuid, String ability) {
        try {
            Connection conn = databaseManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ability_cooldowns WHERE uuid = ? AND ability = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, ability);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData getOrCreatePlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadPlayerData);
    }

    public Map<UUID, PlayerData> getPlayerDataCache() {
        return Collections.unmodifiableMap(cache);
    }

    public void shutdown() {
        for (PlayerData data : cache.values()) {
            savePlayerData(data, false);
        }
        cache.clear();
    }
}
