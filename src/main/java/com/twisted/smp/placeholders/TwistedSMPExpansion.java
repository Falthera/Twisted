package com.twisted.smp.placeholders;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.DataManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.twists.Twist;
import com.twisted.smp.twists.TwistManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TwistedSMPExpansion extends PlaceholderExpansion {

    private final TwistedSMP plugin;
    private final DataManager dataManager;
    private final TwistManager twistManager;

    public TwistedSMPExpansion(TwistedSMP plugin, DataManager dataManager, TwistManager twistManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.twistManager = twistManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "twisted";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TwistedSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data == null) {
            return "0";
        }
        if (params.equalsIgnoreCase("energy")) {
            return String.format("%.0f%%", data.getEnergy());
        }
        if (params.equalsIgnoreCase("essence")) {
            return String.format("%.0f", data.getEssence());
        }
        if (params.equalsIgnoreCase("instability")) {
            return String.format("%.0f%%", data.getInstability());
        }
        if (params.equalsIgnoreCase("stage")) {
            return plugin.getEnergyManager().getStageName(data.getEnergy());
        }
        if (params.equalsIgnoreCase("twist")) {
            return twistManager.getTwistDisplayName(data.getTwist());
        }
        if (params.equalsIgnoreCase("kills")) {
            return String.valueOf(data.getKills());
        }
        if (params.equalsIgnoreCase("deaths")) {
            return String.valueOf(data.getDeaths());
        }
        return null;
    }
}
