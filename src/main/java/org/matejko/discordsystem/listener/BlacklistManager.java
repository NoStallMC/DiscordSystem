package main.java.org.matejko.discordsystem.listener;

import org.bukkit.util.config.Configuration;
import main.java.org.matejko.discordsystem.configuration.Config;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class BlacklistManager {
    @SuppressWarnings("unused")
    private JavaPlugin plugin;
    private Configuration blacklistConfig;
    private File blacklistFile;
	private Config config;

    // Constructor: Initializes the Blacklist Manager
    public BlacklistManager(JavaPlugin plugin, Config config) {
    	this.config = config;
        this.plugin = plugin;

        // Set Path for Blacklist File and Create Directory if Necessary
        File discordBotDir = new File(plugin.getDataFolder().getParentFile(), "DiscordSystem");
        if (!discordBotDir.exists()) {
            discordBotDir.mkdirs();
        }
        blacklistFile = new File(discordBotDir, "shell_blacklist.yml");

        /////////////////////////////////////////////////////////////////////////////
        // Create or Load Blacklist File
        /////////////////////////////////////////////////////////////////////////////
        if (!blacklistFile.exists()) {
            createDefaultBlacklistFile();
        }

        /////////////////////////////////////////////////////////////////////////////
        // Initialize Configuration After File Creation
        /////////////////////////////////////////////////////////////////////////////
        blacklistConfig = new Configuration(blacklistFile);
        loadBlacklistConfig();
    }

    /////////////////////////////////////////////////////////////////////////////
    // Creates the Default Blacklist File if It Doesn't Exist
    /////////////////////////////////////////////////////////////////////////////
    private void createDefaultBlacklistFile() {
        try {
            if (blacklistFile.createNewFile()) {
                System.out.println("Created default blacklist.yml file.");
                blacklistConfig = new Configuration(blacklistFile);
                saveDefaults();
            }
        } catch (IOException e) {
            System.out.println("Failed to create blacklist.yml: " + e.getMessage());
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // Saves Default Values to the Blacklist File
    /////////////////////////////////////////////////////////////////////////////
    private void saveDefaults() {
        blacklistConfig.setProperty("blacklisted-commands", Arrays.asList("command1", "command2"));
        blacklistConfig.save();
    }

    /////////////////////////////////////////////////////////////////////////////
    // Loads the Blacklist Configuration from the File
    /////////////////////////////////////////////////////////////////////////////
    private void loadBlacklistConfig() {
        if (blacklistConfig != null) {
            blacklistConfig.load();
        }

        /////////////////////////////////////////////////////////////////////////////
        // Debug Output: List Loaded Blacklisted Commands
        /////////////////////////////////////////////////////////////////////////////
        if (config.debugEnabled()) {
        List<String> blacklistedCommands = getBlacklistedCommands();
        if (blacklistedCommands.isEmpty()) {
            System.out.println("No blacklisted commands found.");
        } else {
            System.out.println("Loaded blacklisted commands:");
            for (String command : blacklistedCommands) {
                System.out.println(" - " + command);
            }
        }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // Retrieves List of Blacklisted Commands
    /////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unchecked")
    public List<String> getBlacklistedCommands() {
        Object property = blacklistConfig.getProperty("blacklisted-commands");
        if (property instanceof List) {
            return (List<String>) property;
        }
        return Arrays.asList(); // Return an empty list if property is invalid
    }

    /////////////////////////////////////////////////////////////////////////////
    // Checks if a Command is Blacklisted
    /////////////////////////////////////////////////////////////////////////////
    public boolean isCommandBlacklisted(String command) {
        List<String> blacklistedCommands = getBlacklistedCommands();
        return blacklistedCommands.contains(command);
    }
}
