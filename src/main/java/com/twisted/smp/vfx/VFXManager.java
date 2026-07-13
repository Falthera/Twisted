package com.twisted.smp.vfx;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VFXManager {

    private final TwistedSMP plugin;
    private final VFXEngine engine;
    private final VFXConfig config;

    public VFXManager(TwistedSMP plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.config = new VFXConfig(configManager);
        this.engine = new VFXEngine(plugin);
    }

    public VFXEngine engine() {
        return engine;
    }

    public com.twisted.smp.vfx.ScreenShake shake() {
        return engine.shake();
    }

    public com.twisted.smp.vfx.HologramManager holograms() {
        return engine.holograms();
    }

    public com.twisted.smp.vfx.SoundDesigner sounds() {
        return new com.twisted.smp.vfx.SoundDesigner(plugin);
    }

    public VFXConfig config() {
        return config;
    }

    public boolean enabled() {
        return config.isEnabled();
    }

    public void onJoin(Player player) {
        if (!enabled()) return;
        engine.track(player);
    }

    public void onQuit(Player player) {
        if (!enabled()) return;
        engine.untrack(player.getUniqueId());
    }

    public void untrack(UUID uuid) {
        engine.untrack(uuid);
    }

    public void clearAll() {
        engine.clearAll();
    }
}
