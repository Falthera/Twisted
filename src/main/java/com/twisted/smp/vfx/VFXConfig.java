package com.twisted.smp.vfx;

import com.twisted.smp.core.ConfigManager;

public class VFXConfig {

    private final ConfigManager configManager;

    public VFXConfig(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean isEnabled() {
        return configManager.getConfig().getBoolean("vfx.enabled", true);
    }

    public boolean isHologramsEnabled() {
        return configManager.getConfig().getBoolean("vfx.holograms", true);
    }

    public boolean isScreenShakeEnabled() {
        return configManager.getConfig().getBoolean("vfx.screen-shake", true);
    }

    public boolean isSoundsEnabled() {
        return configManager.getConfig().getBoolean("vfx.sounds", true);
    }

    public boolean isDamageNumbersEnabled() {
        return configManager.getConfig().getBoolean("vfx.damage-numbers", true);
    }

    public int getParticleDensity() {
        return configManager.getConfig().getInt("vfx.particle-density", 2);
    }
}
