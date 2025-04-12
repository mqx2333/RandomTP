package com.example.randomtp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 处理/randomtp命令执行
 */
public class RandomTPCommand implements CommandExecutor {
    private final RandomTPPlugin plugin;

    public RandomTPCommand(RandomTPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        if (!player.hasPermission("randomtp.use")) {
            player.sendMessage("§c你没有使用此命令的权限！");
            return true;
        }

        player.sendMessage("§e正在寻找安全位置...");
        new LocationFinder(plugin, player).runTaskAsynchronously(plugin);
        return true;
    }
}