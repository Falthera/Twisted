package com.twisted.smp.vfx;

import com.twisted.smp.TwistedSMP;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VFXEngine {

    private final TwistedSMP plugin;
    private final HologramManager hologramManager;
    private final ScreenShake screenShake;
    private final Map<UUID, VFXPlayer> vfxPlayers = new ConcurrentHashMap<>();

    public VFXEngine(TwistedSMP plugin) {
        this.plugin = plugin;
        this.hologramManager = new HologramManager(plugin);
        this.screenShake = new ScreenShake(plugin);
    }

    public HologramManager holograms() {
        return hologramManager;
    }

    public ScreenShake shake() {
        return screenShake;
    }

    public void track(Player player) {
        vfxPlayers.computeIfAbsent(player.getUniqueId(), u -> new VFXPlayer(plugin, player));
    }

    public void untrack(UUID uuid) {
        VFXPlayer vp = vfxPlayers.remove(uuid);
        if (vp != null) vp.clear();
    }

    public void clearAll() {
        vfxPlayers.values().forEach(VFXPlayer::clear);
        vfxPlayers.clear();
        hologramManager.clearAll();
    }

    public static class VFXPlayer {
        private final TwistedSMP plugin;
        private final Player player;
        private final List<org.bukkit.entity.Display> displays;
        private final List<BukkitRunnable> runnables;

        VFXPlayer(TwistedSMP plugin, Player player) {
            this.plugin = plugin;
            this.player = player;
            this.displays = new ArrayList<>();
            this.runnables = new ArrayList<>();
        }

        public void addDisplay(org.bukkit.entity.Display display) {
            displays.add(display);
        }

        public void addTask(BukkitRunnable task) {
            runnables.add(task);
        }

        public void clear() {
            for (org.bukkit.entity.Display d : displays) {
                if (d != null && d.isValid()) d.remove();
            }
            displays.clear();
            for (BukkitRunnable r : runnables) {
                if (r != null && !r.isCancelled()) r.cancel();
            }
            runnables.clear();
        }

        public Player getPlayer() {
            return player;
        }
    }
}
