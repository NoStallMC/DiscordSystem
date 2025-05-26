package main.java.org.matejko.discordsystem;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.listener.PlayerListUpdater;
import main.java.org.matejko.discordsystem.listener.PlaytimeManager;
import main.java.org.matejko.discordsystem.utils.ActivityManager;

public final class DiscordPlugin extends JavaPlugin {
    private static DiscordPlugin instance;
    private static PlayerListUpdater updater;
    private Logger logger;
    private PlaytimeManager playtimeManager;
    private Config config;

    @Override
    public void onEnable() {
        this.logger = Logger.getLogger("DiscordSystem");
        instance = this;

        // Initialize config first!
        config = new Config(this);
        
        // Initialize Discord-related handlers (JDA, webhook, config)
        GetterHandler.initialize(this, this);
        new ActivityManager(config);

        // Register events and commands
        if (!this.isEnabled()) return;
        PluginRegister.registerAll(this, this, config);
        DeathLoggerHandler.register(config);

        // Init PlaytimeManager & register events
        if (config.statusEnabled()) {
        playtimeManager = new PlaytimeManager(config);
        getServer().getPluginManager().registerEvents(playtimeManager, this);
        playtimeManager.resetAllOnlinePlayers();

        // Init PlayerListUpdater and register events
        updater = new PlayerListUpdater(playtimeManager, config);
        getServer().getPluginManager().registerEvents(updater, this);
        updater.startUpdating();
        }
    }

    @Override
    public void onDisable() {
        GetterHandler.shutdown();
    }

    public static DiscordPlugin instance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }
    
    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }

    public static PlayerListUpdater updater() {
        return updater;
    }
}
