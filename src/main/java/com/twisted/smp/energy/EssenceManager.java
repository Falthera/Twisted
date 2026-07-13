package com.twisted.smp.energy;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import java.util.UUID;
import org.bukkit.entity.Player;

public class EssenceManager {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;

    public EssenceManager(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public double getStartingEssence() {
        return configManager.getConfig().getDouble("essence.starting-essence", 0);
    }

    public double getTwistChangeCost() {
        return configManager.getConfig().getDouble("essence.twist-change-cost", 100);
    }

    public boolean spendEssence(PlayerData data, double amount) {
        if (data.getEssence() < amount) {
            return false;
        }
        data.subtractEssence(amount);
        return true;
    }

    public void awardEssence(PlayerData data, double amount) {
        data.addEssence(amount);
        notifyEssenceChange(data.getUuid(), "+" + amount, "essence-gain", (int) data.getEssence());
    }

    public boolean canAffordTwistChange(double amount) {
        return amount >= getTwistChangeCost();
    }

    public boolean canAffordEvolution(double amount) {
        return amount > 0;
    }

    private void notifyEssenceChange(UUID uuid, String message, String messagePath, int newTotal) {
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            String msg = configManager.getMessage(messagePath, "amount", message, "new", String.valueOf(newTotal));
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
        }
    }
}
