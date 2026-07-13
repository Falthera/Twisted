package com.twisted.smp.listeners;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.DataManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.twists.Twist;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityTeleportEvent;

public class EntityListener implements Listener {

    private final TwistedSMP plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final com.twisted.smp.twists.TwistManager twistManager;
    private final com.twisted.smp.abilities.AbilityManager abilityManager;

    public EntityListener(TwistedSMP plugin, DataManager dataManager, com.twisted.smp.twists.TwistManager twistManager, com.twisted.smp.abilities.AbilityManager abilityManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = plugin.getConfigManager();
        this.twistManager = twistManager;
        this.abilityManager = abilityManager;
    }

    @EventHandler
    public void onEnderTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityTeleportEvent.TeleportCause.ENDER_PEARL) return;
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (data.getTwist() == com.twisted.smp.twists.Twist.VOID) {
            double damageMult = configManager.getTwistConfig("voidwalker").getDouble("passive.pearl-damage-multiplier", 1.5);
            double damage = 1.0 * damageMult;
            player.damage(damage);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (data.getTwist() == com.twisted.smp.twists.Twist.VOID) {
                double fallMult = configManager.getTwistConfig("voidwalker").getDouble("passive.fall-damage-multiplier", 1.5);
                event.setDamage(event.getDamage() * fallMult);
            }
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            if (data.getTwist() == com.twisted.smp.twists.Twist.INFERNAL) {
                event.setCancelled(true);
            }
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE) {
            if (data.getTwist() == com.twisted.smp.twists.Twist.FROSTBORN) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (data.getTwist() == com.twisted.smp.twists.Twist.VOID) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.8f);
        }
    }
}
