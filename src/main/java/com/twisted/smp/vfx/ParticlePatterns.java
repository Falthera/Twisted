package com.twisted.smp.vfx;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class ParticlePatterns {

    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();

    public static void spiral(Location center, double radius, double height, int particles, Color color, double speed) {
        for (int i = 0; i < particles; i++) {
            double angle = (2 * Math.PI / particles) * i + (center.getWorld().getGameTime() * speed);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = (height / particles) * i;

            Location l = center.clone().add(x, y, z);
            DustOptions dust = new DustOptions(color.toBukkit(), 1.0f + (float) (i / (double) particles) * 1.0f);
            center.getWorld().spawnParticle(Particle.DUST, l, 1, 0.02, 0.02, 0.02, 0, dust);
        }
    }

    public static void ringBurst(Location center, double radius, int particles, Color color, float size) {
        for (int i = 0; i < particles; i++) {
            double angle = (2 * Math.PI / particles) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location l = center.clone().add(x, 0.15, z);
            DustOptions dust = new DustOptions(color.toBukkit(), size);
            center.getWorld().spawnParticle(Particle.DUST, l, 2, 0.03, 0.03, 0.03, 0, dust);
            center.getWorld().spawnParticle(Particle.SPELL_MOB_AMBIENT, l, 1, 0.01, 0.01, 0.01, 0);
        }
    }

    public static void vortex(Location center, int particles, Color color, double strength) {
        for (int i = 0; i < particles; i++) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double radius = 0.3 + ThreadLocalRandom.current().nextDouble() * 1.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = ThreadLocalRandom.current().nextDouble() * 2.0;

            Location l = center.clone().add(x, y, z);
            DustOptions dust = new DustOptions(color.toBukkit(), 1.2f);
            center.getWorld().spawnParticle(Particle.DUST, l, 1, 0.02, 0.02, 0.02, 0, dust);
            center.getWorld().spawnParticle(Particle.SPELL_MOB_AMBIENT, l, 1, 0.01, 0.01, 0.01, 0);
        }
    }

    public static void explosion(Location center, Color color, float power) {
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.FLASH, center, 2, 0.5, 0.5, 0.5, 0);
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 20, 1.5, 1.5, 1.5, 0.05);
        center.getWorld().spawnParticle(Particle.FLAME, center, 30, 1.0, 1.0, 1.0, 0.08);
        center.getWorld().spawnParticle(Particle.LAVA, center, 8, 1.0, 0.5, 1.0, 0.02);

        int ringCount = 24;
        for (int r = 1; r <= 3; r++) {
            ringBurst(center, r * 1.2, ringCount, color, power / r);
        }
    }

    public static void trailLine(Location from, Location to, int points, Color color, float size, boolean arc) {
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            Location l = from.clone().add(
                (to.getX() - from.getX()) * t,
                (to.getY() - from.getY()) * t + (arc ? Math.sin(t * Math.PI) * 0.6 : 0),
                (to.getZ() - from.getZ()) * t
            );
            DustOptions dust = new DustOptions(color.toBukkit(), size);
            from.getWorld().spawnParticle(Particle.DUST, l, 1, 0.02, 0.02, 0.02, 0, dust);
        }
    }

    public static void trail(Location location, Color color) {
        DustOptions dust = new DustOptions(color.toBukkit(), 1.0f);
        location.getWorld().spawnParticle(Particle.DUST, location, 2, 0.1, 0.1, 0.1, 0.01, dust);
        location.getWorld().spawnParticle(Particle.SPELL_MOB_AMBIENT, location, 1, 0.05, 0.05, 0.05, 0);
    }

    public static void verticalPillar(Location base, double height, Color color, int durationTicks, org.bukkit.plugin.java.JavaPlugin plugin) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= durationTicks) {
                    cancel();
                    return;
                }
                for (double y = 0.5; y < height; y += 0.35) {
                    Location l = base.clone().add(
                        RNG.nextGaussian() * 0.15,
                        y,
                        RNG.nextGaussian() * 0.15
                    );
                    DustOptions dust = new DustOptions(color.toBukkit(), 1.3f);
                    base.getWorld().spawnParticle(Particle.DUST, l, 2, 0.03, 0.03, 0.03, 0, dust);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void damageNumber(Location location, double amount, Color color, org.bukkit.plugin.java.JavaPlugin plugin) {
        String text = String.format("%.1f", amount);
        org.bukkit.entity.TextDisplay display = (org.bukkit.entity.TextDisplay) location.getWorld().spawnEntity(
            location.clone().add(0, 2.6, 0), org.bukkit.entity.EntityType.TEXT_DISPLAY
        );
        if (display == null) return;

        net.kyori.adventure.text.Component comp = net.kyori.adventure.text.Component.text(text)
            .color(color.toAdventure())
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
        display.customName(comp);
        display.setCustomNameVisible(true);
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        display.setShadowRadius(0.15f);
        display.setShadowStrength(1.0f);

        new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 25) {
                    if (display.isValid()) display.remove();
                    cancel();
                    return;
                }
                double yOffset = tick * 0.07;
                display.teleport(display.getLocation().clone().add(0, 0.07, 0));
                if (tick > 8) {
                    display.setShadowStrength(Math.max(0, 1.0f - (tick - 8) * 0.05f));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public enum Color {
        VOID(0x6c5ce7),
        EARTH(0xa0522d),
        BLOOD(0xe74c3c),
        PHANTOM(0xdfe6e9),
        INFERNO(0xff6b35),
        FROST(0x74b9ff),
        STORM(0xf9ca24),
        RIFT(0xa29bfe),
        EVOLUTION(0xfdcb6e),
        ESSENCE(0xffeaa7);

        final int hex;
        Color(int hex) { this.hex = hex; }

        public org.bukkit.Color toBukkit() {
            return org.bukkit.Color.fromRGB(hex);
        }

        public net.kyori.adventure.text.format.TextColor toAdventure() {
            return net.kyori.adventure.text.format.TextColor.color(hex);
        }
    }

    public enum ParticleType {
        VOID_STEP(org.bukkit.Particle.PORTAL, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT),
        EARTHQUAKE(org.bukkit.Particle.CRIT, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE),
        BLOOD_RAGE(org.bukkit.Particle.ANGRY_VILLAGER, org.bukkit.Sound.ENTITY_BLAZE_AMBIENT),
        FADE(org.bukkit.Particle.CLOUD, org.bukkit.Sound.BLOCK_GLASS_BREAK),
        INFERNO(org.bukkit.Particle.FLAME, org.bukkit.Sound.ENTITY_GHAST_SHOOT),
        FREEZE(org.bukkit.Particle.SNOWFLAKE, org.bukkit.Sound.BLOCK_GLASS_BREAK);

        final org.bukkit.Particle particle;
        final org.bukkit.Sound sound;
        ParticleType(org.bukkit.Particle particle, org.bukkit.Sound sound) {
            this.particle = particle;
            this.sound = sound;
        }

        public void play(Location loc, float volume, float pitch) {
            loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0);
            loc.getWorld().playSound(loc, sound, volume, pitch);
        }
    }
}
