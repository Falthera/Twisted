package com.twisted.smp.anti;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.DatabaseManager;
import com.twisted.smp.core.PlayerData;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AntiAbuseManager {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Long> combatTagged = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastKillTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> killFarmingCount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastKillTime = new ConcurrentHashMap<>();
    private final Map<UUID, Double> playtimeMinutes = new ConcurrentHashMap<>();

    public AntiAbuseManager(TwistedSMP plugin, ConfigManager configManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
    }

    public void loadData(UUID uuid) {
        databaseManager.loadAntiAbuseData(uuid, data -> {
            String lastKillUuidStr = (String) data.get("lastKillUuid");
            if (lastKillUuidStr != null) {
                lastKillTarget.put(uuid, lastKillUuidStr);
            }
            lastKillTime.put(uuid, ((Number) data.get("lastKillTime")).longValue());
            killFarmingCount.put(uuid, ((Number) data.get("samePlayerKills")).intValue());
            playtimeMinutes.put(uuid, ((Number) data.get("playtimeMinutes")).doubleValue());
            double combatTagEnd = ((Number) data.get("combatTagEnd")).doubleValue();
            if (combatTagEnd > System.currentTimeMillis()) {
                combatTagged.put(uuid, (long) combatTagEnd);
            }
        });
    }

    public void saveData(UUID uuid) {
        String killTarget = lastKillTarget.get(uuid);
        UUID killTargetUuid = null;
        if (killTarget != null) {
            try {
                killTargetUuid = UUID.fromString(killTarget);
            } catch (IllegalArgumentException e) {
                killTargetUuid = null;
            }
        }
        databaseManager.saveAntiAbuseData(
            uuid,
            killTargetUuid,
            lastKillTime.getOrDefault(uuid, 0L),
            killFarmingCount.getOrDefault(uuid, 0),
            null,
            playtimeMinutes.getOrDefault(uuid, 0.0),
            combatTagged.getOrDefault(uuid, 0L)
        );
    }

    public void recordKill(UUID killerUuid, UUID victimUuid) {
        lastKillTarget.put(killerUuid, victimUuid.toString());
        lastKillTime.put(killerUuid, System.currentTimeMillis());
        killFarmingCount.merge(killerUuid, 1, Integer::sum);
        saveData(killerUuid);
    }

    public boolean checkKill(UUID killerUuid, UUID victimUuid) {
        if (!configManager.getConfig().getBoolean("anti-abuse.kill-farming.enabled", true)) {
            return true;
        }

        int cooldown = configManager.getConfig().getInt("anti-abuse.kill-farming.cooldown", 60) * 1000;
        long now = System.currentTimeMillis();

        if (lastKillTarget.containsKey(killerUuid)) {
            String victimStr = lastKillTarget.get(killerUuid);
            try {
                UUID lastVictimUuid = UUID.fromString(victimStr);
                if (victimUuid.equals(lastVictimUuid)) {
                    long lastKill = lastKillTime.getOrDefault(killerUuid, 0L);
                    if (now - lastKill < cooldown) {
                        int count = killFarmingCount.merge(killerUuid, 1, Integer::sum);
                        if (count >= configManager.getConfig().getInt("anti-abuse.kill-farming.max-kills-before-penalty", 3)) {
                            return false;
                        }
                    } else {
                        killFarmingCount.put(killerUuid, 1);
                    }
                } else {
                    lastKillTarget.put(killerUuid, victimUuid.toString());
                    killFarmingCount.put(killerUuid, 1);
                }
            } catch (IllegalArgumentException e) {
                lastKillTarget.put(killerUuid, victimUuid.toString());
                killFarmingCount.put(killerUuid, 1);
            }
        } else {
            lastKillTarget.put(killerUuid, victimUuid.toString());
            killFarmingCount.put(killerUuid, 1);
        }

        return true;
    }

    public void tagCombat(PlayerData data) {
        int duration = configManager.getConfig().getInt("anti-abuse.combat-logging.tag-duration", 10) * 1000;
        combatTagged.put(data.getUuid(), System.currentTimeMillis() + duration);
    }

    public boolean isCombatTagged(UUID uuid) {
        Long end = combatTagged.get(uuid);
        if (end == null) return false;
        if (end <= System.currentTimeMillis()) {
            combatTagged.remove(uuid);
            return false;
        }
        return true;
    }

    public double getDeathPenalty(UUID uuid) {
        return configManager.getConfig().getDouble("anti-abuse.combat-logging.energy-penalty", -15);
    }

    public double getInstabilityPenalty(UUID uuid) {
        return configManager.getConfig().getDouble("anti-abuse.combat-logging.instability-penalty", 10);
    }

    public void applyCombatLogPenalty(PlayerData data) {
        data.subtractEnergy(Math.abs(getDeathPenalty(data.getUuid())));
        data.addInstability(getInstabilityPenalty(data.getUuid()));
        plugin.getDataManager().savePlayerData(data, true);
    }

    public void addPlaytime(UUID uuid, double minutes) {
        playtimeMinutes.merge(uuid, minutes, Double::sum);
    }

    public double getPlaytimeMinutes(UUID uuid) {
        return playtimeMinutes.getOrDefault(uuid, 0.0);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        combatTagged.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
