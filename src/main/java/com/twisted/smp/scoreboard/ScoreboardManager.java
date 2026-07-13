package com.twisted.smp.scoreboard;

import com.twisted.smp.TwistedSMP;
import com.twisted.smp.core.PlayerData;
import com.twisted.smp.twists.Twist;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager {

    private final TwistedSMP plugin;

    public ScoreboardManager(TwistedSMP plugin) {
        this.plugin = plugin;
    }

    public void update(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null || !data.isTwistSelected()) {
            org.bukkit.scoreboard.Scoreboard board = player.getServer().getScoreboardManager().getMainScoreboard();
            if (board != null) player.scoreboard(board);
            return;
        }

        org.bukkit.scoreboard.Scoreboard board = player.getServer().getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("twisted", "dummy", MiniMessage.miniMessage().serialize(
            net.kyori.adventure.text.Component.text("Twisted SMP").color(TextColor.color(0xff6b6b))));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        String twistColor = switch (data.getTwist()) {
            case VOID -> "§5";
            case TITAN -> "§8";
            case BERSERKER -> "§c";
            case PHANTOM -> "§f";
            case INFERNAL -> "§6";
            case FROSTBORN -> "§b";
        };
        String twistName = plugin.getTwistManager().getTwistDisplayName(data.getTwist());

        line(obj, "§0 ", 11);
        line(obj, twistColor + twistName, 10);
        line(obj, "§7Energy: §b" + (int) Math.round(data.getEnergy()), 9);
        line(obj, "§7Essence: §d" + (int) Math.round(data.getEssence()), 8);
        line(obj, "§7Instability: §c" + (int) Math.round(data.getInstability()), 7);
        line(obj, "§7Stage: §e" + data.getEvolutionStage(), 6);
        line(obj, "§1 ", 5);

        player.scoreboard(board);
    }

    private void line(Objective obj, String text, int score) {
        Score s = obj.getScore(text);
        s.setScore(score);
    }
}
