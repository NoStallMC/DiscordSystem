package main.java.org.matejko.discordsystem.configuration;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.util.config.Configuration;
import main.java.org.matejko.discordsystem.DiscordPlugin;

public class Config {
    private static final String CONFIG_NAME = "config.yml";
    private final DiscordPlugin plugin;
    private final File configFile;
    private Configuration config;
    private boolean loaded = false;

    public Config(DiscordPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), CONFIG_NAME);
        new ConfigUpdater(plugin).checkAndUpdateConfig();
        loadConfig();
    }

    public void loadConfig() {
    	plugin.getLogger().info("[DiscordSystem] Initializing...");
        try {
            config = new Configuration(configFile);
            config.load();
            loaded = true;
        } catch (Exception e) {
            plugin.getLogger().severe("[DiscordConfig] Error loading config file: " + e.getMessage());
            e.printStackTrace();
            loaded = false;
        }
    }
    
    public List<String> getStringList(String path) {
        return config.getStringList(path, new ArrayList<String>());
    }
    
    public String getString(String path) {
        return config.getString(path, null);
    }
    
    public boolean isLoaded() {
        return loaded;
    }

    public File getConfigFile() {
        return configFile;
    }

    public String token() {
        return config.getString("token", "YourBotToken");
    }

    public String botid() {
        return config.getString("bot-id", "YourBotID");
    }

    public String serverName() {
        return config.getString("server-name", "MyServer");
    }

    public String bact() {
        return config.getString("bot-activity", "%ServerName% with %onlineCount% players online!");
    }

    public boolean webhookEnabled() {
        return config.getBoolean("webhook.enabled", true);
    }
    
    public boolean censorshipEnabled() {
        return config.getBoolean("censorship.enabled", false);
    }
    
    public boolean ownRules() {
        return config.getBoolean("censorship.own", true);
    }
    
    public String webhookUrl() {
        return config.getString("webhook.url", "YourWebhookURL");
    }

    public String messageChannelId() {
        return config.getString("messages.channel-id", "YourMessageChannelID");
    }
    
    public boolean messagesCensorEnabled() {
        return config.getBoolean("messages.censor", false);
    }
    
    public boolean smEnabled() {
        return config.getBoolean("messages.sleepmessages.enabled", false);
    }
    
    public String smFormat() {
        return config.getString("messages.sleepmessages-format", "***%sleepmessage%***");
    }
    
    public String joinMessage() {
        return config.getString("messages.join-message", "%username% connected. (%onlineCount%/%maxCount%)");
    }

    public String quitMessage() {
        return config.getString("messages.quit-message", "%username% disconnected. (%onlineCount%/%maxCount%)");
    }

    public String chatMessage() {
        return config.getString("messages.game-chat-message.message", "%messageAuthor%: %message%");
    }

    public String messageFormat() {
        return config.getString("messages.chat-game-message.message", "&f[&bDiscord&f] &7%user%: %content%");
    }
    public String getNormalServerStartMessages() {
        return config.getString("messages.normal-server-start-message", "%ServerName% is on!");
    }
    public String getNormalServerShutdownMessages() {
        return config.getString("messages.normal-server-shutdown-message", "%ServerName% is off!");
    }

    public boolean serverShellEnabled() {
        return config.getBoolean("server-shell.enabled", false);
    }

    public String shellChannelId() {
        return config.getString("server-shell.channel-id", "YourShellChannelID");
    }

    public List<String> shellAllowedUsers() {
        List<String> defaults = new ArrayList<>();
        defaults.add("player1");
        defaults.add("player2");
        return config.getStringList("server-shell.allowed-users", defaults);
    }

    public List<String> shellAllowedRoles() {
        List<String> defaults = new ArrayList<>();
        defaults.add("role1");
        defaults.add("role2");
        return config.getStringList("server-shell.allowed-roles", defaults);
    }

    public boolean serverShellAllowBots() {
        return config.getBoolean("server-shell.allow-bots.value", false);
    }

    public boolean checkEnabled() {
        return config.getBoolean("check.enabled", false);
    }

    public List<String> checkAllowedUsers() {
        List<String> defaults = new ArrayList<>();
        defaults.add("player1");
        defaults.add("player2");
        return config.getStringList("check.allowed-users", defaults);
    }

    public List<String> checkAllowedRoles() {
        List<String> defaults = new ArrayList<>();
        defaults.add("role1");
        defaults.add("role2");
        return config.getStringList("check.allowed-roles", defaults);
    }
    
    public boolean signEnabled() {
        return config.getBoolean("sign.enabled", false);
    }

    public String signChannelId() {
        return config.getString("sign.channel-id", "YourSignChannelID");
    }
    
    public boolean signCensorEnabled() {
        return config.getBoolean("sign.censor", false);
    }
    
    public boolean statusEnabled() {
        return config.getBoolean("status.enabled", false);
    }
    
    public String statusHeader() {
        return config.getString("status.header", "%greenOrb% Currently Online: %onlineCount%/%maxCount%");
    }
    
    public String statusChannelId() {
        return config.getString("status.channel-id", "YourStatusChannelID");
    }
    
    public boolean statusRegionEnabled() {
        return config.getBoolean("status.regions.enabled", false);
    }
    
    public boolean statusActivityEnabled() {
        return config.getBoolean("status.activity.enabled", false);
    }
    
    public boolean debugEnabled() {
        return config.getBoolean("debug", false);
    }

    public String configVersion() {
    	return config.getString("version", null);
    }
}
