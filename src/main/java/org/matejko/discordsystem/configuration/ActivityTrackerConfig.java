package main.java.org.matejko.discordsystem.configuration;

import org.bukkit.Material;
import org.bukkit.util.config.Configuration;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import java.io.*;
import java.util.*;

public class ActivityTrackerConfig {
    private static String pluginFolder;
    private static Configuration config;
    private static File configFile;
	private static DiscordPlugin plugin;
    private static final String CONFIG_NAME = "activity_config.yml";
    private static final String DEFAULT_CONFIG_PATH = "/" + CONFIG_NAME;
    
    public static void setPlugin(DiscordPlugin pluginInstance) {
        plugin = pluginInstance;
        pluginFolder = plugin.getDataFolder().getPath();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Loads the config file. If it does not exist, copies the default config.
    ////////////////////////////////////////////////////////////////////////////
    public static void load() throws IOException {
        configFile = new File(plugin.getDataFolder(), CONFIG_NAME);
        if (pluginFolder == null) {
            throw new IllegalStateException("Plugin folder not set! Call setPluginFolder() first.");
        }
        if (!configFile.exists()) {
            copyDefaultConfigToServer();
        }
        config = new Configuration(configFile);
        config.load();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Copies the default config file from the JAR to the server's plugin folder.
    ////////////////////////////////////////////////////////////////////////////
    private static void copyDefaultConfigToServer() throws IOException {
        File configDirectory = new File(pluginFolder);
        if (!configDirectory.exists() && !configDirectory.mkdirs()) {
            throw new IOException("[ActivityTrackerAPI] Failed to create directory: " + configDirectory.getPath());
        }
        try (InputStream inputStream = ActivityTrackerConfig.class.getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (inputStream == null) {
                throw new FileNotFoundException("[ActivityTrackerAPI] Default config (" + CONFIG_NAME + ") not found in JAR.");
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

    ////////////////////////////////////////////////////////////////////////////
    // Retrieves an integer value from config file.
    ////////////////////////////////////////////////////////////////////////////
    public static int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Retrieves a list of materials from config file.
    ////////////////////////////////////////////////////////////////////////////
    public static List<Material> getMaterialList(String path, List<Material> def) {
        List<String> stringList = config.getStringList(path, null);
        if (stringList == null) return def;

        List<Material> materials = new ArrayList<>();
        for (String name : stringList) {
            try {
                materials.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid material names
            }
        }
        return materials;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Retrieves a boolean value from the config file.
    ////////////////////////////////////////////////////////////////////////////
    public static boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Returns raw Configuration object for direct access.
    ////////////////////////////////////////////////////////////////////////////
    public static Configuration getRawConfig() {
        return config;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Retrieves a threshold value from config file.
    ////////////////////////////////////////////////////////////////////////////
    public static int getThreshold(String key, int def) {
        return config.getInt("Thresholds." + key, def);
    }
    public static String getActivityName(String key, String def) {
        if (config == null) return def;
        String name = config.getString("Names." + key.toLowerCase(), def);
        if (name == null || name.trim().isEmpty()) return def;
        return name;
    }
}
