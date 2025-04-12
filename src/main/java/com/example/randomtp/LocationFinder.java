package com.example.randomtp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Random;

/**
 * 异步处理位置搜索和验证
 */
public class LocationFinder extends BukkitRunnable {
    private static final Random RANDOM = new Random();
    private final RandomTPPlugin plugin;
    private final Player player;
    private int attempts;

    public LocationFinder(RandomTPPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.attempts = 0;
    }

    @Override
    public void run() {
        final int maxAttempts = plugin.getConfig().getInt("teleport.max-attempts", 50);
        if (attempts++ >= maxAttempts) {
            syncSendMessage("§c无法在" + maxAttempts + "次尝试内找到安全位置！");
            return;
        }

        final Location randomLocation = generateRandomLocation();
        new LocationValidator(randomLocation, attempts).runTask(plugin);
    }

    private Location generateRandomLocation() {
        final World world = player.getWorld();
        final int x = randomCoord("teleport.min-x", "teleport.max-x");
        final int z = randomCoord("teleport.min-z", "teleport.max-z");
        return new Location(world, x, 0, z);
    }

    private int randomCoord(String minPath, String maxPath) {
        return RANDOM.nextInt(
                plugin.getConfig().getInt(maxPath, 10000) -
                        plugin.getConfig().getInt(minPath, -10000)
        ) + plugin.getConfig().getInt(minPath, -10000);
    }

    private void syncSendMessage(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message));
    }

    /**
     * 同步位置验证任务
     */
    private class LocationValidator extends BukkitRunnable {
        private final Location location;
        private final int attemptCount;

        LocationValidator(Location location, int attemptCount) {
            this.location = location;
            this.attemptCount = attemptCount;
        }

        @Override
        public void run() {
            try {
                validateAndTeleport();
            } catch (Exception e) {
                plugin.getLogger().warning("位置验证时发生异常: " + e.getMessage());
            }
        }

        private void validateAndTeleport() {
            location.setY(location.getWorld().getHighestBlockYAt(location) + 1);

            if (isSafeLocation(location)) {
                teleportPlayer(location);
            } else {
                new LocationFinder(plugin, player).runTaskAsynchronously(plugin);
            }
        }

        private boolean isSafeLocation(Location loc) {
            final Block ground = loc.getBlock().getRelative(BlockFace.DOWN);
            final Block feet = loc.getBlock();
            final Block head = feet.getRelative(BlockFace.UP);

            return ground.getType().isSolid() &&
                    isPassable(feet.getType()) &&
                    isPassable(head.getType()) &&
                    checkSurroundings(loc);
        }

        private boolean isPassable(Material material) {
            return material.isAir() || material == Material.WATER;
        }

        private boolean checkSurroundings(Location loc) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final Block block = loc.getBlock().getRelative(dx, -1, dz);
                    if (block.getType() == Material.LAVA) {
                        return false;
                    }
                }
            }
            return true;
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

        private String formatLocation(Location loc) {
            return String.format("X: %d, Y: %d, Z: %d",
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
    }
}