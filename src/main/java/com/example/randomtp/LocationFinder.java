package com.example.randomtp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;

public class LocationFinder extends BukkitRunnable {
    private final RandomTPPlugin plugin;
    private final Player player;
    private final int maxAttempts;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private int attempts;

    public LocationFinder(RandomTPPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.maxAttempts = plugin.getConfig().getInt("teleport.max-attempts", 50);
        this.minX = plugin.getConfig().getInt("teleport.min-x", -10000);
        this.maxX = plugin.getConfig().getInt("teleport.max-x", 10000);
        this.minZ = plugin.getConfig().getInt("teleport.min-z", -10000);
        this.maxZ = plugin.getConfig().getInt("teleport.max-z", 10000);
        this.attempts = 0;
    }

    @Override
    public void run() {
        while (attempts < maxAttempts) {
            attempts++;
            final Location randomLocation = generateRandomLocation();
            if (isSafeLocation(randomLocation)) {
                teleportPlayer(randomLocation);
                return;
            }
        }
        syncSendMessage("§c无法在" + maxAttempts + "次尝试内找到安全位置！");
    }

    private Location generateRandomLocation() {
        final World world = player.getWorld();
        final int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
        final int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
        final int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x, y, z);
    }

    private boolean isSafeLocation(Location loc) {
        final Block ground = loc.getBlock().getRelative(BlockFace.DOWN);
        final Block feet = loc.getBlock();
        final Block head = feet.getRelative(BlockFace.UP);

        if (!ground.getType().isSolid() || !isPassable(feet.getType()) || !isPassable(head.getType())) {
            return false;
        }

        // 只检查脚下方块周围的熔岩
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // 跳过中心方块
                final Block block = ground.getRelative(dx, 0, dz);
                if (block.getType() == Material.LAVA) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPassable(Material material) {
        return material.isAir() || material == Material.WATER;
    }

    private void teleportPlayer(Location location) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                player.teleport(location.add(0.5, 0, 0.5));
                player.sendTitle("§a传送成功", "§7坐标: " + formatLocation(location), 10, 40, 10);
                player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            } catch (Exception e) {
                player.sendMessage("§c传送过程中发生错误！");
                plugin.getLogger().warning("传送玩家时出错: " + e.getMessage());
            }
        });
    }

    private void syncSendMessage(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message));
    }

    private String formatLocation(Location loc) {
        return String.format("X: %d, Y: %d, Z: %d",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}