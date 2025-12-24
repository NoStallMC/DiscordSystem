package main.java.org.matejko.discordsystem.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.configuration.Config;
import org.bukkit.World.Environment;
import static main.java.org.matejko.discordsystem.configuration.ActivityTrackerConfig.*;
import static main.java.org.matejko.discordsystem.utils.ActivityTracker.*;
import java.util.*;

public class ActivityManager {
    ////////////////////////////////////////////////////////////
    // Buffers to store recent actions for each player
    ////////////////////////////////////////////////////////////
    private static final Map<UUID, PlayerActionBuffer> buffers = new HashMap<>();
	private static Config config;
	private static DiscordPlugin plugin;
	private static Essentials essentials;

	public ActivityManager(Config config, Essentials essentials, DiscordPlugin plugin) {
		ActivityManager.plugin = plugin;
	    ActivityManager.config = config;
	    ActivityManager.essentials = essentials;
	}
    public static PlayerActionBuffer getBuffer(Player player) {
        UUID uuid = player.getUniqueId();
        return buffers.computeIfAbsent(uuid, k -> new PlayerActionBuffer());
    }
    private static boolean isActivityEnabled(String activityName) {
        return getThreshold(activityName, 1) > 0;
    }

    ////////////////////////////////////////////////////////////
    // Handles block placements for BUILDING activity
    ////////////////////////////////////////////////////////////
    public static void handlePlace(Player player, Material type) {
    	if (!isActivityEnabled("building")) {
    	    PlayerActivity current = getActivity(player);
    	    if (current == PlayerActivity.BUILDING) {
    	        ActivityTracker.clearActivity(player);
    	    }
    	    return;
    	}
        PlayerActionBuffer buf = getBuffer(player);
        buf.cleanup();
        buf.addOrUpdate(buf.placedBlocks, type);
        long count = buf.placedBlocks.stream().mapToLong(b -> b.count).sum();
        int threshold = getThreshold("building", 5);
        int percent = (int) ((Math.min(count, threshold) / (double) threshold) * 100);
        if (config.debugEnabled()) {
        	plugin.getLogger().info(player.getName() + " placing any block: " + percent + "% BUILDING");
        }
        if (count >= threshold) {
            setActivity(player, PlayerActivity.BUILDING, percent);
        }
    }

    ////////////////////////////////////////////////////////////
    // Handles block breaks for GATHERING and MINING activities
    ////////////////////////////////////////////////////////////
    public static void handleBreak(Player player, Material type) {
        boolean gatheringEnabled = isActivityEnabled("gathering");
        boolean miningEnabled = isActivityEnabled("mining");
        if (!gatheringEnabled && !miningEnabled) {
            PlayerActivity current = getActivity(player);
            if (current == PlayerActivity.GATHERING || current == PlayerActivity.MINING) {
                ActivityTracker.clearActivity(player);
            }
            return;
        }
        if (!gatheringEnabled) {
            PlayerActivity current = getActivity(player);
            if (current == PlayerActivity.GATHERING) {
                ActivityTracker.clearActivity(player);
            }
        }
        if (!miningEnabled) {
            PlayerActivity current = getActivity(player);
            if (current == PlayerActivity.MINING) {
                ActivityTracker.clearActivity(player);
            }
        }
        if (!isGatheringBlock(type) && !isMiningBlock(type)) return;
        PlayerActionBuffer buf = getBuffer(player);
        buf.cleanup();
        buf.addOrUpdate(buf.brokenBlocks, type);
        if (isMiningBlock(type) && miningEnabled) {
            buf.addOrUpdate(buf.miningBlocks, type);
        } 
        if (isGatheringBlock(type) && gatheringEnabled) {
            buf.addOrUpdate(buf.gatheringBlocks, type);
        }
        buf.cleanup();
        int gatherThreshold = getThreshold("gathering", 10);
        int miningThreshold = getThreshold("mining", 5);
        long gatherCount = buf.gatheringBlocks.stream().mapToLong(b -> b.count).sum();
        long miningCount = buf.miningBlocks.stream().mapToLong(b -> b.count).sum();
        int gatherPercent = (int) ((Math.min(gatherCount, gatherThreshold) / (double) gatherThreshold) * 100);
        int miningPercent = (int) ((Math.min(miningCount, miningThreshold) / (double) miningThreshold) * 100);
        if (config.debugEnabled()) {
            plugin.getLogger().info(player.getName() + " breaking " + type + ": " + gatherPercent + "% GATHERING, " + miningPercent + "% MINING");
        }
        PlayerActivity current = getActivity(player);
        if (gatheringEnabled && (gatherCount >= gatherThreshold || (current == PlayerActivity.GATHERING && gatherCount > 0))) {
            setActivity(player, PlayerActivity.GATHERING, gatherPercent);
        }
        if (miningEnabled && (miningCount >= miningThreshold || (current == PlayerActivity.MINING && miningCount > 0))) {
            setActivity(player, PlayerActivity.MINING, miningPercent);
        }
    }

    ////////////////////////////////////////////////////////////
    // Determines if a block is related to gathering activity
    ////////////////////////////////////////////////////////////
    private static boolean isGatheringBlock(Material material) {
        return !(isMiningBlock(material) || isNonGatheringBlock(material));
    }

    private static boolean isMiningBlock(Material material) {
        return material == Material.STONE || material == Material.COAL_ORE ||
               material == Material.IRON_ORE || material == Material.GOLD_ORE ||
               material == Material.DIAMOND_ORE || material == Material.LAPIS_ORE ||
               material == Material.REDSTONE_ORE;
    }

    private static boolean isNonGatheringBlock(Material material) {
        return material == Material.FIRE || material == Material.WATER ||
               material == Material.WHEAT || material == Material.PUMPKIN ||
               material == Material.CROPS;
    }

    ////////////////////////////////////////////////////////////
    // Handles item pickups for HUNTING activity
    ////////////////////////////////////////////////////////////
    public static void handlePickup(Player player, Material type) {
    	if (!isActivityEnabled("hunting")) {
    	    PlayerActivity current = getActivity(player);
    	    if (current == PlayerActivity.HUNTING) {
    	        ActivityTracker.clearActivity(player);
    	    }
    	    return;
    	}
        PlayerActionBuffer buf = getBuffer(player);
        buf.pickedUpItems.add(type);
        buf.cleanup();
        int threshold = getThreshold("hunting", 5);
        int percent = Math.min(buf.hunting * (100 / threshold), 100);
        if (isMobItem(type)) {
            if (config.debugEnabled()) {
            	plugin.getLogger().info(player.getName() + " picked up " + type + ": +" + percent + "% HUNTING");
            }
            setActivity(player, PlayerActivity.HUNTING, percent);
        }
    }

    private static boolean isMobItem(Material mat) {
        return mat == Material.FEATHER || mat == Material.PORK || mat == Material.LEATHER;
    }

    ////////////////////////////////////////////////////////////
    // Handles fishing activity
    ////////////////////////////////////////////////////////////
    public static void handleFish(Player player) {
    	if (!isActivityEnabled("fishing")) {
    		PlayerActivity current = getActivity(player);
    		if (current == PlayerActivity.FISHING) {
    			ActivityTracker.clearActivity(player);
    		}
    		return;
		}
        PlayerActionBuffer buf = getBuffer(player);
        buf.fishCaught++;
        buf.cleanup();
        int threshold = getThreshold("fishing", 2);
        int percent = Math.min(buf.fishCaught * (100 / threshold), 100);
        if (config.debugEnabled()) {
        	plugin.getLogger().info(player.getName() + ": " + percent + "% FISHING");
        }
        if (buf.fishCaught >= threshold) {
            setActivity(player, PlayerActivity.FISHING, percent);
        }
    }

    ////////////////////////////////////////////////////////////
    // Handles farmland interaction for FARMING activity
    ////////////////////////////////////////////////////////////
    public static void handleFarmland(Player player, Material type) {
    	if (!isActivityEnabled("farming")) {
    	    PlayerActivity current = getActivity(player);
    	    if (current == PlayerActivity.FARMING) {
    	        ActivityTracker.clearActivity(player);
    	    }
    	    return;
    	}
        PlayerActionBuffer buf = getBuffer(player);
        buf.addOrUpdate(buf.farmlandBlocks, type);
        buf.cleanup();
        long count = buf.farmlandBlocks.stream().mapToLong(b -> b.count).sum();
        int threshold = getThreshold("farming", 3);
        int percent = (int) ((Math.min(count, threshold) / (double) threshold) * 100);
        if (config.debugEnabled()) {
        	plugin.getLogger().info(player.getName() + " farmland/crop: " + percent + "% FARMING");
        }
        if (count >= threshold) {
            setActivity(player, PlayerActivity.FARMING, percent);
        }
    }

    ////////////////////////////////////////////////////////////
    // Handles Nether movement for EXPLORING_NETHER activity
    ////////////////////////////////////////////////////////////
    public static void handleMove(Player player, Material blockBelow) {
    	if (!isActivityEnabled("nether_distance")) {
    		PlayerActivity current = getActivity(player);
    		if (current == PlayerActivity.EXPLORING_NETHER) {
    			ActivityTracker.clearActivity(player);
    		}
    		return;
		}
        if (player.getWorld().getEnvironment() == Environment.NETHER &&
            (blockBelow == Material.NETHERRACK || blockBelow == Material.SOUL_SAND)) {
            PlayerActionBuffer buf = getBuffer(player);
            buf.netherDistance++;
            int threshold = getThreshold("nether_distance", 30);
            int percent = Math.min((int) ((buf.netherDistance / (double) threshold) * 100), 100);
            if (config.debugEnabled()) {
            	plugin.getLogger().info(player.getName() + ": " + percent + "% EXPLORING_NETHER");
            }
            if (buf.netherDistance >= threshold) {
                setActivity(player, PlayerActivity.EXPLORING_NETHER, percent);
            }
        }
    }
    public static boolean isAFK(Player player) {
        if (essentials == null) return false;
        User user = essentials.getUser(player.getName());
        return user != null && user.isAfk();
    }
    public static void tickAFKActivity() {
        if (config.debugEnabled()) {
            	plugin.getLogger().info("Ticking for AFK players.");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean afk = isAFK(player);
            if (config.debugEnabled()) {
            		plugin.getLogger().info("[AFK Check] " + player.getName() + " AFK: " + afk);
            }
            if (!isActivityEnabled("afk")) {
                PlayerActivity current = getActivity(player);
                if (current == PlayerActivity.AFK) {
                    ActivityTracker.clearActivity(player);
                }
                return;
            }
            if (afk) {
                PlayerActionBuffer buf = getBuffer(player);
                buf.afkSeconds++;
                int threshold = getThreshold("afk", 60); // seconds
                int percent = Math.min((int)((buf.afkSeconds / (double)threshold) * 100), 100);
                if (config.debugEnabled()) {
                	plugin.getLogger().info("[AFK] " + player.getName() + ": " + buf.afkSeconds + "s -> " + percent + "%");
                }
                if (getActivity(player) == PlayerActivity.AFK) {
                    activityStrength.put(player.getUniqueId(), 100.0);
                }
                if (buf.afkSeconds >= threshold && getActivity(player) != PlayerActivity.AFK) {
                    ActivityTracker.setActivity(player, PlayerActivity.AFK, percent);
                }
            } else {
                PlayerActionBuffer buf = getBuffer(player);
                if (getActivity(player) == PlayerActivity.AFK && buf.afkSeconds >= getThreshold("afk", 60)) {
                    	plugin.getLogger().info("[AFK Recovery] " + player.getName() + " is no longer AFK. Resuming activity.");
                    ActivityTracker.resumePreviousActivity(player);
                }
                buf.afkSeconds = 0; // Reset AFK counter
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // Local override of setActivity() from ActivityTracker
    // Used to directly access the underlying maps without calling external API
    ////////////////////////////////////////////////////////////
    private static void setActivity(Player player, PlayerActivity activity, int strengthDelta) {
        UUID uuid = player.getUniqueId();
        PlayerActivity current = playerActivities.get(uuid);
        if (current != null && current == activity) {
            double currentStrength = activityStrength.getOrDefault(uuid, 0.0);
            double newStrength = Math.min(100.0, currentStrength + strengthDelta);
            activityStrength.put(uuid, newStrength);
        } else {
            playerActivities.put(uuid, activity);
            activityStrength.put(uuid, Math.min(100.0, strengthDelta));
        }
        activitySetTime.put(uuid, System.currentTimeMillis());
        if (config.debugEnabled()) {
            	plugin.getLogger().info("[ActivityTracker] Set activity for " + player.getName() + ": " + activity + " at " + System.currentTimeMillis() + " with strength " + String.format("%.2f", activityStrength.get(uuid)));
        }
    }
}
