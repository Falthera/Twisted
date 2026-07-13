package com.twisted.smp.events;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.vfx.HologramManager;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import com.twisted.smp.vfx.SoundDesigner;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class TwistedStorm implements Listener {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;
    private final com.twisted.smp.core.DataManager dataManager;
    private com.twisted.smp.energy.EnergyManager energyManager;
    private com.twisted.smp.energy.InstabilityManager instabilityManager;
    private TwistedStormEffectListener effectListener;
    private org.bukkit.scheduler.BukkitTask task;
    private org.bukkit.scheduler.BukkitTask lightningTask;
    private int timeRemaining;
    private boolean active = false;
    private org.bukkit.scheduler.BukkitTask hologramRefreshTask;
    private org.bukkit.scheduler.BukkitTask ambientTask;

    public TwistedStorm(TwistedSMP plugin, ConfigManager configManager, com.twisted.smp.core.DataManager dataManager,
                        com.twisted.smp.energy.EnergyManager energyManager, com.twisted.smp.energy.InstabilityManager instabilityManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.energyManager = energyManager;
        this.instabilityManager = instabilityManager;
    }

    public TwistedStorm(TwistedSMP plugin, ConfigManager configManager, com.twisted.smp.core.DataManager dataManager) {
        this(plugin, configManager, dataManager,
             plugin != null ? plugin.getEnergyManager() : null,
             plugin != null ? plugin.getInstabilityManager() : null);
    }

    public boolean start() {
        if (active) return false;
        active = true;
        timeRemaining = configManager.getEventConfig("storm").getInt("duration", 300);

        double gainMult = configManager.getEventConfig("storm").getDouble("energy-gain-multiplier", 2.0);
        double instabilityMult = configManager.getEventConfig("storm").getDouble("instability-gain-multiplier", 1.5);

        for (World world : Bukkit.getWorlds()) {
            if (configManager.getEventConfig("storm").getBoolean("set-rain", true)) {
                world.setStorm(true);
                world.setThundering(true);
                world.setWeatherDuration(timeRemaining * 20);
                world.setThunderDuration(timeRemaining * 20);
            }
        }

        broadcastStart();

        effectListener = new TwistedStormEffectListener(gainMult, instabilityMult, energyManager, instabilityManager);
        plugin.getServer().getPluginManager().registerEvents(effectListener, plugin);

        startLightning();
        startHolograms();

        task = new org.bukkit.scheduler.BukkitRunnable() {
            public void run() {
                timeRemaining--;
                if (timeRemaining <= 0) {
                    stop();
                    return;
                }

                if (timeRemaining <= 10) {
                    broadcastCountdown();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        return true;
    }

    public boolean stop() {
        if (!active) return false;
        active = false;
        if (effectListener != null) {
            effectListener.deactivate();
            plugin.getServer().getPluginManager().unregisterEvents(effectListener);
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (lightningTask != null) {
            lightningTask.cancel();
            lightningTask = null;
        }
        if (hologramRefreshTask != null) {
            hologramRefreshTask.cancel();
            hologramRefreshTask = null;
        }
        if (ambientTask != null) {
            ambientTask.cancel();
            ambientTask = null;
        }
        for (World world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(0);
            world.setThunderDuration(0);
        }
        plugin.vfx().holograms().clearAll();
        broadcastEnd();
        return true;
    }

    public boolean isActive() {
        return active;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    private void startLightning() {
        final HologramManager holograms = plugin.vfx().holograms();
        final ScreenShake shake = plugin.vfx().shake();
        final SoundDesigner sounds = plugin.vfx().sounds();

        lightningTask = new org.bukkit.scheduler.BukkitRunnable() {
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                int freq = configManager.getEventConfig("storm").getInt("lightning-frequency", 20);
                if (ThreadLocalRandom.current().nextInt(freq) == 0) {
                    for (World world : Bukkit.getWorlds()) {
                        if (world.getPlayers().isEmpty()) continue;
                        Player targetP = world.getPlayers().get(ThreadLocalRandom.current().nextInt(world.getPlayers().size()));
                        Location loc = targetP.getLocation().clone().add(
                            ThreadLocalRandom.current().nextDouble() * 60 - 30,
                            0,
                            ThreadLocalRandom.current().nextDouble() * 60 - 30);
                        world.strikeLightning(loc);
                        shake.shakeNearby(loc, 18, ScreenShake.Intensity.HEAVY);
                        ParticlePatterns.explosion(loc, ParticlePatterns.Color.STORM, 2.5f);
                        for (Player p : world.getPlayers()) {
                            if (p.getLocation().distance(loc) < 40) {
                                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
                                if (p.getLocation().distance(loc) < 15) {
                                    shake.shake(p, ScreenShake.Intensity.HEAVY);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startHolograms() {
        hologramRefreshTask = new org.bukkit.scheduler.BukkitRunnable() {
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location loc = p.getLocation().clone().add(0, 12, 0);
                    plugin.vfx().holograms().spawnTextHologram(loc, "§8§lTWISTED STORM", 25, TextColor.color(0x555555));
                    plugin.vfx().holograms().spawnTextHologram(loc.clone().add(0, -0.5, 0), "§c§l" + timeRemaining + "s", 25, TextColor.color(0xff0000));
                }
            }
        }.runTaskTimer(plugin, 0L, 15L);

        ambientTask = new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (tick % 3 == 0) {
                        Location l = p.getLocation().clone().add(
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 30,
                            10,
                            (ThreadLocalRandom.current().nextDouble() - 0.5) * 30
                        );
                        ParticlePatterns.vortex(l, 8, ParticlePatterns.Color.STORM, 0.4);
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void broadcastStart() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(configManager.getMessage("event-storm-start")));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15, 0, false, false));
            plugin.vfx().shake().shake(p, ScreenShake.Intensity.EXTREME);
            plugin.vfx().sounds().playStormStart(p);
            p.getWorld().playSound(p.getLocation(), Sound.WEATHER_RAIN, 1.5f, 0.5f);
            plugin.vfx().holograms().spawnTextHologram(p.getLocation().clone().add(0, 6, 0), "§8§lTWISTED STORM", 80, TextColor.color(0x555555));
        }
    }

    private void broadcastEnd() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(configManager.getMessage("event-storm-end")));
            p.getWorld().playSound(p.getLocation(), Sound.WEATHER_RAIN, 0.5f, 1.0f);
        }
    }

    private void broadcastCountdown() {
        String template = configManager.getMessage("event-countdown");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(template.replace("%time%", String.valueOf(timeRemaining))));
        }
    }

    public static class TwistedStormEffectListener implements org.bukkit.event.Listener {
        private boolean active = true;
        private final double energyGainMultiplier;
        private final double instabilityGainMultiplier;
        private final com.twisted.smp.energy.EnergyManager energyManager;
        private final com.twisted.smp.energy.InstabilityManager instabilityManager;

        public TwistedStormEffectListener(double energyGainMultiplier, double instabilityGainMultiplier,
                                          com.twisted.smp.energy.EnergyManager energyManager,
                                          com.twisted.smp.energy.InstabilityManager instabilityManager) {
            this.energyGainMultiplier = energyGainMultiplier;
            this.instabilityGainMultiplier = instabilityGainMultiplier;
            this.energyManager = energyManager;
            this.instabilityManager = instabilityManager;
        }

        public void deactivate() {
            this.active = false;
        }

        @org.bukkit.event.EventHandler
        public void onPlayerDeathInStorm(org.bukkit.event.entity.PlayerDeathEvent event) {
            if (!active || energyManager == null || instabilityManager == null) return;

            org.bukkit.entity.Player victim = event.getEntity();
            org.bukkit.entity.Player killer = victim.getKiller();
            if (killer == null) return;

            ParticlePatterns.explosion(victim.getLocation().clone().add(0, 0.5, 0), ParticlePatterns.Color.STORM, 2.0f);
            killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.7f);

            com.twisted.smp.core.PlayerData killerData = energyManager.getPlugin().getDataManager().loadPlayerData(killer.getUniqueId());
            if (killerData == null || !killerData.isTwistSelected()) return;

            double baseReward = energyManager.getKillReward();
            double bonusEnergy = baseReward * (energyGainMultiplier - 1.0);
            if (bonusEnergy > 0) {
                energyManager.addEnergy(killerData, bonusEnergy);
                org.bukkit.entity.Player onlineKiller = org.bukkit.Bukkit.getPlayer(killer.getUniqueId());
                if (onlineKiller != null && onlineKiller.isOnline()) {
                    String msg = energyManager.getConfigManager().getMessage("energy-gain", "amount", "+" + (int) bonusEnergy, "new", String.valueOf((int) killerData.getEnergy()));
                    onlineKiller.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
                }
            }

            double bonusInstability = instabilityManager.getAbilityOverflowInstability() * (instabilityGainMultiplier - 1.0);
            if (bonusInstability > 0) {
                killerData.addInstability(bonusInstability);
                org.bukkit.entity.Player onlineKiller = org.bukkit.Bukkit.getPlayer(killer.getUniqueId());
                if (onlineKiller != null && onlineKiller.isOnline()) {
                    String msg = instabilityManager.getConfigManager().getMessage("instability-gain", "amount", String.valueOf((int) bonusInstability), "new", String.valueOf((int) killerData.getInstability()));
                    onlineKiller.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
                }
            }

            energyManager.getPlugin().getDataManager().savePlayerData(killerData, true);
        }
    }
}
