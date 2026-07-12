package com.twisted.smp.listeners;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.events.RiftEvent;
import com.twisted.smp.vfx.HologramManager;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import com.twisted.smp.vfx.SoundDesigner;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WorldListener implements Listener {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;

    public WorldListener(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onDragonKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        for (Player p : event.getEntity().getWorld().getPlayers()) {
            PlayerData data = plugin.getDataManager().getOrCreatePlayerData(p.getUniqueId());
            if (data.isTwistSelected()) {
                plugin.getEnergyManager().awardDragonDeath(data);
                Location loc = dragon.getLocation().clone().add(0, 2, 0);
                ParticlePatterns.explosion(loc, ParticlePatterns.Color.EVOLUTION, 3.5f);
                plugin.vfx().shake().shakeNearby(loc, 30, ScreenShake.Intensity.CINEMATIC);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
                p.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, loc, 80, 3.0, 3.0, 3.0, 0.08);
                p.getWorld().spawnParticle(org.bukkit.Particle.FLASH, loc, 6, 2.0, 2.0, 2.0, 0);
                plugin.vfx().holograms().spawnTextHologram(loc.clone().add(0, 4.0, 0), "§d§lDRAGON SLAIN", 80, ParticlePatterns.Color.VOID.toAdventure());
                p.getWorld().playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 0.8f);
            }
        }
    }

    @EventHandler
    public void onWitherKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Wither wither)) return;
        for (Player p : event.getEntity().getWorld().getPlayers()) {
            PlayerData data = plugin.getDataManager().getOrCreatePlayerData(p.getUniqueId());
            if (data.isTwistSelected()) {
                plugin.getEnergyManager().awardWitherDeath(data);
                Location loc = wither.getLocation().clone().add(0, 2, 0);
                ParticlePatterns.explosion(loc, ParticlePatterns.Color.STORM, 3.0f);
                plugin.vfx().shake().shakeNearby(loc, 25, ScreenShake.Intensity.CINEMATIC);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
                p.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, loc, 60, 2.0, 2.0, 2.0, 0.08);
                p.getWorld().spawnParticle(org.bukkit.Particle.FLASH, loc, 4, 1.5, 1.5, 1.5, 0);
                plugin.vfx().holograms().spawnTextHologram(loc.clone().add(0, 3.5, 0), "§8§lWITHER FALLEN", 70, ParticlePatterns.Color.STORM.toAdventure());
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.CHEST) {
                RiftEvent rift = plugin.getRiftEvent();
                if (rift.isActive() && block.getLocation().distance(rift.getRiftLocation()) < 20) {
                    org.bukkit.scheduler.Bukkit.getScheduler().runTask(plugin, () -> rift.openRiftChest(event.getPlayer()));
                }
            }
        }
    }
}
