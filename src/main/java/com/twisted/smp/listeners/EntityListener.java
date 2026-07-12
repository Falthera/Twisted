package com.twisted.smp.listeners;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
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
    private final ConfigManager configManager;

    public EntityListener(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onEnderTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (data.getTwist() == com.twisted.smp.twists.Twist.VOID) {
            double damageMult = configManager.getTwistConfig("voidwalker").getDouble("passive.pearl-damage-multiplier", 1.5);
            org.bukkit.event.entity.EntityDamageEvent eventDamage = new org.bukkit.event.entity.EntityDamageEvent(
                player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL, (float) (1.0 * damageMult));
            player.setLastDamageCause(eventDamage);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
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
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (data.getTwist() == com.twisted.smp.twists.Twist.VOID) {
            event.getEntity().playSound(event.getEntity().getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.8f);
        }
    }
}
