package com.twisted.smp.events;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.energy.EssenceManager;
import com.twisted.smp.vfx.HologramManager;
import com.twisted.smp.vfx.ParticlePatterns;
import com.twisted.smp.vfx.ScreenShake;
import com.twisted.smp.vfx.SoundDesigner;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.Color;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RiftEvent implements Listener {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;
    private final com.twisted.smp.core.DataManager dataManager;
    private final EssenceManager essenceManager;
    private boolean active = false;
    private Location riftLocation;
    private Location portalCenter;
    private org.bukkit.scheduler.BukkitTask particleTask;
    private org.bukkit.scheduler.BukkitTask expireTask;
    private org.bukkit.scheduler.BukkitTask localBroadcastTask;
    private int duration;
    private final Map<Location, Material> placedBlocks = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> openedChest = ConcurrentHashMap.newKeySet();
    private java.util.List<String> allowedWorlds;

    public RiftEvent(TwistedSMP plugin, ConfigManager configManager, com.twisted.smp.core.DataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.essenceManager = new EssenceManager(plugin, configManager);
    }

    public boolean start() {
        if (active) return false;
        active = true;
        duration = configManager.getEventConfig("rift").getInt("duration", 600);
        allowedWorlds = configManager.getEventConfig("rift").getStringList("allowed-worlds");
        openedChest.clear();

        org.bukkit.World world = null;
        if (allowedWorlds != null && !allowedWorlds.isEmpty()) {
            for (String worldName : allowedWorlds) {
                org.bukkit.World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    world = w;
                    break;
                }
            }
        }
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        int minDist = configManager.getEventConfig("rift").getInt("min-distance-from-spawn", 500);
        int maxDist = configManager.getEventConfig("rift").getInt("max-distance-from-spawn", 2000);
        Location spawn = world.getSpawnLocation();

        int dist = ThreadLocalRandom.current().nextInt(minDist, maxDist);
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        Location riftLoc = spawn.clone().add(
            Math.cos(angle) * dist,
            0,
            Math.sin(angle) * dist);
        while (riftLoc.getBlockY() > 60) riftLoc.add(0, -1, 0);
        while (!riftLoc.getBlock().getType().isSolid()) riftLoc.add(0, 1, 0);

        riftLocation = riftLoc;
        portalCenter = riftLoc.clone().add(0, 2, 0);

        ParticlePatterns.explosion(riftLoc.clone().add(0, 1, 0), ParticlePatterns.Color.RIFT, 2.8f);
        plugin.vfx().shake().shakeNearby(riftLoc, 25, ScreenShake.Intensity.HEAVY);
        plugin.vfx().sounds().playRiftSpawn(riftLoc.getWorld().getPlayers().isEmpty() ? null : riftLoc.getWorld().getPlayers().get(0));

        buildRiftPortal(riftLoc);

        broadcastStart();

        plugin.vfx().holograms().spawnRiftHologram(riftLoc.clone().add(0, 2, 0), duration);
        ParticlePatterns.verticalPillar(riftLoc.clone().add(0, 0.5, 0), 10, ParticlePatterns.Color.RIFT, duration, plugin);

        localBroadcastTask = new org.bukkit.scheduler.BukkitRunnable() {
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                double localRadius = configManager.getEventConfig("rift").getDouble("broadcast.local-radius", 200);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    double d = p.getLocation().distance(riftLoc);
                    if (d < localRadius) {
                        p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize(configManager.getMessage("event-rift-start-local")));
                        break;
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 60L);

        particleTask = new org.bukkit.scheduler.BukkitRunnable() {
            double tick = 0;

            public void run() {
                if (!active) { cancel(); return; }
                tick += 0.05;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    double d = p.getLocation().distance(riftLoc);
                    if (d < 45) {
                        for (int i = 0; i < 5; i++) {
                            double angle = (2 * Math.PI / 12) * i + tick;
                            double r = 2.5 + Math.sin(tick * 2) * 0.5;
                            Location portalParticle = portalCenter.clone().add(
                                Math.cos(angle) * r,
                                Math.sin(tick * 3 + i) * 1.2,
                                Math.sin(angle) * r
                            );
                            DustOptions dust = new DustOptions(Color.fromRGB(161, 155, 254), 1.5f);
                            p.spawnParticle(Particle.DUST, portalParticle, 2, 0.05, 0.05, 0.05, 0, dust);
                            p.spawnParticle(Particle.PORTAL, portalParticle.clone().add(0, 0.3, 0), 3, 0.1, 0.1, 0.1, 0.02);
                            if (ThreadLocalRandom.current().nextFloat() < 0.2) {
                                p.playSound(portalParticle, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.8f + (float) (Math.random() * 0.5));
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        expireTask = new org.bukkit.scheduler.BukkitRunnable() {
            public void run() {
                if (!active) { cancel(); return; }
                duration--;
                if (duration <= 0) {
                    stop();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        return true;
    }

    public boolean stop() {
        if (!active) return false;
        active = false;
        removeRiftPortal();
        if (particleTask != null) particleTask.cancel();
        if (expireTask != null) expireTask.cancel();
        if (localBroadcastTask != null) localBroadcastTask.cancel();
        openedChest.clear();
        plugin.vfx().holograms().clearAll();
        return true;
    }

    public boolean isActive() {
        return active;
    }

    public Location getRiftLocation() {
        return riftLocation;
    }

    private void buildRiftPortal(Location base) {
        Location portalHigh = base.clone().add(0, 6, 0);
        portalCenter = base.clone().add(0, 3, 0);

        placedBlocks.clear();

        Material frameMat = Material.END_STONE_BRICKS;
        Material netherrackMat = Material.NETHERRACK;
        Material fireMat = Material.FIRE;
        Material lanternMat = Material.END_ROD;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 6; y++) {
                    Location loc = base.clone().add(x, y, z);
                    boolean isEdge = Math.abs(x) == 2 || Math.abs(z) == 2;
                    boolean isTop = y == 6 && isEdge;
                    boolean isBottom = y == 0 && isEdge;
                    Block block = loc.getBlock();

                    if (isTop || isBottom || (isEdge && y > 0 && y < 6)) {
                        placedBlocks.put(loc.clone(), block.getType());
                        block.setType(frameMat, false);
                    } else if (Math.abs(x) <= 1 && Math.abs(z) <= 1 && y >= 1 && y <= 5) {
                        placedBlocks.put(loc.clone(), block.getType());
                        block.setType(Material.AIR, false);
                    } else if (!isEdge && y == 0) {
                        placedBlocks.put(loc.clone(), block.getType());
                        block.setType(netherrackMat, false);
                        if (Math.abs(x) == 1 && Math.abs(z) == 1 && y == 1) {
                            block.getRelative(org.bukkit.block.BlockFace.UP).setType(fireMat, false);
                            placedBlocks.put(loc.clone().add(0, 1, 0), Material.AIR);
                        }
                    }
                }
            }
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Location loc = base.clone().add(x, 0, z);
                if (loc.getBlock().getType() == Material.AIR) {
                    placedBlocks.put(loc.clone(), Material.AIR);
                    loc.getBlock().setType(Material.PODZOL, false);
                }
            }
        }

        Location topCenter = base.clone().add(0, 7, 0);
        placedBlocks.put(topCenter, topCenter.getBlock().getType());
        topCenter.getBlock().setType(lanternMat, false);
    }

    private void removeRiftPortal() {
        for (Map.Entry<Location, Material> entry : placedBlocks.entrySet()) {
            org.bukkit.block.Block block = entry.getKey().getBlock();
            if (block != null) {
                block.setType(entry.getValue(), false);
            }
        }
        placedBlocks.clear();
    }

    private void broadcastStart() {
        String msg = configManager.getMessage("event-rift-start-broadcast");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);
            plugin.vfx().shake().shake(p, ScreenShake.Intensity.HEAVY);
        }
    }

    public void openRiftChest(Player player) {
        if (openedChest.contains(player.getUniqueId())) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(plugin.getConfigManager().getMessage("withdraw-fail", "amount", "this")));
            return;
        }

        Location loc = riftLocation.clone().add(0, 1, 0);
        Block block = loc.getBlock();
        boolean firstOpen = block.getType() != Material.CHEST;
        if (firstOpen) {
            block.setType(Material.CHEST, false);
            Chest chest = (Chest) block.getState();
            Inventory inv = chest.getInventory();
            inv.clear();

            Random random = new Random();
            List<String> materials = configManager.getEventConfig("rift").getStringList("rewards.materials");
            for (String matStr : materials) {
                String[] parts = matStr.split(";");
                if (parts.length != 3) continue;
                try {
                    Material mat = Material.valueOf(parts[0]);
                    int min = Integer.parseInt(parts[1]);
                    int max = Integer.parseInt(parts[2]);
                    int amount = random.nextInt(max - min + 1) + min;
                    inv.addItem(new ItemStack(mat, amount));
                } catch (IllegalArgumentException e) { /* ignore invalid materials */ }
            }

            openedChest.add(player.getUniqueId());
        }

        if (!openedChest.contains(player.getUniqueId() + "_essence")) {
            PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
            if (data != null) {
                List<Map<?, ?>> essenceList = configManager.getEventConfig("rift").getMapList("rewards.essence");
                if (!essenceList.isEmpty()) {
                    Map<?, ?> map = essenceList.get(new Random().nextInt(essenceList.size()));
                    double min = ((Number) map.get("min")).doubleValue();
                    double max = ((Number) map.get("max")).doubleValue();
                    double amount = min + Math.random() * (max - min);
                    data.addEssence(amount);
                    dataManager.savePlayerData(data, true);
                }
            }
            openedChest.add(player.getUniqueId() + "_essence");
        }

        if (firstOpen) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.CHEST) {
                    block.setType(Material.AIR, false);
                }
            }, 200L);
        }

        Chest chest = (Chest) block.getState();
        chest.update();

        ParticlePatterns.explosion(loc.clone().add(0.5, 0.5, 0.5), ParticlePatterns.Color.RIFT, 2.0f);
        plugin.vfx().shake().shake(player, ScreenShake.Intensity.LIGHT);
        plugin.vfx().sounds().playRiftChestOpen(block.getLocation());
        plugin.vfx().holograms().spawnTextHologram(loc.clone().add(0, 2.2, 0), "§d§lREWARD!", 35, ParticlePatterns.Color.RIFT.toAdventure());
    }
}
