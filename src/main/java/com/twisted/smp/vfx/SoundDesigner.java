package com.twisted.smp.vfx;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class SoundDesigner {

    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public SoundDesigner(org.bukkit.plugin.java.JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playAbilitySound(Location loc, SoundDesign abilitySound, boolean highStage) {
        float pitch = highStage ? 1.0f : 0.85f;
        switch (abilitySound) {
            case VOID_STEP:
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.2f);
                loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 0.6f, 1.5f);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 0.4f, 1.8f);
                break;
            case EARTHQUAKE:
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.4f);
                loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 0.8f);
                loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.2f);
                break;
            case BLOOD_RAGE:
                loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, pitch);
                loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.6f);
                loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.6f, 0.9f);
                break;
            case FADE:
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, 0.6f, 1.2f);
                loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 1.0f);
                break;
            case INFERNO_BURST:
                loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, 1.4f, 0.6f);
                loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 1.2f);
                break;
            case FREEZE:
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.35f);
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.0f);
                break;
        }
    }

    public void playEvolutionSound(Location loc, boolean stage3) {
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.2f);
        if (stage3) {
            loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 0.7f);
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 1.5f);
        }
    }

    public void playTwistSelectSound(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 2.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
    }

    public void playStormStart(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.4f);
        loc.getWorld().playSound(loc, Sound.WEATHER_RAIN, 0.8f, 0.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.4f, 0.5f);
    }

    public void playRiftSpawn(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.3f, 1.5f);
    }

    public void playDeathImpact(Player victm, Player killer) {
        Location loc = victm.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.6f, 0.5f);
        if (killer != null) {
            killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.0f);
        }
    }

    public void playRiftChestOpen(Location loc) {
        loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_OPEN, 0.8f, 0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 2.0f);
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RETURN, 0.5f, 1.5f);
    }

    public enum SoundDesign {
        VOID_STEP, EARTHQUAKE, BLOOD_RAGE, FADE, INFERNO_BURST, FREEZE,
        EVOLUTION, TWIST_SELECT, STORM_START, RIFT_SPAWN, DEATH_IMPACT, RIFT_CHEST
    }
}
