package com.twisted.smp.listeners;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.anti.AntiAbuseManager;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.DataManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.twists.Twist;
import com.twisted.smp.twists.TwistManager;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import com.twisted.smp.vfx.SoundDesigner;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.concurrent.ThreadLocalRandom;

public class CombatListener implements Listener {

    private final TwistedSMP plugin;
    private final DataManager dataManager;
    private final TwistManager twistManager;
    private final AntiAbuseManager antiAbuse;
    private final ConfigManager configManager;

    public CombatListener(TwistedSMP plugin, DataManager dataManager, TwistManager twistManager,
                          AntiAbuseManager antiAbuse, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.twistManager = twistManager;
        this.antiAbuse = antiAbuse;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        PlayerData victimData = dataManager.getOrCreatePlayerData(victim.getUniqueId());

        if (killer != null) {
            PlayerData killerData = dataManager.getOrCreatePlayerData(killer.getUniqueId());

            boolean canGiveEnergy = antiAbuse.checkKill(killer.getUniqueId(), victim.getUniqueId());
            double energyGain = configManager.getConfig().getDouble("energy.kill-reward", 25);
            double energyLoss = configManager.getConfig().getDouble("energy.death-penalty", -35);

            Location deathLoc = victim.getLocation().clone().add(0, 0.5, 0);

            ParticlePatterns.explosion(deathLoc, killerData.isTwistSelected() && killerData.getTwist() == Twist.INFERNAL
                ? ParticlePatterns.Color.INFERNO : ParticlePatterns.Color.BLOOD, 2.0f);
            plugin.vfx().shake().shakeNearby(deathLoc, 12, ScreenShake.Intensity.HEAVY);
            plugin.vfx().sounds().playDeathImpact(victim, killer);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.6f, 0.5f);

            ParticlePatterns.ringBurst(deathLoc, 1.8, 24, ParticlePatterns.Color.BLOOD, 1.2f);
            victim.getWorld().spawnParticle(Particle.LARGE_SMOKE, deathLoc, 12, 0.6, 0.6, 0.6, 0.04);
            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, deathLoc, 10, 0.8, 0.8, 0.8, 0);

            if (killerData.isTwistSelected()) {
                String twistColor = switch (killerData.getTwist()) {
                    case VOID -> "§5";
                    case TITAN -> "§8";
                    case BERSERKER -> "§c";
                    case PHANTOM -> "§f";
                    case INFERNAL -> "§6";
                    case FROSTBORN -> "§b";
                };
                plugin.vfx().holograms().spawnTextHologram(deathLoc.clone().add(0, 1.2, 0),
                    twistColor + "☠ " + killer.getName() + " §8× " + victim.getName(), 50, net.kyori.adventure.text.format.TextColor.color(0xbbbbbb));
            }

            if (canGiveEnergy) {
                plugin.getEnergyManager().addEnergy(killerData, energyGain);
                killerData.addKill();
                killer.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getMessage("kill-player", "victim", victim.getName(), "energy", String.valueOf((int) energyGain))));
            } else {
                double reduced = energyGain * 0.5;
                plugin.getEnergyManager().addEnergy(killerData, reduced);
                killerData.addKill();
                killer.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getMessage("kill-farming-warn")));
            }

            plugin.getEnergyManager().subtractEnergy(victimData, Math.abs(energyLoss));
            victimData.addDeath();
            victim.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getMessage("killed-by-player", "killer", killer.getName(), "energy", String.valueOf((int) Math.abs(energyLoss)))));

            dataManager.savePlayerData(killerData, true);
            antiAbuse.recordKill(killer.getUniqueId(), victim.getUniqueId());
        } else {
            double penalty = configManager.getConfig().getDouble("energy.normal-death-penalty", -25);
            plugin.getEnergyManager().subtractEnergy(victimData, Math.abs(penalty));
            victim.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getMessage("death-penalty", "energy", String.valueOf((int) Math.abs(penalty)))));
        }

        if (plugin.getEvolutionManager().canEvolve(victimData)) {
            victim.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getMessage("evolution-available")));
        }

        dataManager.savePlayerData(victimData, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        PlayerData victimData = dataManager.loadPlayerData(victim.getUniqueId());
        if (victimData == null || !victimData.isTwistSelected()) return;

        double damage = event.getDamage();

        if (event.getDamager() instanceof Player attacker) {
            PlayerData attackerData = dataManager.loadPlayerData(attacker.getUniqueId());
            double multiplier = 1.0;
            if (attackerData != null && attackerData.isTwistSelected()) {
                multiplier *= plugin.getEnergyManager().getEffectivenessMultiplier(attackerData.getTwist(), attackerData.getEnergy());
            }
            if (victimData.getTwist() == Twist.PHANTOM) {
                multiplier *= 0.6;
            } else if (victimData.getTwist() == Twist.TITAN) {
                multiplier *= 0.7;
            }
            event.setDamage(damage * multiplier);
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            double multiplier = victimData.getTwist() == Twist.VOID ? 1.5 : 1.0;
            event.setDamage(damage * multiplier);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        PlayerData data = dataManager.loadPlayerData(shooter.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (data.getTwist() == Twist.TITAN) {
            event.getEntity().setVelocity(event.getEntity().getVelocity().multiply(0.7));
        }
    }
}
