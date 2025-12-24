package main.java.org.matejko.discordsystem.configuration;

import org.bukkit.util.config.Configuration;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import java.io.*;
import java.util.Map;

public class ConfigUpdater {
    private final DiscordPlugin plugin;
    private final File configFile;
    private final String resourceName = "config.yml";
    public ConfigUpdater(DiscordPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), resourceName);
    }
    public void checkAndUpdateConfig() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            if (!configFile.exists()) {
                copyDefault();
                plugin.getLogger().info("[DiscordSystem] Config created.");
                return;
            }
            Configuration userConfig = new Configuration(configFile);
            userConfig.load();
            Configuration defaultConfig = loadDefaultConfig();
            String userVersion = userConfig.getString("version"); 
            String internalVersion = defaultConfig.getString("version");
            if (userVersion == null || isVersionOutdated(userVersion, internalVersion)) {
                plugin.getLogger().info("[DiscordSystem] Config is outdated (v" + userVersion + " -> v" + internalVersion + "). Updating...");
                merge(defaultConfig.getAll(), userConfig);
                userConfig.setProperty("version", internalVersion);
                userConfig.save();
                plugin.getLogger().info("[DiscordSystem] Config updated successfully.");
            } else {
                plugin.getLogger().info("[DiscordSystem] Config is up to date." + " (v" + internalVersion+ ")");
            }
        } catch (Exception e) {
            plugin.getLogger().info("[DiscordSystem] Config update failed!");
            e.printStackTrace();
        }
    }
    private boolean isVersionOutdated(String current, String internal) {
        if (current == null) return true;
        if (internal == null) return false;
        try {
            double cur = Double.parseDouble(current);
            double inter = Double.parseDouble(internal);
            return cur < inter;
        } catch (NumberFormatException e) {
            return !current.equalsIgnoreCase(internal);
        }
    }
    private void copyDefault() throws IOException {
        InputStream in = getResource(resourceName);
        if (in == null) {
            throw new FileNotFoundException("Default config.yml not found in JAR");
        }
        try (OutputStream out = new FileOutputStream(configFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }
    private Configuration loadDefaultConfig() throws IOException {
        InputStream in = getResource(resourceName);
        if (in == null) {
            throw new FileNotFoundException("Default config.yml not found in JAR");
        }
        File temp = File.createTempFile("default-config", ".yml");
        temp.deleteOnExit();
        try (OutputStream out = new FileOutputStream(temp)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
        Configuration config = new Configuration(temp);
        config.load();
        return config;
    }
    private InputStream getResource(String name) {
        return plugin.getClass().getClassLoader().getResourceAsStream(name);
    }
    private void merge(Map<String, Object> defaults, Configuration userConfig) {
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            Object defValue = entry.getValue();
            if (key.equalsIgnoreCase("version")) continue;
            Object userValue = userConfig.getProperty(key);
            if (userValue == null) {
                userConfig.setProperty(key, defValue);
            }
        }
    }
}