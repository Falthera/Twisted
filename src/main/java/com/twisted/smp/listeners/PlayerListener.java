package com.twisted.smp.listeners;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.DataManager;
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
import org.bukkit.event.block.Action;
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

        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            plugin.getAntiAbuseManager().recordPlayerJoin(player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
        }

        if (!data.isTwistSelected()) {
            if (!player.hasPlayedBefore()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        twistManager.openTwistSelectionGUI(player);
                    }
                }, 40L);
            } else {
                Twist randomTwist = Twist.getRandomTwists(1).get(0);
                twistManager.handleTwistSelection(player, randomTwist);
            }
        }

        refreshPassiveEffects(player, data);
        plugin.getDataManager().savePlayerData(data, true);

        if (data.isTwistSelected()) {
            plugin.getScoreboardManager().update(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data != null) {
            UUID uuid = player.getUniqueId();
            if (plugin.getAntiAbuseManager().isCombatTagged(uuid)) {
                plugin.getAntiAbuseManager().applyCombatLogPenalty(data);
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    plugin.getConfigManager().getMessage("death-penalty", "energy", String.valueOf(Math.abs((int) plugin.getAntiAbuseManager().getDeathPenalty(uuid))))));
            }
            data.purgeExpiredCooldowns();
            dataManager.savePlayerData(data, false);
            plugin.getAntiAbuseManager().saveData(uuid);
            dataManager.getPlayerDataCache().remove(uuid);
            if (plugin.vfx() != null) {
                plugin.vfx().untrack(uuid);
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
            org.bukkit.configuration.ConfigurationSection berserkerConfig = plugin.getConfigManager().getTwistConfig("berserker");
            double hungerMult = berserkerConfig != null ? berserkerConfig.getDouble("passive.hunger-drain-multiplier", 1.0) : 1.0;
            if (hungerMult > 1.0 && event.getFoodLevel() > player.getFoodLevel()) {
                int delta = event.getFoodLevel() - player.getFoodLevel();
                int adjusted = (int) Math.floor(delta / hungerMult);
                event.setFoodLevel(player.getFoodLevel() + Math.max(0, adjusted));
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.contains("Twisted Essence")) {
            if (data.getEssence() > 0) {
                data.addEssence(1);
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    plugin.getConfigManager().getMessage("essence-deposit-success", "amount", "1")));
                plugin.getDataManager().savePlayerData(data, true);
            }
            event.setCancelled(true);
            return;
        }

        if (displayName.contains("Stability Crystal")) {
            if (data.getInstability() > 0) {
                double reduction = Math.min(50, data.getInstability());
                data.subtractInstability(reduction);
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    "<green>Instability reduced by " + (int) reduction + "%"));
                plugin.getDataManager().savePlayerData(data, true);
            } else {
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                    "<red>You have no instability to reduce."));
            }
            event.setCancelled(true);
            return;
        }

        if (displayName.contains("Rift Key")) {
            if (plugin.getRiftEvent() == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Rift Events are not available."));
                event.setCancelled(true);
                return;
            }
            if (!plugin.getRiftEvent().isActive()) {
                plugin.getRiftEvent().start();
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<dark_aqua>You have summoned a Rift Event!"));
                plugin.getDataManager().savePlayerData(data, true);
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>A Rift Event is already active."));
            }
            event.setCancelled(true);
            return;
        }

        if (displayName.contains("Twisted Core")) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }
            double rerollCost = plugin.getConfigManager().getConfig().getDouble("essence.twist-change-cost", 100);
            if (data.getEssence() < rerollCost) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfigManager().getMessage("essence-insufficient", "amount",
                        String.valueOf((int) rerollCost), "have", String.valueOf((int) data.getEssence()))));
                event.setCancelled(true);
                return;
            }
            java.util.List<Twist> available = new java.util.ArrayList<>(Twist.getAllTwists());
            available.remove(data.getTwist());
            if (available.isEmpty()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>No other twists available."));
                event.setCancelled(true);
                return;
            }
            Twist newTwist = available.get(new java.util.Random().nextInt(available.size()));
            data.subtractEssence(rerollCost);
            data.setTwist(newTwist);
            data.setTwistSelected(true);
            data.writeToPersistentData(player.getPersistentDataContainer());
            plugin.getDataManager().savePlayerData(data, true);
            refreshPassiveEffects(player, data);
            String newName = twistManager.getTwistDisplayName(newTwist);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>Twisted Core! New twist: <white>" + newName + "</white>"));
            plugin.vfx().sounds().playTwistSelectSound(player);
            plugin.vfx().holograms().spawnTextHologram(player.getLocation().clone().add(0, 2.0, 0),
                "§d§lTWIST CHANGE", 40, net.kyori.adventure.text.format.TextColor.color(0xa29bfe));
            event.setCancelled(true);
            return;
        }

        if (displayName.contains("Verity Crown")) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }
            int extraEnergy = 25;
            plugin.getEnergyManager().addEnergy(data, extraEnergy);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gold>Verity Crown activated! +" + extraEnergy + " energy."));
            plugin.vfx().sounds().playAbilitySound(player.getLocation(), SoundDesigner.SoundDesign.VOID_STEP, true);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 20, 0.5, 1.0, 0.5, 0.05);
            event.setCancelled(true);
            return;
        }

        if (displayName.contains("Verity Talisman")) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }
            if (data.getEssence() > 0) {
                double convertRatio = 2.0;
                double converted = Math.min(data.getEssence(), 50);
                data.subtractEssence(converted);
                plugin.getEnergyManager().addEnergy(data, converted * convertRatio);
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gold>Verity Talisman converts " + (int) converted + " Essence to " + (int) (converted * convertRatio) + " Energy!"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>No Essence to convert. Deposit Essence first."));
            }
            plugin.getDataManager().savePlayerData(data, true);
            event.setCancelled(true);
            return;
        }

        if (displayName.contains("Verity Focus")) {
            if (data.isOnCooldown("verity_focus")) {
                long remaining = data.getCooldownRemaining("verity_focus");
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Verity Focus on cooldown! " + (remaining / 1000) + "s remaining."));
                event.setCancelled(true);
                return;
            }
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }
            data.subtractInstability(100);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.ABSORPTION, 20 * 20, 0, false, false));
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<#a29bfe>Verity Focus absorbs all instability and grants Absorption."));
            plugin.vfx().sounds().playAbilitySound(player.getLocation(), SoundDesigner.SoundDesign.FADE, true);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 30, 0.5, 1.0, 0.5, 0.04);
            long cooldownEnd = System.currentTimeMillis() + 60_000L;
            data.putCooldown("verity_focus", cooldownEnd);
            plugin.getDataManager().savePlayerData(data, true);
            event.setCancelled(true);
            return;
        }

        org.bukkit.Material abilityMat = null;
        if (data.getTwist() == Twist.VOID) {
            abilityMat = org.bukkit.Material.CHORUS_FRUIT;
        } else if (data.getTwist() == Twist.INFERNAL) {
            abilityMat = org.bukkit.Material.BLAZE_ROD;
        } else if (data.getTwist() == Twist.FROSTBORN) {
            abilityMat = org.bukkit.Material.PACKED_ICE;
        } else if (data.getTwist() == Twist.TITAN) {
            abilityMat = org.bukkit.Material.COBBLESTONE;
        } else if (data.getTwist() == Twist.BERSERKER) {
            abilityMat = org.bukkit.Material.REDSTONE;
        } else if (data.getTwist() == Twist.PHANTOM) {
            abilityMat = org.bukkit.Material.GLASS;
        }
        if (abilityMat != null && item.getType() == abilityMat) {
            event.setCancelled(true);
            if (plugin.getAbilityManager().useAbility(player, data.getTwist())) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
            }
            return;
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
        org.bukkit.attribute.AttributeInstance speed = player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.1);
        }
        org.bukkit.attribute.AttributeInstance kb = player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.setBaseValue(0.0);
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
                org.bukkit.configuration.ConfigurationSection config = plugin.getConfigManager().getTwistConfig("titan");
                double extraHearts = config != null ? config.getDouble("passive.extra-hearts", 4) : 4;
                org.bukkit.attribute.AttributeInstance maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(20 + extraHearts + ((data.getEvolutionStage() - 1) * extraHearts));
                }
                double kbResist = config != null ? config.getDouble("passive.knockback-resistance", 0.0) : 0.0;
                org.bukkit.attribute.AttributeInstance kb = player.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
                if (kb != null) {
                    kb.setBaseValue(kbResist);
                }
                double speedMult = config != null ? config.getDouble("passive.movement-speed-multiplier", 1.0) : 1.0;
                if (speedMult != 1.0) {
                    org.bukkit.attribute.AttributeInstance speed = player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                    if (speed != null) {
                        speed.setBaseValue(0.1 * speedMult);
                    }
                }
                double miningMult = config != null ? config.getDouble("passive.mining-speed-multiplier", 1.0) : 1.0;
                if (miningMult < 1.0) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, (int) Math.ceil((1.0 - miningMult) * 3), false, false));
                } else if (miningMult > 1.0) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.DIG_SPEED, Integer.MAX_VALUE, (int) Math.floor(miningMult - 1), false, false));
                }
            }
            default -> {}
        }
    }
}
