package com.example.randomtp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 异步处理位置搜索和验证（除传送外都在异步线程执行）
 * 注意：下列部分 Bukkit API 调用（如 getHighestBlockYAt、getType 等）通常要求在主线程调用，
 * 这里将这些调用放在异步中，可能会带来线程安全性问题，请充分测试后再使用于生产环境！
 */
public class LocationFinder extends BukkitRunnable {
    private final RandomTPPlugin plugin;
    private final Player player;
    private final int maxAttempts;
    private final int minX, maxX, minZ, maxZ;

    public LocationFinder(RandomTPPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        // 缓存配置中的传送范围和最大尝试次数
        this.maxAttempts = plugin.getConfig().getInt("teleport.max-attempts", 50);
        this.minX = plugin.getConfig().getInt("teleport.min-x", -10000);
        this.maxX = plugin.getConfig().getInt("teleport.max-x", 10000);
        this.minZ = plugin.getConfig().getInt("teleport.min-z", -10000);
        this.maxZ = plugin.getConfig().getInt("teleport.max-z", 10000);
    }

    @Override
    public void run() {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Location randomLocation = generateRandomLocation();

            // 以下调用均在异步线程中执行：计算最高方块高度、安全性检测等
            World world = player.getWorld();
            if (world == null) continue;
            int highestY = world.getHighestBlockYAt(randomLocation);
            randomLocation.setY(highestY + 1);

            if (isSafeLocation(randomLocation)) {
                // 传送操作在主线程执行
                teleportPlayer(randomLocation);
                return;
            }
        }
        syncSendMessage("§c无法在 " + maxAttempts + " 次尝试内找到安全位置！");
    }

    private Location generateRandomLocation() {
        World world = player.getWorld();
        int x = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
        int z = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
        return new Location(world, x, 0, z);
    }

    /**
     * 位置安全性验证，在异步线程中执行（存在线程安全风险，请谨慎使用）
     */
    private boolean isSafeLocation(Location loc) {
        // 注意：这里使用 loc.getBlock() 及后续方法在异步线程下调用可能有风险
        Block ground = loc.getBlock().getRelative(BlockFace.DOWN);
        Block feet = loc.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        return ground.getType().isSolid() &&
                isPassable(feet.getType()) &&
                isPassable(head.getType()) &&
                checkSurroundings(loc);
    }

    private boolean isPassable(Material material) {
        return material.isAir() || material == Material.WATER;
    }

    /**
     * 检查传送位置周围 3x3 地面的安全性，避免传送到附近有熔岩的地方
     * 该检查同样在异步中执行
     */
    private boolean checkSurroundings(Location loc) {
        Block base = loc.getBlock().getRelative(0, -1, 0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = base.getRelative(dx, 0, dz);
                if (block.getType() == Material.LAVA) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 仅将传送操作放在主线程执行，避免线程安全问题
     */
    private void teleportPlayer(Location location) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                // 移动到区块中心
                location.add(0.5, 0, 0.5);
                player.teleport(location);
                player.sendTitle("§a传送成功", "§7坐标: " + formatLocation(location), 10, 40, 10);
                player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            } catch (Exception e) {
                player.sendMessage("§c传送过程中发生错误！");
                plugin.getLogger().warning("传送玩家时出错: " + e.getMessage());
            }
        });
    }

    private String formatLocation(Location loc) {
        return String.format("X: %d, Y: %d, Z: %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private void syncSendMessage(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message));
    }
}