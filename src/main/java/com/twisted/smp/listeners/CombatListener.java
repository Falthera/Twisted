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
            if (canGiveEnergy && antiAbuse.checkAltFarming(killer.getUniqueId(), victim.getUniqueId())) {
                canGiveEnergy = false;
                killer.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Kill reward denied: same-IP farming not permitted (30 min playtime required)."));
            }
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
            victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.7f, 0.4f);
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

        victimData.clearAbilityCooldowns();
        if (victimData.getInstability() > 0) {
            double reduced = victimData.getInstability() * 0.4;
            victimData.subtractInstability(reduced);
            victim.sendMessage(MiniMessage.miniMessage().deserialize(
                "<dark_aqua>Instability reduced by " + (int) reduced + "% on death."));
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
                if (attackerData.getTwist() == Twist.BERSERKER) {
                    org.bukkit.attribute.AttributeInstance maxHealth = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    double maxHp = maxHealth != null ? maxHealth.getValue() : 20.0;
                    double healthPercent = attacker.getHealth() / maxHp;
                    double lowHealthMult = configManager.getTwistConfig("berserker").getDouble("passive.low-health-damage-multiplier", 2.0);
                    multiplier *= 1.0 + Math.max(0, 1.0 - healthPercent) * (lowHealthMult - 1.0);
                }
            }
            if (victimData.getTwist() == Twist.PHANTOM) {
                org.bukkit.configuration.ConfigurationSection phantomConfig = configManager.getTwistConfig("phantom");
                double armorMult = phantomConfig != null ? phantomConfig.getDouble("passive.armor-effectiveness-multiplier", 0.6) : 0.6;
                multiplier *= armorMult;
            } else if (victimData.getTwist() == Twist.TITAN) {
                multiplier *= 0.7;
            }
            event.setDamage(damage * multiplier);

            if (damage > 0 && attackerData != null && attackerData.isTwistSelected()) {
                antiAbuse.tagCombat(victimData);
                antiAbuse.tagCombat(attackerData);
            }
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            double multiplier = victimData.getTwist() == Twist.VOID ? 1.5 : 1.0;
            event.setDamage(damage * multiplier);

            Player shooter = getProjectileShooter(projectile);
            if (shooter != null) {
                PlayerData shooterData = dataManager.loadPlayerData(shooter.getUniqueId());
                if (shooterData != null && shooterData.isTwistSelected()) {
                    antiAbuse.tagCombat(victimData);
                }
            }
        }
    }

    private Player getProjectileShooter(org.bukkit.entity.Projectile projectile) {
        if (projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        PlayerData data = dataManager.loadPlayerData(shooter.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (data.getTwist() == Twist.TITAN) {
            event.getEntity().setVelocity(event.getEntity().getVelocity().multiply(0.7));
        }

        if (data.getTwist() == Twist.VOID && event.getEntity() instanceof org.bukkit.entity.EnderPearl) {
            org.bukkit.configuration.ConfigurationSection voidConfig = configManager.getTwistConfig("voidwalker");
            double rangeMult = voidConfig != null ? voidConfig.getDouble("passive.pearl-range-multiplier", 2.0) : 2.0;
            double cdReduction = voidConfig != null ? voidConfig.getDouble("passive.pearl-cooldown-reduction", 0.5) : 0.5;
            event.getEntity().setVelocity(event.getEntity().getVelocity().multiply(rangeMult));
            int baseCooldown = ((org.bukkit.entity.EnderPearl) event.getEntity()).getCooldown();
            ((org.bukkit.entity.EnderPearl) event.getEntity()).setCooldown((int) (baseCooldown * (1.0 - cdReduction)));
        }
    }
}
