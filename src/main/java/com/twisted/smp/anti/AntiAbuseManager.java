package com.twisted.smp.anti;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.DatabaseManager;
import com.twisted.smp.core.PlayerData;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class AntiAbuseManager {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Long> combatTagged = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastKillTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> killFarmingCount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastKillTime = new ConcurrentHashMap<>();

    public AntiAbuseManager(TwistedSMP plugin, ConfigManager configManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
    }

    public boolean checkKill(UUID killerUuid, UUID victimUuid) {
        if (!configManager.getConfig().getBoolean("anti-abuse.kill-farming.enabled", true)) {
            return true;
        }

        int cooldown = configManager.getConfig().getInt("anti-abuse.kill-farming.cooldown", 60) * 1000;
        long now = System.currentTimeMillis();

        if (lastKillTarget.containsKey(killerUuid) &&
            victimUuid.equals(UUID.fromString(lastKillTarget.get(killerUuid)))) {
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

        return true;
    }

    public void recordKill(UUID killerUuid, UUID victimUuid) {
        lastKillTarget.put(killerUuid, victimUuid.toString());
        lastKillTime.put(killerUuid, System.currentTimeMillis());
        killFarmingCount.merge(killerUuid, 1, Integer::sum);
    }

    public void tagCombat(PlayerData data) {
        int duration = configManager.getConfig().getInt("anti-abuse.combat-logging.tag-duration", 10) * 1000;
        combatTagged.put(data.getUuid(), System.currentTimeMillis() + duration);
    }

    public boolean isCombatTagged(UUID uuid) {
        Long end = combatTagged.get(uuid);
        return end != null && end > System.currentTimeMillis();
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

    public void tick() {
        long now = System.currentTimeMillis();
        combatTagged.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
