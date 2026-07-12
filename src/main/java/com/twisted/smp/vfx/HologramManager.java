package com.twisted.smp.vfx;

import com.twisted.smp.TwistedSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final TwistedSMP plugin;
    private final Map<UUID, List<Display>> activeHolograms = new ConcurrentHashMap<>();

    public HologramManager(TwistedSMP plugin) {
        this.plugin = plugin;
    }

    public void spawnTextHologram(Location location, String text, int durationTicks, TextColor color) {
        if (location == null || location.getWorld() == null) return;

        Display display = (Display) location.getWorld().spawnEntity(location.toHighestLocation(1), EntityType.TEXT_DISPLAY);
        if (display == null) return;

        Component comp = Component.text(text).color(color != null ? color : NamedTextColor.WHITE);
        display.customName(comp);
        display.setCustomNameVisible(true);
        display.setBillboard(Display.Billboard.CENTER);
        display.setShadowRadius(0.12f);
        display.setShadowStrength(1.0f);
        display.setSeeThrough(false);
        display.setBrightness(new Display.Brightness(15, 15));

        track(display);

        if (durationTicks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (display.isValid()) display.remove();
                    untrack(display);
                }
            }.runTaskLater(plugin, durationTicks);
        }
    }

    public void spawnTitleHologram(Location location, String title, String subtitle, int durationTicks) {
        spawnTextHologram(location, title, durationTicks, TextColor.color(0xfdcb6e));
        if (subtitle != null && !subtitle.isEmpty()) {
            Location subLoc = location.clone().add(0, -0.45, 0);
            spawnTextHologram(subLoc, subtitle, durationTicks, NamedTextColor.GRAY);
        }
    }

    public void spawnEvolutionPillar(Location location, int durationTicks) {
        if (location == null || location.getWorld() == null) return;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }

                double height = 3.0 + Math.sin(ticks * 0.15) * 0.5;
                Location beamLoc = location.clone().add(0, height, 0);

                spawnLightBeam(beamLoc, ticks * 0.1);

                for (double y = 0; y < height; y += 0.4) {
                    Location l = location.clone().add(0, y, 0);
                    l.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, l, 2, 0.15, 0.15, 0.15, 0.01);
                    l.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, l, 1, 0.1, 0.1, 0.1, 0.005);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnLightBeam(Location location, double offset) {
        TextDisplay beam = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        if (beam == null) return;
        Component comp = Component.text("|").color(TextColor.color(0x55ffff)).font(net.kyori.adventure.text.format.NamedTextColor.WHITE);
        beam.customName(comp);
        beam.setCustomNameVisible(true);
        beam.setBillboard(Display.Billboard.VERTICAL);
        beam.setShadowRadius(0f);
        beam.setShadowStrength(0f);
        beam.setBrightness(new Display.Brightness(15, 15));
        track(beam);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (beam.isValid()) beam.remove();
                untrack(beam);
            }
        }.runTaskLater(plugin, 8L);
    }

    public void spawnOrbitalText(Location center, String text, int rings, int durationTicks) {
        if (center == null || center.getWorld() == null) return;

        int particlesPerRing = 8;
        for (int ring = 0; ring < rings; ring++) {
            final int ringIndex = ring;
            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (tick >= durationTicks) {
                        cancel();
                        return;
                    }
                    for (int i = 0; i < particlesPerRing; i++) {
                        double angle = (2 * Math.PI / particlesPerRing) * i + (tick * 0.05 * (ringIndex + 1));
                        double radius = 1.2 + ringIndex * 0.6;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        double y = tick * 0.035;

                        Location l = center.clone().add(x, y, z);
                        l.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, l, 1, 0.02, 0.02, 0.02, 0);
                    }
                    tick++;
                }
            }.runTaskTimer(plugin, ring * 2L, 1L);
        }
    }

    public void spawnRiftHologram(Location location, int durationTicks) {
        spawnTextHologram(location.clone().add(0, 7, 0), "§5§lRIFT EVENT", durationTicks, TextColor.color(0xa29bfe));
        spawnTextHologram(location.clone().add(0, 6.5, 0), "§dRewards lie within...", durationTicks, NamedTextColor.LIGHT_PURPLE);
    }

    public void spawnStormHologram(Location location, int durationTicks) {
        spawnTextHologram(location.clone().add(0, 8, 0), "§8§lTWISTED STORM", durationTicks, NamedTextColor.DARK_GRAY);
        spawnTextHologram(location.clone().add(0, 7.5, 0), "§cTake cover...", durationTicks, TextColor.color(0xe74c3c));
    }

    public void clearAll() {
        for (List<Display> list : activeHolograms.values()) {
            for (Display d : list) {
                if (d != null && d.isValid()) d.remove();
            }
        }
        activeHolograms.clear();
    }

    private void track(Display display) {
        activeHolograms.computeIfAbsent(display.getUniqueId(), u -> new ArrayList<>()).add(display);
    }

    private void untrack(Display display) {
        List<Display> list = activeHolograms.get(display.getUniqueId());
        if (list != null) {
            list.remove(display);
            if (list.isEmpty()) activeHolograms.remove(display.getUniqueId());
        }
    }
}
