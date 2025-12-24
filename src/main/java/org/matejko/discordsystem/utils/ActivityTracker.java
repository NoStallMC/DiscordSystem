package main.java.org.matejko.discordsystem.utils;

import org.bukkit.entity.Player;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.configuration.ActivityTrackerConfig;
import main.java.org.matejko.discordsystem.configuration.Config;
import org.bukkit.Server;
import java.util.*;

public class ActivityTracker {

    ////////////////////////////////////////////////////////////////////////////////
    // Enum representing different types of player activities
    ////////////////////////////////////////////////////////////////////////////////
    public enum PlayerActivity {
    	AFK, BUILDING, GATHERING, HUNTING, MINING, FISHING, FARMING, EXPLORING_NETHER
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Internal tracking data structures
    ////////////////////////////////////////////////////////////////////////////////
    static final Map<UUID, Double> activityStrength = new HashMap<>();
    static final Map<UUID, PlayerActivity> playerActivities = new HashMap<>();
    static final Map<UUID, Long> activitySetTime = new HashMap<>();
    private static final Map<UUID, PlayerActivity> pausedActivity = new HashMap<>();
    private static final Map<UUID, Double> pausedStrength = new HashMap<>();
    private static DiscordPlugin plugin;
	private static Config config;

    ////////////////////////////////////////////////////////////////////////////////
    // Initializes the tracker and starts the decay task
    ////////////////////////////////////////////////////////////////////////////////
    public static void init(DiscordPlugin pl, Config config) {
		ActivityTracker.config = config;
        plugin = pl;
        Server server = plugin.getServer();
        
        // Configurable decay settings
        int decayTimeSeconds = ActivityTrackerConfig.getInt("Decay-Time", 10);
        int decaySpeedTicks = ActivityTrackerConfig.getInt("Decay-Speed", 20);
        double decayPerSecond = 100.0 / decayTimeSeconds;
        double ticksPerSecond = 20.0;
        double decayIntervalSeconds = decaySpeedTicks / ticksPerSecond;
        double decayPerInterval = decayPerSecond * decayIntervalSeconds;

        // Schedule decay task
        server.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
                Iterator<Map.Entry<UUID, Double>> iter = activityStrength.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<UUID, Double> entry = iter.next();
                    UUID uuid = entry.getKey();
                    double currentStrength = entry.getValue();
                    PlayerActivity activity = playerActivities.get(uuid);
                    long activityStartTime = activitySetTime.getOrDefault(uuid, 0L);
                    if (activityStartTime == 0L) continue;
                    double newStrength = currentStrength - decayPerInterval;
                    if (newStrength <= 0.0) {
                        iter.remove();
                        playerActivities.remove(uuid);
                        activitySetTime.remove(uuid);
                        if (config.debugEnabled()) {
                        	plugin.getLogger().info("[ActivityTracker] Cleared expired activity for: " + uuid);
                        }
                    } else {
                        activityStrength.put(uuid, newStrength);
                        if (activity != null && config.debugEnabled()) {
                            plugin.getLogger().info("[DEBUG] " + uuid + " current activity: " + activity + " at strength " + String.format("%.2f", newStrength) + "%");
                            plugin.getLogger().info("[ActivityTracker] Decay time: " + decayTimeSeconds + "s -> " + String.format("%.2f", decayPerSecond) + "% per second");
                            plugin.getLogger().info("[ActivityTracker] Decay interval: " + decaySpeedTicks + " ticks (" + String.format("%.2f", decayIntervalSeconds) + "s) -> " + String.format("%.2f", decayPerInterval) + "% per check");
                        }
                    }
                }
            }
        }, decaySpeedTicks, decaySpeedTicks);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Sets a player's activity and updates the strength
    ////////////////////////////////////////////////////////////////////////////////
    public static void setActivity(Player player, PlayerActivity activity, int strengthDelta) {
        if (strengthDelta < 10) return;
        UUID uuid = player.getUniqueId();
        PlayerActivity current = playerActivities.get(uuid);
        if (current == PlayerActivity.AFK && activity != PlayerActivity.AFK) {
            pausedActivity.put(uuid, activity);
            pausedStrength.put(uuid, (double) strengthDelta);
            return;
        }
        if (activity == PlayerActivity.AFK && current != PlayerActivity.AFK && current != null) {
            pausedActivity.put(uuid, current);
            pausedStrength.put(uuid, activityStrength.getOrDefault(uuid, 0.0));
        }
        if (current != null && current == activity) {
            double currentStrength = activityStrength.getOrDefault(uuid, 0.0);
            double newStrength = Math.min(100.0, currentStrength + strengthDelta);
            activityStrength.put(uuid, newStrength);
            if (config.debugEnabled()) {
            	plugin.getLogger().info("[DEBUG] Updated activity strength for " + player.getName() + ": " + String.format("%.2f", currentStrength) + "% -> " + String.format("%.2f", newStrength) + "%");
            }
        } else {
            playerActivities.put(uuid, activity);
            activityStrength.put(uuid, Math.min(100.0, strengthDelta));
            if (config.debugEnabled()) {
            	plugin.getLogger().info("[ActivityTracker] Set activity for " + player.getName() + ": " + activity + " at strength " + strengthDelta + "%");
            }
        }
        activitySetTime.put(uuid, System.currentTimeMillis());
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Gets the current activity for a player
    ////////////////////////////////////////////////////////////////////////////////
    public static PlayerActivity getActivity(Player player) {
        return playerActivities.get(player.getUniqueId());
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    // Checks if a player currently has any active activity
    ////////////////////////////////////////////////////////////////////////////////
    public static boolean isActive(Player player) {
        return getActivity(player) != null;
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    // Manually clears all activity data for a player
    ////////////////////////////////////////////////////////////////////////////////
    public static void clearActivity(Player player) {
        UUID uuid = player.getUniqueId();
        playerActivities.remove(uuid);
        activitySetTime.remove(uuid);
        activityStrength.remove(uuid);
        pausedActivity.remove(uuid);
        pausedStrength.remove(uuid);
        if (config.debugEnabled()) {
        	plugin.getLogger().info("[ActivityTracker] Manually cleared activity for " + player.getName());
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    // Resumes Previous Activity of player
    ////////////////////////////////////////////////////////////////////////////////
    public static void resumePreviousActivity(Player player) {
        UUID uuid = player.getUniqueId();
        if (pausedActivity.containsKey(uuid) && pausedStrength.containsKey(uuid)) {
            playerActivities.put(uuid, pausedActivity.get(uuid));
            activityStrength.put(uuid, pausedStrength.get(uuid));
            activitySetTime.put(uuid, System.currentTimeMillis());
            if (config.debugEnabled()) {
            	plugin.getLogger().info("[ActivityTracker] Resumed previous activity for " + player.getName() + ": " + pausedActivity.get(uuid));
            }
            pausedActivity.remove(uuid);
            pausedStrength.remove(uuid);
        } else {
            clearActivity(player);
        }
    }
}
