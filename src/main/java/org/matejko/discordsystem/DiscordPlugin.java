package main.java.org.matejko.discordsystem;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.earth2me.essentials.Essentials;
import main.java.org.matejko.discordsystem.configuration.CensorshipRulesManager;
import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.listener.PlayerListUpdater;
import main.java.org.matejko.discordsystem.listener.PlaytimeManager;
import main.java.org.matejko.discordsystem.utils.ActivityManager;

public final class DiscordPlugin extends JavaPlugin {
    private static DiscordPlugin instance;
    private static PlayerListUpdater updater;
	private static CensorshipRulesManager censorshipRules;
    private PlaytimeManager playtimeManager;
	private Essentials essentials;
    private Logger logger;
    Config config;

    @Override
    public void onEnable() {
        this.logger = Logger.getLogger("DiscordSystem");
        instance = this;
        getLogger().info("[DiscordSystem] is starting up!");
        config = new Config(this);
        config.loadConfig();
        if ("YourBotToken".equals(config.token())) {
            logger.severe("===========================================");
            logger.severe("Discord token is not set! Disabling plugin!");
            logger.severe("===========================================");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        censorshipRules = new CensorshipRulesManager(getDataFolder(), config, this);
        Plugin p = Bukkit.getPluginManager().getPlugin("Essentials");
        if (p instanceof Essentials) {
            this.essentials = (Essentials) p;
        }
        if (config.statusEnabled()) { new ActivityManager(config, essentials, this); }
        PluginRegister.registerAll(this, this, config);
        if (config.statusEnabled()) {
        	playtimeManager = new PlaytimeManager(config, this);
        	getServer().getPluginManager().registerEvents(playtimeManager, this);
        	playtimeManager.resetAllOnlinePlayers();
        	updater = new PlayerListUpdater(playtimeManager, config, this);
        	getServer().getPluginManager().registerEvents(updater, this);
        }
        getLogger().info("[DiscordSystem] has been enabled!");
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
    public CensorshipRulesManager getCensorshipRules() {
        return censorshipRules;
    }
    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }
    public void startJdaDependentTasks() {
        if (config.statusEnabled() && updater != null) {
            updater.startUpdating();
        }
    }
    public static PlayerListUpdater updater() {
        return updater;
    }
}
