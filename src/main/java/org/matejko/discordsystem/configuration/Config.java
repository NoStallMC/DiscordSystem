package main.java.org.matejko.discordsystem.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.util.config.Configuration;
import main.java.org.matejko.discordsystem.DiscordPlugin;

public class Config {
    private static final String CONFIG_NAME = "config.yml";
    private static final String DEFAULT_CONFIG_PATH = "/" + CONFIG_NAME;
    private final DiscordPlugin plugin;
    private final File configFile;
    private Configuration config;
    private boolean loaded = false;

    public Config(DiscordPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), CONFIG_NAME);
        if (!configFile.exists()) {
            try {
                copyDefaultConfigToServer();
                plugin.getLogger().info("[DiscordConfig] Default config.yml copied from JAR.");
            } catch (IOException e) {
                plugin.getLogger().severe("[DiscordConfig] Failed to copy default config: " + e.getMessage());
                e.printStackTrace();
            }
        }
        loadConfig();
    }

    public void loadConfig() {
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

    private void copyDefaultConfigToServer() throws IOException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IOException("[DiscordConfig] Failed to create plugin data folder: " + dataFolder.getPath());
        }
        try (InputStream inputStream = getClass().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (inputStream == null) {
                throw new IOException("[DiscordConfig] Default config (" + CONFIG_NAME + ") not found in JAR resources.");
            }
            try (OutputStream outputStream = new FileOutputStream(configFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
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

    public String joinMessage() {
        return config.getString("messages.join-message", "%username% connected. (%onlineCount%/%maxCount%)");
    }

    public String quitMessage() {
        return config.getString("messages.quit-message", "%username% disconnected. (%onlineCount%/%maxCount%)");
    }

    public String chatMessage() {
        return config.getString("messages.game-chat-message", "%messageAuthor%: %message%");
    }

    public String messageFormat() {
        return config.getString("messages.chat-game-message", "&f[&bDiscord&f] &7%user%: %content%");
    }
    
    public List<String> getServerStartMessages() {
        return config.getStringList("messages.server-start-message", new ArrayList<String>());
    }

    public List<String> getServerShutdownMessages() {
        return config.getStringList("messages.server-shutdown-message", new ArrayList<String>());
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
    
    public boolean serverShellAllowBots() {
        return config.getBoolean("server-shell.allow-bots", false);
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

    public String statusChannelId() {
        return config.getString("status.channel-id", "YourStatusChannelID");
    }
    
    public boolean debugEnabled() {
        return config.getBoolean("debug", false);
    }
}
