package com.example.randomtp;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * 主插件类，处理插件生命周期和基础配置
 */
public class RandomTPPlugin extends JavaPlugin {
    private static final int CONFIG_VERSION = 1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        upgradeConfig();

        getCommand("randomtp").setExecutor(new RandomTPCommand(this));
        getServer().getLogger().info("[RandomTP] 插件已激活 - 版本 " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        getServer().getLogger().info("[RandomTP] 插件已卸载");
    }

    /**
     * 配置文件版本升级机制
     */
    private void upgradeConfig() {
        if (getConfig().getInt("config-version", 0) < CONFIG_VERSION) {
            getLogger().warning("检测到旧版配置文件，正在进行升级...");
            // 未来升级配置时添加迁移逻辑
            getConfig().set("config-version", CONFIG_VERSION);
            saveConfig();
        }
    }
}