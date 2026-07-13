package com.twisted.smp.abilities;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.energy.EnergyManager;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import com.twisted.smp.vfx.SoundDesigner;
import com.twisted.smp.vfx.VFXManager;
import com.twisted.smp.twists.Twist;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Particle;

import java.util.*;

public class AbilityManager {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;
    private final Map<Twist, AbstractAbility> abilities = new HashMap<>();
    private final VFXManager vfxManager;
    private final ScreenShake screenShake;
    private final SoundDesigner soundDesigner;

    public AbilityManager(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.vfxManager = plugin.vfx();
        this.screenShake = vfxManager.shake();
        this.soundDesigner = plugin.vfx().sounds();
        registerAbilities();
    }

    private void registerAbilities() {
        abilities.put(Twist.VOID, new VoidStepAbility(plugin));
        abilities.put(Twist.TITAN, new EarthquakeAbility(plugin));
        abilities.put(Twist.BERSERKER, new BloodRageAbility(plugin));
        abilities.put(Twist.PHANTOM, new FadeAbility(plugin));
        abilities.put(Twist.INFERNAL, new InfernoBurstAbility(plugin));
        abilities.put(Twist.FROSTBORN, new FreezeAbility(plugin));
    }

    public boolean useAbility(Player player, Twist twist) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) return false;

        AbstractAbility ability = abilities.get(twist);
        if (ability == null) return false;

        int cooldown = ability.getCooldown(data.getEvolutionStage());
        String abilityId = twist.name() + "-" + ability.getName();

        if (data.isOnCooldown(abilityId)) {
            long remaining = data.getCooldownRemaining(abilityId);
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(configManager.getMessage("ability-cooldown", "ability", ability.getName(), "time", String.valueOf(remaining))));
            return false;
        }

        EnergyManager energyManager = plugin.getEnergyManager();

        int energyThreshold = (int) configManager.getConfig().getDouble("instability.energy-threshold", 150);
        if (data.getEnergy() >= energyThreshold) {
            plugin.getInstabilityManager().addInstabilityForAbilityUse(data);
        }

        boolean success = ability.execute(player, data);
        if (success) {
            long endTime = System.currentTimeMillis() + cooldown * 1000L;
            data.putCooldown(abilityId, endTime);
            plugin.getDataManager().saveCooldown(player.getUniqueId(), abilityId, endTime);
            plugin.getDataManager().savePlayerData(data, true);

            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(configManager.getMessage("ability-used", "ability", ability.getName())));
        }
        return success;
    }

    public String getAbilityName(Twist twist) {
        AbstractAbility ability = abilities.get(twist);
        return ability != null ? ability.getName() : "Unknown";
    }

    public int getCooldown(Twist twist, int evolutionStage) {
        AbstractAbility ability = abilities.get(twist);
        return ability != null ? ability.getCooldown(evolutionStage) : 0;
    }

    abstract class AbstractAbility {
        private final TwistedSMP plugin;

        AbstractAbility(TwistedSMP plugin) {
            this.plugin = plugin;
        }

        abstract String getName();
        abstract boolean execute(Player player, PlayerData data);
        abstract int getCooldown(int evolutionStage);
    }

    class VoidStepAbility extends AbstractAbility {
        VoidStepAbility(TwistedSMP plugin) { super(plugin); }

        @Override String getName() { return "Void Step"; }
        @Override int getCooldown(int evolutionStage) {
            return 15 + (3 - evolutionStage) * 5;
        }

        @Override
        boolean execute(Player player, PlayerData data) {
            int stage = data.getEvolutionStage();
            double range = 15 + (stage - 1) * 10;
            Location loc = player.getLocation();
            Vector direction = loc.getDirection().normalize();
            Location target = loc.clone().add(direction.multiply(range));
            if (target.getY() < -64) target.setY(loc.getY());

            VFXManager engine = plugin.vfx();
            ScreenShake shake = engine.shake();
            SoundDesigner sounds = plugin.vfx().sounds();

            ParticlePatterns.explosion(loc.clone().add(direction.clone().multiply(0.5)), ParticlePatterns.Color.VOID, 1.8f);
            sounds.playAbilitySound(loc, SoundDesigner.SoundDesign.VOID_STEP, stage >= 2);

            player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.PORTAL, loc, 30, 0.8, 0.8, 0.8, 0.08);

            player.teleport(target);

            shake.shake(player, ScreenShake.Intensity.LIGHT);
            ParticlePatterns.explosion(target.clone(), ParticlePatterns.Color.VOID, 1.4f);
            player.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.4f, 0.9f);
            player.getWorld().spawnParticle(Particle.PORTAL, target, 35, 0.8, 0.8, 0.8, 0.1);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, target, 10, 1.0, 0.4, 1.0, 0.02);

            if (stage >= 2) {
                String title = stage >= 3 ? "§5§lVOID MASTER" : "§d§lVOID STRIDE";
                engine.holograms().spawnTextHologram(target.clone().add(0, 2.2, 0), title, 40, ParticlePatterns.Color.VOID.toAdventure());
                shake.shake(player, ScreenShake.Intensity.LIGHT);
            }

            return true;
        }
    }

    class EarthquakeAbility extends AbstractAbility {
        EarthquakeAbility(TwistedSMP plugin) { super(plugin); }
        @Override String getName() { return "Earthquake"; }
        @Override int getCooldown(int evolutionStage) {
            return 45 - (evolutionStage - 1) * 10;
        }

        @Override
        boolean execute(Player player, PlayerData data) {
            int stage = data.getEvolutionStage();
            double radius = 6 + (stage - 1) * 2;
            double damage = 3 + (stage - 1) * 1;
            double kby = 0.8 + (stage - 1) * 0.3;
            Location loc = player.getLocation();

            VFXManager engine = plugin.vfx();
            ScreenShake shake = engine.shake();
            SoundDesigner sounds = plugin.vfx().sounds();

            shake.shakeWorld(player.getWorld(), ScreenShake.Intensity.HEAVY);
            sounds.playAbilitySound(loc, SoundDesigner.SoundDesign.EARTHQUAKE, stage >= 2);

            for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (e instanceof LivingEntity target && target != player && !target.isDead()) {
                    double distance = target.getLocation().distance(loc);
                    if (distance <= radius) {
                        target.damage(damage, player);
                        Vector kb = target.getLocation().toVector().subtract(loc.toVector()).normalize();
                        kb.setY(kby);
                        kb.multiply(1.0 - (distance / radius) * 0.5);
                        target.setVelocity(kb);

                        ParticlePatterns.damageNumber(target.getLocation().clone().add(0, 1, 0), damage, ParticlePatterns.Color.EARTH, plugin);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_STONE_BREAK, 0.7f, 0.8f);
                    }
                }
            }

            ParticlePatterns.explosion(loc.clone(), ParticlePatterns.Color.EARTH, 2.0f);
            ParticlePatterns.ringBurst(loc, radius, 64, ParticlePatterns.Color.EARTH, 1.6f);
            player.getWorld().spawnParticle(Particle.CLOUD, loc, 50, radius, 0.6, radius, 0.02);
            player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, loc, 30, radius, 0.4, radius, 0.01);

            if (stage >= 2) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        ParticlePatterns.ringBurst(loc, radius * 0.9, 32, ParticlePatterns.Color.EARTH, 1.0f);
                        player.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1.2f, 0.6f);
                    }
                }.runTaskLater(plugin, 12L);
            }

            if (stage >= 3) {
                engine.holograms().spawnTextHologram(loc.clone().add(0, 2.5, 0), "§c☠ SEISMIC COLLAPSE", 50, ParticlePatterns.Color.EARTH.toAdventure());
                shake.shakeNearby(loc, radius + 5, ScreenShake.Intensity.EXTREME);
            }

            return true;
        }
    }

    class BloodRageAbility extends AbstractAbility {
        BloodRageAbility(TwistedSMP plugin) { super(plugin); }
        @Override String getName() { return "Blood Rage"; }
        @Override int getCooldown(int evolutionStage) {
            return 60 - (evolutionStage - 1) * 15;
        }

        @Override
        boolean execute(Player player, PlayerData data) {
            int stage = data.getEvolutionStage();
            int duration = 10 + (stage - 1) * 5;
            int strengthLevel = 1 + (stage - 1);
            int speedLevel = 1 + (stage - 1);

            VFXManager engine = plugin.vfx();
            ScreenShake shake = engine.shake();
            SoundDesigner sounds = plugin.vfx().sounds();

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH, duration * 20, strengthLevel, false, false, true));
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, duration * 20, speedLevel, false, false, true));

            shake.shake(player, ScreenShake.Intensity.MEDIUM);
            sounds.playAbilitySound(player.getLocation(), SoundDesigner.SoundDesign.BLOOD_RAGE, stage >= 2);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.6f);

            for (int i = 0; i < 25; i++) {
                player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, player.getLocation().add(
                    (Math.random() - 0.5) * 1.2, Math.random() * 1.5, (Math.random() - 0.5) * 1.2
                ), 1, 0.1, 0.1, 0.1, 0);
            }

            ParticlePatterns.vortex(player.getLocation(), 60, ParticlePatterns.Color.BLOOD, 1.5);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 20, 0.7, 0.3, 0.7, 0,
                new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0xe74c3c), 1.0f));

            engine.holograms().spawnTextHologram(player.getLocation().clone().add(0, 2.6, 0), "§c§lBLOOD RAGE", duration * 20, ParticlePatterns.Color.BLOOD.toAdventure());

            new org.bukkit.scheduler.BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= duration * 20) {
                        cancel();
                        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.02);
                        return;
                    }

                    if (ticks % 5 == 0) {
                        ParticlePatterns.vortex(player.getLocation().add(0, 0.3, 0), 15, ParticlePatterns.Color.BLOOD, 1.0);
                        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1.2, 0), 3, 0.6, 0.3, 0.6, 0.01);
                    }
                    if (ticks % 7 == 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.6f, 0.7f + (float) Math.random() * 0.5f);
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

            return true;
        }
    }

    class FadeAbility extends AbstractAbility {
        FadeAbility(TwistedSMP plugin) { super(plugin); }
        @Override String getName() { return "Fade"; }
        @Override int getCooldown(int evolutionStage) {
            return 60 - (evolutionStage - 1) * 15;
        }

        @Override
        boolean execute(Player player, PlayerData data) {
            int stage = data.getEvolutionStage();
            int duration = 8 + (stage - 1) * 4;

            VFXManager engine = plugin.vfx();
            ScreenShake shake = engine.shake();
            SoundDesigner sounds = plugin.vfx().sounds();

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INVISIBILITY, duration * 20, 0, false, false, true));
            if (stage >= 2) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, duration * 20, 0, false, false, true));
            }

            shake.shake(player, ScreenShake.Intensity.LIGHT);
            sounds.playAbilitySound(player.getLocation(), SoundDesigner.SoundDesign.FADE, stage >= 2);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.8f, 1.5f);

            for (int i = 0; i < 40; i++) {
                double angle = (2 * Math.PI / 40) * i;
                double r = 0.8 + Math.sin(i * 0.5) * 0.3;
                Location l = player.getLocation().add(Math.cos(angle) * r, Math.random() * 0.8, Math.sin(angle) * r);
                player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, l, 1, 0.05, 0.05, 0.05, 0.02);
                player.getWorld().spawnParticle(Particle.SMOKE, l, 1, 0.05, 0.05, 0.05, 0.01);
            }

            engine.holograms().spawnTextHologram(player.getLocation().clone().add(0, 1.8, 0), "§f§lFADE", duration * 20, ParticlePatterns.Color.PHANTOM.toAdventure());

            if (stage >= 3) {
                String title = "§f§lGHOST REALM";
                engine.holograms().spawnTextHologram(player.getLocation().clone().add(0, 2.6, 0), title, duration * 20, ParticlePatterns.Color.PHANTOM.toAdventure());
                engine.holograms().spawnOrbitalText(player.getLocation().clone().add(0, 1.0, 0), "§f", 2, duration * 20);
            }

            new org.bukkit.scheduler.BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= duration * 20) {
                        cancel();
                        ParticlePatterns.explosion(player.getLocation().add(0, 0.3, 0), ParticlePatterns.Color.PHANTOM, 1.0f);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.5f);
                        return;
                    }
                    if (ticks % 4 == 0) {
                        ParticlePatterns.vortex(player.getLocation().add(0, 0.5, 0), 12, ParticlePatterns.Color.PHANTOM, 0.6);
                    }
                    if (ticks % 10 == 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.3f, 1.5f);
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

            return true;
        }
    }

    class InfernoBurstAbility extends AbstractAbility {
        InfernoBurstAbility(TwistedSMP plugin) { super(plugin); }
        @Override String getName() { return "Inferno Burst"; }
        @Override int getCooldown(int evolutionStage) {
            return 45 - (evolutionStage - 1) * 10;
        }

        @Override
        boolean execute(Player player, PlayerData data) {
            int stage = data.getEvolutionStage();
            int radius = 5 + (stage - 1) * 2;
            int igniteDuration = 4 + (stage - 1) * 2;

            VFXManager engine = plugin.vfx();
            ScreenShake shake = engine.shake();
            SoundDesigner sounds = plugin.vfx().sounds();

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false));

            Location loc = player.getLocation();
            shake.shake(player, stage >= 3 ? ScreenShake.Intensity.HEAVY : ScreenShake.Intensity.MEDIUM);
            sounds.playAbilitySound(loc, SoundDesigner.SoundDesign.INFERNO_BURST, stage >= 2);
            loc.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc, 25, 0.2, 0.2, 0.2, 0.04);
            loc.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, loc, 15, 0.5, 0.3, 0.5, 0.03);

            for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (e instanceof LivingEntity target && target != player && !target.isDead()) {
                    target.setFireTicks(igniteDuration * 20);
                    target.damage(Math.max(1, (double) igniteDuration / 2), player);
                    ParticlePatterns.explosion(target.getLocation().add(0, 0.6, 0), ParticlePatterns.Color.INFERNO, 1.2f);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 1.0f);
                }
            }

            ParticlePatterns.explosion(loc, ParticlePatterns.Color.INFERNO, stage >= 2 ? 2.5f : 1.8f);
            ParticlePatterns.ringBurst(loc, radius, 48, ParticlePatterns.Color.INFERNO, 1.4f);
            loc.getWorld().spawnParticle(org.bukkit.Particle.LAVA, loc, 15, radius * 0.6, 0.4, radius * 0.6, 0.04);

            engine.holograms().spawnTextHologram(loc.clone().add(0, 3.0, 0), "§6§lINFERNO BURST", 35, ParticlePatterns.Color.INFERNO.toAdventure());

            if (stage >= 2) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        particleHotGround(loc, radius);
                    }
                }.runTaskLater(plugin, 5L);
            }

            if (stage >= 3) {
                shake.shakeNearby(loc, radius + 8, ScreenShake.Intensity.CINEMATIC);
                engine.holograms().spawnTextHologram(loc.clone().add(0, 4.2, 0), "§c§lHELLSCAPE", 60, ParticlePatterns.Color.INFERNO.toAdventure());
            }

            return true;
        }

        private void particleHotGround(Location center, double radius) {
            for (double x = -radius; x <= radius; x += 0.8) {
                for (double z = -radius; z <= radius; z += 0.8) {
                    Location l = center.clone().add(x, 0.05, z);
                    if (l.distance(center) <= radius) {
                        l.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, l, 3, 0.1, 0.05, 0.1, 0.01);
                        l.getWorld().spawnParticle(org.bukkit.Particle.LAVA, l, 1, 0.1, 0.05, 0.1, 0.005);
                    }
                }
            }
        }
    }

    class FreezeAbility extends AbstractAbility {
        FreezeAbility(TwistedSMP plugin) { super(plugin); }
        @Override String getName() { return "Freeze"; }
        @Override int getCooldown(int evolutionStage) {
            return 40 - (evolutionStage - 1) * 10;
        }

        @Override
        boolean execute(Player player, PlayerData data) {
            int stage = data.getEvolutionStage();
            int radius = 5 + (stage - 1) * 2;
            int slownessLevel = stage * 2;
            if (slownessLevel > 10) slownessLevel = 10;

            boolean freezeSelf = stage >= 3;

            VFXManager engine = plugin.vfx();
            ScreenShake shake = engine.shake();
            SoundDesigner sounds = plugin.vfx().sounds();
            Location loc = player.getLocation();

            shake.shake(player, ScreenShake.Intensity.LIGHT);
            shake.shakeNearby(loc, radius + 4, ScreenShake.Intensity.LIGHT);
            sounds.playAbilitySound(loc, SoundDesigner.SoundDesign.FREEZE, stage >= 2);
            player.getWorld().playSound(loc, Sound.BLOCK_GLASS_HIT, 1.0f, 1.2f);

            for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (e instanceof LivingEntity target && !target.isDead()) {
                    if (target == player && !freezeSelf) continue;
                    int amplifier = Math.max(0, slownessLevel - 1);
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 80, amplifier, false, false));
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 40, 0, false, false));
                    particleFreezeHit(target.getLocation().add(0, 0.8, 0), 20);
                }
            }

            ParticlePatterns.explosion(loc, ParticlePatterns.Color.FROST, 1.6f);
            ParticlePatterns.ringBurst(loc, radius, 56, ParticlePatterns.Color.FROST, 2.0f);
            player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, loc, 40, radius, 2, radius, 0.08);
            particleIceFloor(loc, radius);

            engine.holograms().spawnTextHologram(loc.clone().add(0, 2.2, 0), "§b§lFREEZE", 40, ParticlePatterns.Color.FROST.toAdventure());

            if (stage >= 2) {
                engine.holograms().spawnTextHologram(loc.clone().add(0, 3.0, 0), "§f§lFROST NEXUS", 55, ParticlePatterns.Color.FROST.toAdventure());
            }

            return true;
        }

        private void particleFreezeHit(Location center, int count) {
            for (int i = 0; i < count; i++) {
                double x = (Math.random() - 0.5) * 1.0;
                double y = Math.random() * 1.3;
                double z = (Math.random() - 0.5) * 1.0;
                Location l = center.clone().add(x, y, z);
                center.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, l, 1, 0.05, 0.05, 0.05, 0.03);
                center.getWorld().spawnParticle(org.bukkit.Particle.BLOCK_CRUMBLE, l, 1, 0.05, 0.05, 0.05, 0, new org.bukkit.inventory.ItemStack(org.bukkit.Material.ICE));
            }
        }

        private void particleIceFloor(Location center, double radius) {
            for (double x = -radius; x <= radius; x += 0.6) {
                for (double z = -radius; z <= radius; z += 0.6) {
                    if (new Vector(x, 0, z).length() > radius) continue;
                    Location l = center.clone().add(x, 0.02, z);
                    center.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, l, 1, 0.1, 0.02, 0.1, 0.01);
                }
            }
        }
    }
}
