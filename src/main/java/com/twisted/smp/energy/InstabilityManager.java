package com.twisted.smp.energy;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;

import java.util.Random;

public class InstabilityManager {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    public InstabilityManager(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public double getMaxInstability() {
        return configManager.getConfig().getDouble("instability.max-instability", 100);
    }

    public double getEnergyThreshold() {
        return configManager.getConfig().getDouble("instability.energy-threshold", 150);
    }

    public double getPassiveInstabilityGain() {
        return configManager.getConfig().getDouble("instability.passive-instability-gain", 0);
    }

    public int getEffectTickInterval() {
        return configManager.getConfig().getInt("instability.effect-tick-interval", 60);
    }

    public double getAbilityOverflowInstability() {
        return configManager.getConfig().getDouble("instability.ability-overflow-instability", 5);
    }

    public boolean shouldApplyInstability(PlayerData data) {
        return data.getEnergy() >= getEnergyThreshold();
    }

    public void tickInstability(PlayerData data) {
        if (data.getInstability() <= 0) return;

        double gain = getPassiveInstabilityGain();
        if (gain > 0 && data.getEnergy() >= getEnergyThreshold()) {
            data.addInstability(gain);
        }
    }

    public void addInstabilityForAbilityUse(PlayerData data) {
        if (shouldApplyInstability(data)) {
            data.addInstability(getAbilityOverflowInstability());
        }
    }

    public int getInstabilityLevel(PlayerData data) {
        double instability = data.getInstability();
        if (instability <= 25) return 0;
        if (instability <= 50) return 1;
        if (instability <= 75) return 2;
        return 3;
    }

    public String getInstabilityEffectMessage(PlayerData data) {
        int level = getInstabilityLevel(data);
        return switch (level) {
            case 0 -> "None";
            case 1 -> "Minor corruption: chance of random teleportation and nausea.";
            case 2 -> "Strong corruption: amplified abilities at the cost of health.";
            case 3 -> "Critical corruption: losing control to the void.";
            default -> "Unknown";
        };
    }

    public boolean shouldReduceInstability(PlayerData data) {
        return data.getEnergy() < getEnergyThreshold() && data.getInstability() > 0;
    }

    public void reduceInstability(PlayerData data, double amount) {
        if (shouldReduceInstability(data)) {
            data.addInstability(-amount);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TwistedSMP getPlugin() {
        return plugin;
    }
    public void applyInstabilityEffects(PlayerData data) {
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(data.getUuid());
        if (player == null || !player.isOnline()) return;

        int level = getInstabilityLevel(data);
        if (level >= 1 && random.nextDouble() < 0.08 + level * 0.07) {
            player.damage(1.0);
        }
        if (level >= 2) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH, 40, 0, false, false));
            if (random.nextDouble() < 0.2 + level * 0.15) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WITHER, 20, 0, false, false));
            }
            if (random.nextDouble() < 0.1 + level * 0.1) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NAUSEA, 60, 0, false, false));
            }
        }
        if (level >= 3) {
            if (random.nextDouble() < 0.25 + level * 0.1) {
                org.bukkit.Location loc = player.getLocation().clone().add(
                    (random.nextDouble() - 0.5) * 10,
                    0,
                    (random.nextDouble() - 0.5) * 10);
                if (player.getWorld().getBlockAt(loc).getType().isAir()) {
                    player.teleport(loc);
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        "<dark_red>The void reaches out..."));
                }
            }
            if (random.nextDouble() < 0.15) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 0, false, false));
            }
        }
    }
}
