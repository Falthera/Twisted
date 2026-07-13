package com.twisted.smp.evolution;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.twists.Twist;
import com.twisted.smp.vfx.HologramManager;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import com.twisted.smp.vfx.SoundDesigner;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;

public class EvolutionManager {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;

    public EvolutionManager(TwistedSMP plugin, ConfigManager configManager, com.twisted.smp.core.DataManager dataManager, com.twisted.smp.twists.TwistManager twistManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean canEvolve(PlayerData data) {
        int stage = data.getEvolutionStage();
        if (stage >= 3) return false;

        Twist twist = data.getTwist();
        int nextStage = stage + 1;

        int energyReq = twist.getEvolutionEnergyReq(nextStage);
        int essenceReq = twist.getEvolutionEssenceReq(nextStage);

        if (data.getEnergy() < energyReq) return false;
        if (data.getEssence() < essenceReq) return false;

        return true;
    }

    public boolean evolve(PlayerData data) {
        if (!canEvolve(data)) return false;

        int nextStage = data.getEvolutionStage() + 1;
        Twist twist = data.getTwist();

        int energyReq = twist.getEvolutionEnergyReq(nextStage);
        int essenceReq = twist.getEvolutionEssenceReq(nextStage);

        data.setEnergy(Math.max(data.getEnergy() - energyReq, 0));
        data.subtractEssence(essenceReq);
        data.setEvolutionStage(nextStage);

        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(data.getUuid());
        if (player != null && player.isOnline()) {
            String newName = twist.getEvolutionName(nextStage);
            String emoji = nextStage >= 3 ? "§6§l★ §f§lEVOLUTION ★ §6§l" : "§e§lEVOLUTION";

            HologramManager holo = plugin.vfx().holograms();
            ScreenShake shake = plugin.vfx().shake();
            SoundDesigner sounds = plugin.vfx().sounds();
            Location loc = player.getLocation();

            shake.shake(player, nextStage >= 3 ? ScreenShake.Intensity.CINEMATIC : ScreenShake.Intensity.HEAVY);
            shake.shakeNearby(loc, 20, nextStage >= 3 ? ScreenShake.Intensity.HEAVY : ScreenShake.Intensity.MEDIUM);
            sounds.playEvolutionSound(loc, nextStage >= 3);

            ParticlePatterns.explosion(loc.clone().add(0, 1, 0), ParticlePatterns.Color.EVOLUTION, nextStage >= 3 ? 3.0f : 2.0f);
            holo.spawnEvolutionPillar(loc, nextStage >= 3 ? 80 : 50);
            holo.spawnOrbitalText(loc.clone().add(0, 2.5, 0), "§6§l✦ §f§l" + newName + " §6§l✦", 3, 70);
            holo.spawnTextHologram(loc.clone().add(0, 4.0, 0), emoji, 60, ParticlePatterns.Color.EVOLUTION.toAdventure());

            player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 50, 1.5, 2.0, 1.5, 0.05);
            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.clone().add(0, 1, 0), 30, 1.0, 2.5, 1.0, 0.02);
            player.getWorld().spawnParticle(org.bukkit.Particle.FLASH, loc.clone().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);

            net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.text("EVOLVED!")
                .color(net.kyori.adventure.text.format.TextColor.color(0xfdcb6e));
            net.kyori.adventure.text.Component subtitle = net.kyori.adventure.text.Component.text(newName)
                .color(net.kyori.adventure.text.format.TextColor.color(0xffffff));
            player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofMillis(250))));

            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(configManager.getMessage("evolution-success", "new_name", newName)));

            plugin.getPlayerListener().refreshPassiveEffects(player, data);

            if (nextStage == 3) {
                hologramTitleRing(player, loc);
            }
        }

        plugin.getDataManager().savePlayerData(data, true);
        return true;
    }

    private void hologramTitleRing(Player player, Location loc) {
        int particlesPerRing = 20;
        int rings = 3;

        for (int ring = 0; ring < rings; ring++) {
            final int ringIndex = ring;
            new org.bukkit.scheduler.BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    double radius = 1.5 + ringIndex * 0.9;
                    for (int i = 0; i < particlesPerRing; i++) {
                        double angle = (2 * Math.PI / particlesPerRing) * i - (tick * 0.08);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        double y = 1.2 + Math.sin(tick * 0.15 + i) * 0.3;

                        Location l = loc.clone().add(x, y, z);
                        l.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, l, 1, 0.02, 0.02, 0.02, 0);
                    }
                    tick++;
                }
            }.runTaskTimer(plugin, ring * 3L, 1L);
        }
    }

    public EvolutionRequirements getRequirements(PlayerData data) {
        int stage = data.getEvolutionStage();
        if (stage >= 3) return EvolutionRequirements.maxed();

        Twist twist = data.getTwist();
        int nextStage = stage + 1;
        return new EvolutionRequirements(
            twist.getEvolutionEnergyReq(nextStage),
            twist.getEvolutionEssenceReq(nextStage),
            data.getEnergy() >= twist.getEvolutionEnergyReq(nextStage),
            data.getEssence() >= twist.getEvolutionEssenceReq(nextStage)
        );
    }

    public record EvolutionRequirements(int energyRequired, int essenceRequired,
                                        boolean hasEnoughEnergy, boolean hasEnoughEssence) {
        public boolean meetsRequirements() {
            return hasEnoughEnergy && hasEnoughEssence;
        }
        public static EvolutionRequirements maxed() {
            return new EvolutionRequirements(0, 0, true, true);
        }
    }
}
