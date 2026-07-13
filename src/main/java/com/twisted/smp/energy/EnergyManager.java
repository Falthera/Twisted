package com.twisted.smp.energy;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import java.util.UUID;
import com.twisted.smp.twists.Twist;

public class EnergyManager {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;

    public EnergyManager(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public double getMaxEnergy() {
        return configManager.getConfig().getDouble("energy.max-energy", 200);
    }

    public double getStartingEnergy() {
        return configManager.getConfig().getDouble("energy.starting-energy", 0);
    }

    public double getKillReward() {
        return configManager.getConfig().getDouble("energy.kill-reward", 25);
    }

    public double getDeathPenalty() {
        return configManager.getConfig().getDouble("energy.death-penalty", -35);
    }

    public double getNormalDeathPenalty() {
        return configManager.getConfig().getDouble("energy.normal-death-penalty", -25);
    }

    public double getDragonKillReward() {
        return configManager.getConfig().getDouble("energy.dragon-kill-reward", 50);
    }

    public double getWitherKillReward() {
        return configManager.getConfig().getDouble("energy.wither-kill-reward", 40);
    }

    public void awardKillEnergy(PlayerData killer, PlayerData victim) {
        double reward = getKillReward();
        double victimPenalty = getDeathPenalty();

        if (killer.getTwist() == victim.getTwist() && !configManager.getConfig().contains("anti-abuse.kill-farming.same-twist-xp")) {
            // Same twist kills are still tracked by anti-abuse manager but still award energy
        }

        addEnergy(killer, reward);
        subtractEnergy(victim, Math.abs(victimPenalty));

        plugin.getDataManager().savePlayerData(killer, true);
        plugin.getDataManager().savePlayerData(victim, true);
    }

    public void awardDragonDeath(PlayerData player) {
        double reward = getDragonKillReward();
        addEnergy(player, reward);
        plugin.getDataManager().savePlayerData(player, true);
    }

    public void awardWitherDeath(PlayerData player) {
        double reward = getWitherKillReward();
        addEnergy(player, reward);
        plugin.getDataManager().savePlayerData(player, true);
    }

    public boolean addEnergy(PlayerData data, double amount) {
        if (amount < 0) {
            amount = -amount;
        }
        double oldEnergy = data.getEnergy();
        double max = getMaxEnergy();
        double newEnergy = oldEnergy + amount;
        if (newEnergy > max) {
            double overflow = newEnergy - max;
            data.setEnergy(max);
            double ratio = configManager.getConfig().getDouble("essence.overflow-ratio", 1.0);
            data.addEssence(overflow * ratio);
        } else {
            data.setEnergy(newEnergy);
        }
        return true;
    }

    public boolean subtractEnergy(PlayerData data, double amount) {
        data.subtractEnergy(amount);
        return true;
    }

    public void applyDeathPenalty(PlayerData data, boolean wasPVP) {
        if (wasPVP) {
            subtractEnergy(data, Math.abs(getDeathPenalty()));
        } else {
            subtractEnergy(data, Math.abs(getNormalDeathPenalty()));
        }
    }

    public String getStageName(double energy) {
        if (energy <= 50) return "Dormant";
        if (energy <= 100) return "Awakened";
        if (energy <= 150) return "Enhanced";
        if (energy < 200) return "Corrupted";
        return "Ascended";
    }

    public String getStageColor(double energy) {
        if (energy <= 50) return "<gray>";
        if (energy <= 100) return "<green>";
        if (energy <= 150) return "<blue>";
        if (energy < 200) return "<red>";
        return "<gold>";
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TwistedSMP getPlugin() {
        return plugin;
    }

    public double getEffectivenessMultiplier(Twist twist, double energy) {
        return switch (twist) {
            case TITAN -> 1.0 + (energy / getMaxEnergy()) * 0.5;
            case BERSERKER -> 1.0 + (200 - Math.min(energy, 200)) / 200 * 1.5;
            case PHANTOM -> 1.0 + (energy / getMaxEnergy()) * 0.3;
            case INFERNAL -> 1.0 + (energy / getMaxEnergy()) * 0.4;
            case FROSTBORN -> 1.0 + (energy / getMaxEnergy()) * 0.35;
            case VOID -> 1.0 + (energy / getMaxEnergy()) * 0.45;
            default -> 1.0;
        };
    }

    private void notifyEnergyChange(UUID uuid, String message, String messagePath, int newTotal) {
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            String msg = messagePath.isEmpty() ? message : configManager.getMessage(messagePath, "amount", message, "new", String.valueOf(newTotal));
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
        }
    }
}
