package com.twisted.smp.vfx;

import com.twisted.smp.TwistedSMP;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class ScreenShake {

    private final TwistedSMP plugin;

    public ScreenShake(TwistedSMP plugin) {
        this.plugin = plugin;
    }

    public void shake(Player player, Intensity intensity) {
        if (player == null || !player.isOnline()) return;

        int ticks = intensity.ticks;
        double strength = intensity.strength;
        long delayStep = 1;

        for (int i = 0; i < ticks; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || player.isDead()) {
                        cancel();
                        return;
                    }
                    double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * strength;
                    double offsetY = (ThreadLocalRandom.current().nextDouble() - 0.5) * strength * 0.8;
                    double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * strength;
                    Vector v = new Vector(offsetX, offsetY + 0.15, offsetZ);
                    player.setVelocity(v);
                }
            }.runTaskLater(plugin, i * delayStep);
        }
    }

    public void shakeNearby(Location center, double radius, Intensity intensity) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getWorld().equals(center.getWorld()) && p.getLocation().distance(center) <= radius) {
                shake(p, intensity);
            }
        }
    }

    public void shakeWorld(org.bukkit.World world, Intensity intensity) {
        for (Player p : world.getPlayers()) {
            shake(p, intensity);
        }
    }

    public enum Intensity {
        LIGHT(4, 0.04),
        MEDIUM(8, 0.10),
        HEAVY(14, 0.18),
        EXTREME(22, 0.32),
        CINEMATIC(30, 0.50);

        final int ticks;
        final double strength;

        Intensity(int ticks, double strength) {
            this.ticks = ticks;
            this.strength = strength;
        }
    }
}
