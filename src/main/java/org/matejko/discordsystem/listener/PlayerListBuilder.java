package main.java.org.matejko.discordsystem.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import main.java.org.matejko.discordsystem.utils.ActivityTracker;
import main.java.org.matejko.discordsystem.utils.RegionFinder;

import java.util.ArrayList;
import java.util.List;

public class PlayerListBuilder {

    ////////////////////////////////////////////////////////////////////////////////
    // Column Widths for message
    ////////////////////////////////////////////////////////////////////////////////
    private final int nameColumnWidth = 20;
    private final int timeColumnWidth = 20;
    private final int activityColumnWidth = 20;
    private final int regionColumnWidth = 20;

    ////////////////////////////////////////////////////////////////////////////////
    // Dependencies Initialization
    ////////////////////////////////////////////////////////////////////////////////
    private final PlaytimeManager playtimeManager;
    private final RegionFinder regionFinder;

    ////////////////////////////////////////////////////////////////////////////////
    // Constructor to initialize PlaytimeManager and RegionFinder
    ////////////////////////////////////////////////////////////////////////////////
    public PlayerListBuilder(PlaytimeManager playtimeManager, RegionFinder regionFinder) {
        this.playtimeManager = playtimeManager;
        this.regionFinder = regionFinder;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Build Player List Page
    ////////////////////////////////////////////////////////////////////////////////
    public String buildPage(List<Player> players, int totalOnline, int max) {
        StringBuilder sb = new StringBuilder("```ansi\n");
        sb.append(String.format("\u001B[1;36m🟢 Currently Online: %d/%d\u001B[0m\n\n", totalOnline, max));
        sb.append(String.format(
                "\u001B[1;33m%-" + nameColumnWidth + "s \u001B[1;32m%-" + timeColumnWidth + "s \u001B[1;35m%-" + activityColumnWidth + "s \u001B[1;34m%-" + regionColumnWidth + "s\u001B[0m\n",
                "Name:", "Online For:", "Activity:", "Region:"
        ));
        for (Player player : players) {
            String name = ChatColor.stripColor(player.getDisplayName());
            String time = getSessionTime(player);
            String activity = getFormattedActivity(player);  // Using formatted activity
            String region = getFormattedRegion(player);  // Using formatted region
            sb.append(String.format("%-" + nameColumnWidth + "s %-" + timeColumnWidth + "s %-" + activityColumnWidth + "s %-" + regionColumnWidth + "s\n", name, time, activity, region));
        }
        sb.append("```");
        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Build Overflow Page for Player List
    ////////////////////////////////////////////////////////////////////////////////
    public String buildOverflowPage(List<Player> players) {
        StringBuilder sb = new StringBuilder("```ansi\n");
        sb.append(String.format(
                "\u001B[1;33m%-" + nameColumnWidth + "s \u001B[1;32m%-" + timeColumnWidth + "s \u001B[1;35m%-" + activityColumnWidth + "s \u001B[1;34m%-" + regionColumnWidth + "s\u001B[0m\n",
                "Name:", "Online For:", "Activity:", "Region:"
        ));
        for (Player player : players) {
            String name = ChatColor.stripColor(player.getDisplayName());
            String time = getSessionTime(player);
            String activity = getFormattedActivity(player);  // Using formatted activity
            String region = getFormattedRegion(player);  // Using formatted region
            sb.append(String.format("%-" + nameColumnWidth + "s %-" + timeColumnWidth + "s %-" + activityColumnWidth + "s %-" + regionColumnWidth + "s\n", name, time, activity, region));
        }
        sb.append("```");
        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Paginate Players into Pages
    ////////////////////////////////////////////////////////////////////////////////
    public List<List<Player>> paginatePlayers(List<Player> list, int pageSize) {
        List<List<Player>> pages = new ArrayList<>();
        for (int i = 0; i < list.size(); i += pageSize) {
            pages.add(list.subList(i, Math.min(i + pageSize, list.size())));
        }
        return pages;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Get Player's Session Time
    ////////////////////////////////////////////////////////////////////////////////
    private String getSessionTime(Player player) {
        return playtimeManager.getSessionTime(player);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Format Player's Current Activity
    ////////////////////////////////////////////////////////////////////////////////
    private String getFormattedActivity(Player player) {
    	
        // Fetching the player's current activity and formatting it
        ActivityTracker.PlayerActivity activity = ActivityTracker.getActivity(player);
        if (activity == null) return ""; // If no activity, return " "
        // Convert the activity enum to a formatted string (e.g., "Building", "Fishing")
        switch (activity) {
            case BUILDING:
                return "Building";
            case GATHERING:
                return "Gathering";
            case HUNTING:
                return "Hunting";
            case MINING:
                return "Mining";
            case FISHING:
                return "Fishing";
            case FARMING:
                return "Farming";
            case EXPLORING_NETHER:
                return "Exploring Nether"; // Without underscores, formatted nicely
            default:
                return activity.name(); // Default to the original enum name if unhandled
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Format Player's Current Region
    ////////////////////////////////////////////////////////////////////////////////
    private String getFormattedRegion(Player player) {
    	
        // Fetching the player's current region and formatting it by replacing underscores with spaces
        String region = regionFinder.getRegion(player);
        if (region == null) return ""; // Return " " if no region is found
        return region.replace("_", " "); // Replace underscores with spaces
    }
}
