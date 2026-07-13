package com.twisted.smp.listeners;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.energy.EssenceManager;
import com.twisted.smp.twists.Twist;
import com.twisted.smp.twists.TwistManager;
import com.twisted.smp.vfx.HologramManager;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import com.twisted.smp.vfx.SoundDesigner;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerListener implements Listener {

    private final TwistedSMP plugin;
    private final com.twisted.smp.core.DataManager dataManager;
    private final TwistManager twistManager;

    public PlayerListener(TwistedSMP plugin, com.twisted.smp.core.DataManager dataManager, TwistManager twistManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.twistManager = twistManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data == null) return;

        data.writeToPersistentData(player.getPersistentDataContainer());

        if (!data.isTwistSelected() && !player.hasPlayedBefore()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    twistManager.openTwistSelectionGUI(player);
                }
            }, 40L);
        }

        refreshPassiveEffects(player, data);
        plugin.getDataManager().savePlayerData(data, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data != null) {
            data.purgeExpiredCooldowns();
            dataManager.savePlayerData(data, false);
            dataManager.getPlayerDataCache().remove(player.getUniqueId());
            if (plugin.vfx() != null) {
                plugin.vfx().untrack(player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title == null || !title.contains("Select Your Twist")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        ItemStack item = event.getCurrentItem();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();
        Twist selectedTwist = null;
        for (Twist twist : Twist.getAllTwists()) {
            if (org.bukkit.ChatColor.stripColor(displayName).toLowerCase()
                    .contains(org.bukkit.ChatColor.stripColor(twistManager.getTwistDisplayName(twist)).toLowerCase())) {
                selectedTwist = twist;
                break;
            }
        }
        if (selectedTwist == null) return;

        if (twistManager.handleTwistSelection(player, selectedTwist)) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title == null || !title.contains("Select Your Twist")) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Item itemEntity = event.getItem();
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (data.getTwist() == Twist.BERSERKER) {
            if (itemEntity.getItemStack().getType() == Material.PORKCHOP ||
                itemEntity.getItemStack().getType() == Material.BEEF ||
                itemEntity.getItemStack().getType() == Material.ROTTEN_FLESH) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        if (data.getTwist() == Twist.BERSERKER) {
            int hungerRate = (int) (1 / 1.8);
            if (event.getFoodLevel() > player.getFoodLevel()) {
                player.setFoodLevel(Math.max(0, player.getFoodLevel() - hungerRate));
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected() || data.getTwist() != Twist.VOID) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        HologramManager holo = plugin.vfx().holograms();
        ScreenShake shake = plugin.vfx().shake();
        SoundDesigner sounds = plugin.vfx().sounds();

        if (from.getWorld().equals(to.getWorld()) && from.distance(to) > 3.0) {
            ParticlePatterns.spiral(from.clone().add(0, 0.2, 0), 1.5, 2.0, 25, ParticlePatterns.Color.VOID, 0.2);
            holo.spawnTextHologram(from.clone().add(0, 1.8, 0), "§5§lVOID PHASE", 20, ParticlePatterns.Color.VOID.toAdventure());
            from.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.4f);
            from.getWorld().spawnParticle(Particle.PORTAL, from, 25, 0.6, 0.6, 0.6, 0.06);

            to.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.4f, 1.2f);
            ParticlePatterns.ringBurst(to.clone().add(0, 0.2, 0), 2.5, 30, ParticlePatterns.Color.VOID, 2.0f);
            to.getWorld().spawnParticle(Particle.PORTAL, to, 35, 0.6, 0.6, 0.6, 0.08);
            to.getWorld().spawnParticle(Particle.DRAGON_BREATH, to, 10, 1.0, 0.5, 1.0, 0.02);
        }
    }

    public void refreshPassiveEffects(Player player, PlayerData data) {
        stopPassiveEffects(player);
        if (data.isTwistSelected()) {
            startPassiveEffects(player, data);
        }
    }

    private void stopPassiveEffects(Player player) {
        org.bukkit.attribute.AttributeInstance maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20.0);
        }
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE);
    }

    private void startPassiveEffects(Player player, PlayerData data) {
        if (!data.isTwistSelected()) return;
        Twist twist = data.getTwist();

        switch (twist) {
            case PHANTOM -> {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            }
            case INFERNAL -> {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            }
            case TITAN -> {
                org.bukkit.attribute.AttributeInstance maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(20 + (data.getEvolutionStage() * 4));
                }
            }
            default -> {}
        }
    }
