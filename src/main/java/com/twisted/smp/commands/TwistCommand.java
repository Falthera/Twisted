package com.twisted.smp.commands;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.ConfigManager;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.twists.Twist;
import com.twisted.smp.twists.TwistManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class TwistCommand implements CommandExecutor, TabCompleter {

    private final TwistedSMP plugin;
    private final TwistManager twistManager;
    private final com.twisted.smp.energy.EnergyManager energyManager;
    private final com.twisted.smp.evolution.EvolutionManager evolutionManager;
    private final com.twisted.smp.energy.EssenceManager essenceManager;

    public TwistCommand(TwistedSMP plugin) {
        this.plugin = plugin;
        this.twistManager = plugin.getTwistManager();
        this.energyManager = plugin.getEnergyManager();
        this.evolutionManager = plugin.getEvolutionManager();
        this.essenceManager = plugin.getEssenceManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "stats" -> handleStats(sender, args);
            case "ability" -> handleAbility(sender);
            case "evolve" -> handleEvolve(sender);
            case "gui" -> handleGUI(sender);
            case "withdraw" -> handleWithdraw(sender, args);
            default -> showHelp(sender);
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        Component message = Component.text("TwistedSMP Commands", NamedTextColor.GOLD)
            .append(Component.newline())
            .append(Component.text("/twist gui", NamedTextColor.YELLOW).hoverEvent(HoverEvent.text(Component.text("Open twist selection GUI"))).append(Component.space()))
            .append(Component.text("/twist stats", NamedTextColor.YELLOW).hoverEvent(HoverEvent.text(Component.text("View your stats"))).append(Component.space()))
            .append(Component.newline())
            .append(Component.text("/twist ability", NamedTextColor.YELLOW).hoverEvent(HoverEvent.text(Component.text("Use your Twist ability"))))
            .append(Component.text("/twist evolve", NamedTextColor.YELLOW).hoverEvent(HoverEvent.text(Component.text("Evolve your Twist"))))
            .append(Component.text("/twist withdraw <amount>", NamedTextColor.YELLOW).hoverEvent(HoverEvent.text(Component.text("Convert essence to items"))));
        player.sendMessage(message);
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Players only."));
            return;
        }
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Data not loaded."));
            return;
        }
        Twist twist = data.getTwist();
        String twistName = twistManager.getTwistDisplayName(twist);
        String stageName = energyManager.getStageName(data.getEnergy());
        String energyColor = energyManager.getStageColor(data.getEnergy());

        Component header = Component.text("Twist Stats".toUpperCase(), NamedTextColor.GOLD);
        Component body = Component.empty()
            .append(statsLine("Twist", twistName, NamedTextColor.WHITE))
            .append(Component.newline())
            .append(statsLine("Stage", stageName, NamedTextColor.YELLOW))
            .append(Component.newline())
            .append(statsLine("Energy", String.format("%.0f%%", data.getEnergy()), NamedTextColor.AQUA))
            .append(Component.newline())
            .append(statsLine("Essence", String.format("%.0f", data.getEssence()), NamedTextColor.LIGHT_PURPLE))
            .append(Component.newline())
            .append(statsLine("Instability", String.format("%.0f%%", data.getInstability()), NamedTextColor.RED))
            .append(Component.newline())
            .append(statsLine("Kills", String.valueOf(data.getKills()), NamedTextColor.GREEN))
            .append(Component.newline())
            .append(statsLine("Deaths", String.valueOf(data.getDeaths()), NamedTextColor.DARK_RED));

        player.sendMessage(header);
        player.sendMessage(body);
    }

    private Component statsLine(String label, String value, NamedTextColor color) {
        return Component.text(label + ": ", NamedTextColor.GRAY).append(Component.text(value, color));
    }

    private void handleAbility(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have not selected a twist yet! Use /twist gui"));
            return;
        }
        plugin.getAbilityManager().useAbility(player, data.getTwist());
    }

    private void handleEvolve(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have not selected a twist yet! Use /twist gui"));
            return;
        }
        boolean success = evolutionManager.evolve(data);
        if (!success) {
            var reqs = evolutionManager.getRequirements(data);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<red>Cannot evolve yet. Need: <gold>" + reqs.energyRequired() + "</gold> Energy and <dark_purple>" + reqs.essenceRequired() + "</dark_purple> Essence"));
        }
    }

    private void handleWithdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have not selected a twist yet! Use /twist gui"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /twist withdraw <amount|all>"));
            return;
        }

        double amount;
        if (args[1].equalsIgnoreCase("all")) {
            amount = data.getEssence();
        } else {
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid amount."));
                return;
            }
        }

        if (amount <= 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Withdraw amount must be positive."));
            return;
        }

        if (data.getEssence() < amount) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                configManager.getMessage("withdraw-fail", "amount", String.valueOf((int) amount))));
            return;
        }

        data.subtractEssence(amount);
        plugin.getDataManager().savePlayerData(data, true);

        int stackSize = (int) Math.min(amount, 64);
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, stackSize);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("Twisted Essence", net.kyori.adventure.text.color.TextColor.color(0xa29bfe)));
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("Worth: " + (int) amount + " Essence", net.kyori.adventure.text.color.TextColor.color(0xbb86fc)));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
        player.sendMessage(MiniMessage.miniMessage().deserialize(
            configManager.getMessage("withdraw-success", "amount", String.valueOf((int) amount))));
    }

    private void handleGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;
        if (data.isTwistSelected()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have already selected a twist: " + twistManager.getTwistDisplayName(data.getTwist())));
            return;
        }
        twistManager.openTwistSelectionGUI(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (args.length == 1) {
            return Arrays.asList("stats", "ability", "evolve", "gui").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
