package main.java.org.matejko.discordsystem;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import main.java.org.matejko.discordsystem.configuration.ActivityTrackerConfig;
import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.listener.SignCommand;
import main.java.org.matejko.discordsystem.listener.SignLoggerListener;
import main.java.org.matejko.discordsystem.utils.ActivityTracker;

public final class PluginRegister {
	////////////////////////////////////////////////////////////////////////////////
    // Registers all plugin events, commands, Discord listeners, and trackers
    ////////////////////////////////////////////////////////////////////////////////
    public static void registerAll(JavaPlugin plugin, DiscordPlugin p, Config config) {
        if (config == null) {
            p.getLogger().severe("[DiscordSystem] Config is null!");
            return;
        }
        plugin.getCommand("signremove").setExecutor(new SignCommand());
        plugin.getCommand("censorship").setExecutor(new CensorshipCommand());
        PluginManager pm = plugin.getServer().getPluginManager();
        PluginEventHandler handler = new PluginEventHandler(config, p);
        pm.registerEvents(handler, plugin);
        handler.registerSleepListener();
        pm.registerEvents(new BlockActivityListener(), plugin);
        GetterHandler.initialize(plugin, p, handler, config);
        DeathLoggerHandler.register(config, p);
        if (config.statusEnabled()) {
            try {
                ActivityTrackerConfig.setPlugin(p);
                ActivityTrackerConfig.load();
                ActivityTracker.init(p, config);
                p.getLogger().info("[DiscordSystem] ActivityTracker initialized successfully.");
            } catch (Exception e) {
                p.getLogger().severe("[DiscordSystem] Failed to initialize ActivityTracker!");
                e.printStackTrace();
            }
        }
        if (config.signEnabled()) {
            pm.registerEvents(new SignLoggerListener(p, config), plugin);
            SignLoggerListener.initializeFile();
            SignLoggerListener.loadSignMap();
        }
    }
}
