package main.java.org.matejko.discordsystem.listener;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.util.config.Configuration;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.GetterHandler;
import main.java.org.matejko.discordsystem.configuration.CensorshipRulesManager;
import main.java.org.matejko.discordsystem.configuration.Config;
import java.io.File;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public final class SignLoggerListener extends ListenerAdapter implements Listener {
    private static final Map<String, Location> signMap = new HashMap<>();
    private static File SIGN_FILE;

    private final Config config;
    private final CensorshipRulesManager censorship;

    public SignLoggerListener(DiscordPlugin plugin, Config config) {
        this.config = config;
        this.censorship = plugin.getCensorshipRules();
        SIGN_FILE = new File(plugin.getDataFolder(), "signs.yml");
        initializeFile();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Initialization Method
    ////////////////////////////////////////////////////////////////////////////////

    public static void initializeFile() {
        File folder = DiscordPlugin.instance().getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        if (!SIGN_FILE.exists()) {
            try {
                SIGN_FILE.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadSignMap() {
        if (!SIGN_FILE.exists()) return;
        Configuration config = new Configuration(SIGN_FILE);
        config.load();
        List<String> keys = config.getKeys("signs");
        if (keys == null) return;
        for (String signID : keys) {
            int x = config.getInt("signs." + signID + ".x", 0);
            int y = config.getInt("signs." + signID + ".y", 0);
            int z = config.getInt("signs." + signID + ".z", 0);
            String worldName = config.getString("signs." + signID + ".world", "world");
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Location loc = new Location(world, x, y, z);
                signMap.put(signID, loc);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Saving into file
    ////////////////////////////////////////////////////////////////////////////////

    private void saveSignMap() {
        Configuration config = new Configuration(SIGN_FILE);
        config.load();
        for (Map.Entry<String, Location> entry : signMap.entrySet()) {
            String signID = entry.getKey();
            Location loc = entry.getValue();
            String path = "signs." + signID;
            config.setProperty(path + ".x", loc.getBlockX());
            config.setProperty(path + ".y", loc.getBlockY());
            config.setProperty(path + ".z", loc.getBlockZ());
            config.setProperty(path + ".world", loc.getWorld().getName());
            if (config.getString(path + ".timestamp") == null) {
                String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                config.setProperty(path + ".timestamp", now);
            }
        }
        config.save();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Event Handling Methods
    ////////////////////////////////////////////////////////////////////////////////

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Location loc = event.getBlock().getLocation();
        long timestamp = System.currentTimeMillis();
        String signID = generateSignID(loc, timestamp);

        signMap.put(signID, loc);
        Configuration config = new Configuration(SIGN_FILE);
        config.load();
        String path = "signs." + signID;
        String formattedContent = formatSignContent(event);

        config.setProperty(path + ".author", event.getPlayer().getName());
        String[] lines = formattedContent.split("\n");
        for (int i = 0; i < 4; i++) {
            config.setProperty(path + ".line" + (i + 1), lines[i]);
        }
        config.setProperty(path + ".x", loc.getBlockX());
        config.setProperty(path + ".y", loc.getBlockY());
        config.setProperty(path + ".z", loc.getBlockZ());
        config.setProperty(path + ".world", loc.getWorld().getName());
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
        config.setProperty(path + ".timestamp", date);

        String signChannelId = GetterHandler.configuration().signChannelId();
        GetterHandler.jda().getTextChannelById(signChannelId).sendMessage(String.format(
            "**New Sign Placed by:** `%s`\n" +
            "**Date:** `%s`\n" +
            "**Location:** `X: %d, Y: %d, Z: %d`\n" +
            "**World:** `%s`\n" +
            "**SignID:** `%s`\n" +
            "**Sign Content:**\n" +
            "```\n%s\n```",
            event.getPlayer().getName(), date, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
            loc.getWorld().getName(), signID, String.join("\n", lines)
        )).queue(message -> {
            config.setProperty(path + ".messageId", message.getId());
            config.save();
        });
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Discord Message Handling Methods
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getChannel().getId().equals(GetterHandler.configuration().signChannelId())) return;
        if (event.getAuthor().isBot() && !config.serverShellAllowBots()) return;
        if (event.isWebhookMessage()) return;
        if (event.getAuthor().getId().equals(config.botid())) return;

        List<String> allowedUsers = new ArrayList<>();
        for (Object obj : (List<?>) GetterHandler.configuration().shellAllowedUsers()) {
            allowedUsers.add(obj.toString());
        }
        if (!allowedUsers.contains(event.getAuthor().getId())) {
            event.getChannel().sendMessage(":no_entry_sign: You are not authorized to remove signs.").queue();
            return;
        }

        String msg = event.getMessage().getContentRaw().trim();
        if (!msg.toLowerCase().startsWith("remove ")) return;
        String signID = msg.substring("remove ".length()).trim();
        if (!signMap.containsKey(signID)) {
            event.getChannel().sendMessage(":x: SignID: `" + signID + "` not found.").queue();
            return;
        }

        Location loc = signMap.get(signID);
        String messageId = getMessageIdFromConfig(signID);

        Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordPlugin.instance(), () -> {
            Block block = loc.getBlock();
            if (block.getType().toString().contains("SIGN")) {
                block.setType(Material.AIR);
                event.getChannel().sendMessage(":white_check_mark: Removed sign with ID `" + signID + "`").queue();
                signMap.remove(signID);
                removeSignFromConfig(signID);
                saveSignMap();
                if (messageId != null) {
                    event.getChannel().retrieveMessageById(messageId).queue(message -> {
                        String newContent = "**⚠️ REMOVED ⚠️**\n" +
                                            "This sign was removed from the server by `" + event.getAuthor().getName() + "`.\n\n" +
                                            message.getContentRaw().replace("**Original Details:**\n", "");
                        message.editMessage(newContent).queue();
                    });
                }
            } else {
                event.getChannel().sendMessage(":warning: No sign found at that location anymore.").queue();
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Utility Methods
    ////////////////////////////////////////////////////////////////////////////////

    static String getMessageIdFromConfig(String signID) {
        Configuration config = new Configuration(SIGN_FILE);
        config.load();
        return config.getString("signs." + signID + ".messageId", null);
    }

    static void removeSignFromConfig(String signID) {
        Configuration config = new Configuration(SIGN_FILE);
        config.load();
        config.setProperty("signs." + signID, null);
        config.save();
    }

    private String formatSignContent(SignChangeEvent event) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String line = event.getLine(i);
            String censoredLine = censorContent(line);
            text.append("line").append(i + 1).append(": ").append(censoredLine).append("\n");
        }
        return text.toString().trim();
    }

    private String censorContent(String content) {
        if (!config.signCensorEnabled()) {
            return content;
        }
        return censorship.censorText(content);
    }

    private String generateSignID(Location loc, long timestamp) {
        try {
            String input = loc.toString() + "-" + timestamp + "-" + UUID.randomUUID();
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes());
            int shortHash = 0;
            for (int i = 0; i < 4; i++) {
                shortHash <<= 8;
                shortHash |= (hash[i] & 0xff);
            }
            shortHash &= 0x7FFFFFFF;
            return String.valueOf(shortHash);
        } catch (Exception e) {
            e.printStackTrace();
            return String.valueOf(timestamp);
        }
    }

    public static Map<String, Location> getSignMap() {
        return signMap;
    }
}
