package com.twisted.smp.commands;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.twists.Twist;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.stream.Collectors;

public class TwistAdminCommand implements CommandExecutor, TabCompleter {

    private final TwistedSMP plugin;
    private final ConfigManager configManager;

    public TwistAdminCommand(TwistedSMP plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("twistedsmp.admin")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(configManager.getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /twistadmin set <player> <twist> | giveenergy <player> <amount> | giveessence <player> <amount> | event start <event> | event stop | reload"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "giveenergy" -> handleGiveEnergy(sender, args);
            case "giveessence" -> handleGiveEssence(sender, args);
            case "event" -> handleEvent(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand."));
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /twistadmin set <player> <twist>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(configManager.getMessage("player-not-found", "arg0", args[1])));
            return;
        }
        Twist twist = Twist.fromConfigId(args[2]);
        if (twist == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown twist: " + args[2]));
            return;
        }
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId());
        data.setTwist(twist);
        data.setTwistSelected(true);
        plugin.getDataManager().savePlayerData(data, true);
        plugin.getPlayerListener().refreshPassiveEffects(target, data);
        sender.sendMessage(MiniMessage.miniMessage().deserialize(
            configManager.getMessage("admin-set-success", "player", target.getName(), "twist", twist.displayName())));
    }

    private void handleGiveEnergy(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /twistadmin giveenergy <player> <amount>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(configManager.getMessage("player-not-found", "arg0", args[1])));
            return;
        }
        try {
            double amount = Double.parseDouble(args[2]);
            PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId());
            plugin.getEnergyManager().addEnergy(data, amount);
            plugin.getDataManager().savePlayerData(data, true);
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getMessage("admin-giveenergy-success", "player", target.getName(), "amount", String.valueOf(amount))));
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid amount."));
        }
    }

    private void handleGiveEssence(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /twistadmin giveessence <player> <amount>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(configManager.getMessage("player-not-found", "arg0", args[1])));
            return;
        }
        try {
            double amount = Double.parseDouble(args[2]);
            PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId());
            data.addEssence(amount);
            plugin.getDataManager().savePlayerData(data, true);
            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getMessage("admin-giveessence-success", "player", target.getName(), "amount", String.valueOf(amount))));
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid amount."));
        }
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /twistadmin event start|stop <name>"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /twistadmin event start|stop <name>"));
            return;
        }
        String sub = args[1].toLowerCase();
        String event = args[2].toLowerCase();

        boolean result;
        if ("start".equals(sub)) {
            result = switch (event) {
                case "storm" -> plugin.getTwistedStorm().start();
                case "rift" -> plugin.getRiftEvent().start();
                default -> false;
            };
            if (result) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getMessage("admin-event-start", "event", event)));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getMessage("event-invalid", "event", event)));
            }
        } else if ("stop".equals(sub)) {
            result = switch (event) {
                case "storm" -> plugin.getTwistedStorm().stop();
                case "rift" -> plugin.getRiftEvent().stop();
                default -> false;
            };
            if (result) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getMessage("admin-event-stop", "event", event)));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(
                    configManager.getMessage("event-invalid", "event", event)));
            }
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().reload();
            plugin.getTwistManager().loadTwistConfigs();
            plugin.getTwistedStorm().stop();
            plugin.getRiftEvent().stop();
            sender.sendMessage(MiniMessage.miniMessage().deserialize(configManager.getMessage("admin-reload")));
        } catch (Exception e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Reload failed: " + e.getMessage()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("twistedsmp.admin")) return List.of();
        if (args.length == 1) {
            return List.of("set", "giveenergy", "giveessence", "event", "reload").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if ("event".equals(args[0].toLowerCase()) || "set".equals(args[0].toLowerCase())
                || "giveenergy".equals(args[0].toLowerCase()) || "giveessence".equals(args[0].toLowerCase())) {
                if ("event".equals(args[0].toLowerCase())) {
                    return List.of("start", "stop");
                }
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        return List.of();
    }
}
