package main.java.org.matejko.discordsystem;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import main.java.org.matejko.discordsystem.configuration.ActivityTrackerConfig;
import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.listener.MessageReceiveListener;
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
            p.getLogger().severe("[DiscordSystem] Config is null!");
            p.getLogger().severe("[DiscordSystem] Config is null!");
            return;
        }
        ////////////////////////////////////////////////////////////////////////////////
        // Register plugin command executors
        ////////////////////////////////////////////////////////////////////////////////
        plugin.getCommand("signremove").setExecutor(new SignCommand());
        plugin.getCommand("censorship").setExecutor(new CensorshipCommand());
        PluginManager pm = plugin.getServer().getPluginManager();
        
        ////////////////////////////////////////////////////////////////////////////////
        // Register Plugin event listeners
        ////////////////////////////////////////////////////////////////////////////////
        PluginEventHandler handler = new PluginEventHandler(config, p);
        plugin.getServer().getPluginManager().registerEvents(handler, plugin);
        handler.initPresence();
        pm.registerEvents(new BlockActivityListener(), plugin);

        // Register Discord event listeners
        GetterHandler.jda().addEventListener(new MessageReceiveListener(config));

        // Initialize player activity tracking
        if (config.statusEnabled()) {
        ActivityTracker.init(plugin, config);

        // Load activity tracker config
        try {
            ActivityTrackerConfig.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
        // Initialize and load persistent sign logger state
        if (config.signEnabled()) {
        pm.registerEvents(new SignLoggerListener(p, config), plugin);
        SignLoggerListener.initializeFile();
        SignLoggerListener.loadSignMap();
        }
    }
}
