package main.java.org.matejko.discordsystem.configuration;

import org.bukkit.util.config.Configuration;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CensorshipRulesManager {
    private final File dataFolder;
    private Configuration ownRulesConfig;
    private Set<String> blacklist = new HashSet<String>();
    private Set<String> whitelist = new HashSet<String>();
    private boolean rulesLoaded = false;
    private static DiscordPlugin plugin;
	private Config config;

    public CensorshipRulesManager(File dataFolder, Config config, DiscordPlugin plugin) {
    	this.config = config;
    	CensorshipRulesManager.plugin = plugin;
        this.dataFolder = dataFolder;
        loadOrCreateOwnRulesFile();
        ensureRulesLoaded();
    }

    private void loadOrCreateOwnRulesFile() {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File rulesFile = new File(dataFolder, "censorship_rules.yml");
            if (!rulesFile.exists()) {
                rulesFile.createNewFile();

                List<String> defaultBlacklist = new ArrayList<String>();
                defaultBlacklist.add("word1");
                defaultBlacklist.add("word2");

                List<String> defaultWhitelist = new ArrayList<String>();
                defaultWhitelist.add("wordd1");
                defaultWhitelist.add("wordd2");

                Configuration defaultConfig = new Configuration(rulesFile);
                defaultConfig.load();

                defaultConfig.setProperty("Blacklisted", defaultBlacklist);
                defaultConfig.setProperty("Whitelisted", defaultWhitelist);
                defaultConfig.save();
            }
            ownRulesConfig = new Configuration(new File(dataFolder, "censorship_rules.yml"));
            ownRulesConfig.load();
        } catch (IOException e) {
            e.printStackTrace();
            ownRulesConfig = null;
        }
    }

    public boolean censorshipEnabled() {
        if (config == null) return false;
        return config.censorshipEnabled();
    }

    public boolean ownRules() {
        if (config == null) return true;
        return config.ownRules();
    }

    public Set<String> getBlacklist() {
        ensureRulesLoaded();
        return blacklist;
    }

    public Set<String> getWhitelist() {
        ensureRulesLoaded();
        return whitelist;
    }

    public void reloadRules() {
        loadOrCreateOwnRulesFile();
        rulesLoaded = false;  // force reload on next get
        ensureRulesLoaded();
    }

    private void ensureRulesLoaded() {
        if (rulesLoaded) return;

        blacklist.clear();
        whitelist.clear();

        if (!censorshipEnabled()) {
            rulesLoaded = true;
            return;
        }

        if (ownRules()) {
            if (ownRulesConfig != null) {
                // Read Blacklisted list
                List<?> black = ownRulesConfig.getList("Blacklisted");
                if (black != null) {
                    for (Object b : black) {
                        if (b instanceof String) {
                            blacklist.add(((String) b).toLowerCase());
                        }
                    }
                }

                // Read Whitelisted list
                List<?> white = ownRulesConfig.getList("Whitelisted");
                if (white != null) {
                    for (Object w : white) {
                        if (w instanceof String) {
                            whitelist.add(((String) w).toLowerCase());
                        }
                    }
                }

                // Compare to default values
                Set<String> defaultBlacklist = new HashSet<String>();
                defaultBlacklist.add("word1");
                defaultBlacklist.add("word2");

                Set<String> defaultWhitelist = new HashSet<String>();
                defaultWhitelist.add("wordd1");
                defaultWhitelist.add("wordd2");

                if (blacklist.equals(defaultBlacklist) && whitelist.equals(defaultWhitelist)) {
                    System.err.println("[CensorshipRules] Rules match default values. Modify your censorship_rules.yml !!!");
                    System.err.println("[CensorshipRules] Rules match default values. Modify your censorship_rules.yml !!!");
                    System.err.println("[CensorshipRules] Rules match default values. Modify your censorship_rules.yml !!!");
                    System.err.println("[CensorshipRules] Disabled !");
                    rulesLoaded = true;
                    return;
                }
            }
        } else {
            File chatGuardConfigFile = new File("plugins/ChatGuard/config/config.yml");
            if (!chatGuardConfigFile.exists()) {
                plugin.getLogger().info("[CensorshipRules] ChatGuard config file not found.");
                rulesLoaded = true;
                return;
            }

            Configuration chatGuardConfig = new Configuration(chatGuardConfigFile);
            chatGuardConfig.load();

            List<?> whiteListFromChatGuard = chatGuardConfig.getList("filter.rules.terms.whitelist");
            if (whiteListFromChatGuard != null) {
                for (Object w : whiteListFromChatGuard) {
                    if (w instanceof String) {
                        whitelist.add(((String) w).toLowerCase());
                    }
                }
            }

            List<?> blackListFromChatGuard = chatGuardConfig.getList("filter.rules.terms.blacklist");
            if (blackListFromChatGuard != null) {
                for (Object b : blackListFromChatGuard) {
                    if (b instanceof String) {
                        blacklist.add(((String) b).toLowerCase());
                    }
                }
            }
        }

        if (config.debugEnabled()) {
            plugin.getLogger().info("[DEBUG] [CensorshipRules] Loaded blacklist:");
            for (String word : blacklist) {
                plugin.getLogger().info("  - " + word);
            }
            plugin.getLogger().info("[DEBUG] [CensorshipRules] Loaded whitelist:");
            for (String word : whitelist) {
                plugin.getLogger().info("  - " + word);
            }
        }

        rulesLoaded = true;
    }
    
    public void saveRules() {
        if (ownRulesConfig == null) {
            plugin.getLogger().info("[CensorshipRules] Cannot save: config is null.");
            return;
        }

        List<String> blackListToSave = new ArrayList<String>(blacklist);
        List<String> whiteListToSave = new ArrayList<String>(whitelist);

        ownRulesConfig.setProperty("Blacklisted", blackListToSave);
        ownRulesConfig.setProperty("Whitelisted", whiteListToSave);

        try {
            ownRulesConfig.save();
            reloadRules();
        } catch (Exception e) {
            plugin.getLogger().info("[CensorshipRules] Failed to save rules:");
            e.printStackTrace();
        }
    }
    public String censorText(String input) {
        Set<String> blacklist = getBlacklist();
        Set<String> whitelist = getWhitelist();

        // Basic leetspeak normalization map
        Map<Character, Character> leetMap = new HashMap<>();
        leetMap.put('1', 'i');
        leetMap.put('!', 'i');
        leetMap.put('3', 'e');
        leetMap.put('4', 'a');
        leetMap.put('@', 'a');
        leetMap.put('7', 't');
        leetMap.put('0', 'o');
        leetMap.put('$', 's');
        leetMap.put('5', 's');

        StringBuilder result = new StringBuilder();
        for (String word : input.split(" ")) {
            String stripped = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            String normalized = normalizeLeet(stripped, leetMap);

            if (whitelist.contains(normalized)) {
                result.append(word).append(" ");
            } else if (blacklist.contains(normalized)) {
                result.append(repeatChar('#', word.length())).append(" ");
            } else {
                result.append(word).append(" ");
            }
        }
        return result.toString().trim();
    }

    // Helper to normalize basic leetspeak
    private String normalizeLeet(String input, Map<Character, Character> leetMap) {
        StringBuilder normalized = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (leetMap.containsKey(c)) {
                normalized.append(leetMap.get(c));
            } else {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    // Helper to replace word with censor chars
    private String repeatChar(char c, int times) {
        char[] arr = new char[times];
        Arrays.fill(arr, c);
        return new String(arr);
    }

}
