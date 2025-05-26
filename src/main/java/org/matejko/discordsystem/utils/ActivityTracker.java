package main.java.org.matejko.discordsystem.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import main.java.org.matejko.discordsystem.configuration.ActivityTrackerConfig;
import main.java.org.matejko.discordsystem.configuration.Config;

import org.bukkit.Server;

import java.util.*;

public class ActivityTracker {

    ////////////////////////////////////////////////////////////////////////////////
    // Enum representing different types of player activities
    ////////////////////////////////////////////////////////////////////////////////
    public enum PlayerActivity {
        BUILDING, GATHERING, HUNTING, MINING, FISHING, FARMING, EXPLORING_NETHER
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Internal tracking data structures
    ////////////////////////////////////////////////////////////////////////////////
    static final Map<UUID, Integer> activityStrength = new HashMap<>();
    static final Map<UUID, PlayerActivity> playerActivities = new HashMap<>();
    static final Map<UUID, Long> activitySetTime = new HashMap<>();
    private static Plugin plugin;
	private static Config config;

    ////////////////////////////////////////////////////////////////////////////////
    // Initializes the tracker and starts the decay task
    ////////////////////////////////////////////////////////////////////////////////
    public static void init(Plugin pl, Config config) {
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
                Iterator<Map.Entry<UUID, Integer>> iter = activityStrength.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<UUID, Integer> entry = iter.next();
                    UUID uuid = entry.getKey();
                    int currentStrength = entry.getValue();
                    PlayerActivity activity = playerActivities.get(uuid);
                    long activityStartTime = activitySetTime.getOrDefault(uuid, 0L);
                    if (activityStartTime == 0L) continue;
                    double newStrength = currentStrength - decayPerInterval;
                    if (newStrength <= 0.0) {
                        iter.remove();
                        playerActivities.remove(uuid);
                        activitySetTime.remove(uuid);
                        if (config.debugEnabled()) {
                        System.out.println("[ActivityTracker] Cleared expired activity for: " + uuid);
                        }
                    } else {
                        activityStrength.put(uuid, (int) Math.round(newStrength));
                        if (activity != null && config.debugEnabled()) {
                            System.out.println("[DEBUG] " + uuid + " current activity: " + activity + " at strength " + (int) newStrength + "%");
                            System.out.println("[ActivityTracker] Decay time: " + decayTimeSeconds + "s -> " + String.format("%.2f", decayPerSecond) + "% per second");
                            System.out.println("[ActivityTracker] Decay interval: " + decaySpeedTicks + " ticks (" + String.format("%.2f", decayIntervalSeconds) + "s) -> " + String.format("%.2f", decayPerInterval) + "% per check");
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
        if (current != null && current == activity) {
            int currentStrength = activityStrength.getOrDefault(uuid, 0);
            int newStrength = Math.min(100, currentStrength + strengthDelta);
            activityStrength.put(uuid, newStrength);
            if (config.debugEnabled()) {
            System.out.println("[DEBUG] Updated activity strength for " + player.getName() + ": " + currentStrength + "% -> " + newStrength + "%");
            }
        } else {
            playerActivities.put(uuid, activity);
            activityStrength.put(uuid, Math.min(100, strengthDelta));
            if (config.debugEnabled()) {
            System.out.println("[ActivityTracker] Set activity for " + player.getName() + ": " + activity + " at strength " + strengthDelta + "%");
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
        if (config.debugEnabled()) {
        System.out.println("[ActivityTracker] Manually cleared activity for " + player.getName());
        }
    }
}
