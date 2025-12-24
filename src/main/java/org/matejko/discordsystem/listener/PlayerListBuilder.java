package main.java.org.matejko.discordsystem.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import main.java.org.matejko.discordsystem.utils.ActivityTracker;
import main.java.org.matejko.discordsystem.utils.EmojiSetGetter;
import main.java.org.matejko.discordsystem.utils.RegionFinder;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.configuration.ActivityTrackerConfig;
import main.java.org.matejko.discordsystem.configuration.Config;
import java.util.ArrayList;
import java.util.List;

public class PlayerListBuilder {
    ////////////////////////////////////////////////////////////////////////////////
    // Column Widths for message
    ////////////////////////////////////////////////////////////////////////////////
    private final int nameColumnWidthFull = 20;
    private final int timeColumnWidthFull = 20;
    private final int activityColumnWidthFull = 20;
    private final int regionColumnWidthFull = 20;
    private final int nameColumnWidthThree = 25;
    private final int timeColumnWidthThree = 25;
    private final int nameColumnWidthTwo = 30;
    private final int timeColumnWidthTwo = 30;

    ////////////////////////////////////////////////////////////////////////////////
    // Dependencies Initialization
    ////////////////////////////////////////////////////////////////////////////////
    private final PlaytimeManager playtimeManager;
    private final RegionFinder regionFinder;
    private final Config config;
    @SuppressWarnings("unused")
	private static DiscordPlugin plugin;
    private final boolean activityFeatureEnabled;
    private final boolean regionFeatureEnabled;

    ////////////////////////////////////////////////////////////////////////////////
    // Constructor to initialize PlaytimeManager, RegionFinder and Config
    ////////////////////////////////////////////////////////////////////////////////
    public PlayerListBuilder(PlaytimeManager playtimeManager, RegionFinder regionFinder, Config config, DiscordPlugin plugin) {
        PlayerListBuilder.plugin = plugin;
        this.playtimeManager = playtimeManager;
        this.regionFinder = regionFinder;
        this.config = config;
        this.activityFeatureEnabled = config.statusActivityEnabled();
        
        if (config.statusRegionEnabled() && regionFinder.isHooked()) {
            this.regionFeatureEnabled = true;
        } else {
            if (config.statusRegionEnabled()) {
                plugin.getLogger().info("[DiscordPlugin] Region feature was enabled in config but WorldGuard plugin is missing – disabling.");
                plugin.getLogger().info("[DiscordPlugin] Region feature was enabled in config but WorldGuard plugin is missing – disabling.");
                plugin.getLogger().info("[DiscordPlugin] Region feature was enabled in config but WorldGuard plugin is missing – disabling.");
            }
            this.regionFeatureEnabled = false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Build Player List Page (with header and totalOnline/max count)
    ////////////////////////////////////////////////////////////////////////////////
    public String buildPage(List<Player> players, int totalOnline, int max) {
        StringBuilder sb = new StringBuilder("```ansi\n");
        String headerWithEmojis = EmojiSetGetter.translateEmojis(config.statusHeader()
                .replace("%onlineCount%", String.valueOf(totalOnline))
                .replace("%maxCount%", String.valueOf(max)));
        sb.append("\u001B[1;36m").append(headerWithEmojis).append("\u001B[0m\n\n");
        String headerFormat = buildHeaderFormat();
        sb.append(String.format(headerFormat, buildHeaders()));
        for (Player player : players) {
            String name = ChatColor.stripColor(player.getDisplayName());
            String time = getSessionTime(player);
            String activity = activityFeatureEnabled ? EmojiSetGetter.translateEmojis(getFormattedActivity(player)) : "";
            String region = regionFeatureEnabled ? EmojiSetGetter.translateEmojis(getFormattedRegion(player)) : "";
            String playerFormat = buildPlayerFormat();
            sb.append(String.format(playerFormat, buildRow(name, time, activity, region)));
        }
        sb.append("```");
        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Build Overflow Page for Player List
    ////////////////////////////////////////////////////////////////////////////////
    public String buildOverflowPage(List<Player> players) {
        StringBuilder sb = new StringBuilder("```ansi\n");
        String headerFormat = buildHeaderFormat();
        sb.append(String.format(headerFormat, buildHeaders()));
        for (Player player : players) {
            String name = ChatColor.stripColor(player.getDisplayName());
            String time = getSessionTime(player);
            String activity = activityFeatureEnabled ? getFormattedActivity(player) : "";
            String region = regionFeatureEnabled ? getFormattedRegion(player) : "";
            String playerFormat = buildPlayerFormat();
            sb.append(String.format(playerFormat, buildRow(name, time, activity, region)));
        }
        sb.append("```");
        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Paginate Players into pages of pageSize
    ////////////////////////////////////////////////////////////////////////////////
    public List<List<Player>> paginatePlayers(List<Player> list, int pageSize) {
        List<List<Player>> pages = new ArrayList<>();
        for (int i = 0; i < list.size(); i += pageSize) {
            pages.add(list.subList(i, Math.min(i + pageSize, list.size())));
        }
        return pages;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Build header format string depending on which columns are enabled
    ////////////////////////////////////////////////////////////////////////////////
    private String buildHeaderFormat() {
        if (activityFeatureEnabled && regionFeatureEnabled) {
            return "\u001B[1;33m%-" + nameColumnWidthFull + "s \u001B[1;32m%-" + timeColumnWidthFull + "s \u001B[1;35m%-" + activityColumnWidthFull + "s \u001B[1;34m%-" + regionColumnWidthFull + "s\u001B[0m\n";
        } else if (activityFeatureEnabled || regionFeatureEnabled) {
            return "\u001B[1;33m%-" + nameColumnWidthThree + "s \u001B[1;32m%-" + timeColumnWidthThree + "s " +
                    (activityFeatureEnabled ? "\u001B[1;35m%-" + activityColumnWidthFull + "s" : "\u001B[1;34m%-" + regionColumnWidthFull + "s") +
                    "\u001B[0m\n";
        } else {
            return "\u001B[1;33m%-" + nameColumnWidthTwo + "s \u001B[1;32m%-" + timeColumnWidthTwo + "s\u001B[0m\n";
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Build player row format string depending on which columns are enabled
    ////////////////////////////////////////////////////////////////////////////////
    private String buildPlayerFormat() {
        if (activityFeatureEnabled && regionFeatureEnabled) {
            return "%-" + nameColumnWidthFull + "s %-" + timeColumnWidthFull + "s %-" + activityColumnWidthFull + "s %-" + regionColumnWidthFull + "s\n";
        } else if (activityFeatureEnabled || regionFeatureEnabled) {
            return "%-" + nameColumnWidthThree + "s %-" + timeColumnWidthThree + "s %-" +
                    (activityFeatureEnabled ? activityColumnWidthFull : regionColumnWidthFull) + "s\n";
        } else {
            return "%-" + nameColumnWidthTwo + "s %-" + timeColumnWidthTwo + "s\n";
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Build headers array depending on enabled columns
    ////////////////////////////////////////////////////////////////////////////////
    private Object[] buildHeaders() {
        if (activityFeatureEnabled && regionFeatureEnabled) {
            return new Object[]{"Name:", "Online For:", "Activity:", "Region:"};
        } else if (activityFeatureEnabled) {
            return new Object[]{"Name:", "Online For:", "Activity:"};
        } else if (regionFeatureEnabled) {
            return new Object[]{"Name:", "Online For:", "Region:"};
        } else {
            return new Object[]{"Name:", "Online For:"};
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Build row array depending on enabled columns
    ////////////////////////////////////////////////////////////////////////////////
    private Object[] buildRow(String name, String time, String activity, String region) {
        if (activityFeatureEnabled && regionFeatureEnabled) {
            return new Object[]{name, time, activity, region};
        } else if (activityFeatureEnabled) {
            return new Object[]{name, time, activity};
        } else if (regionFeatureEnabled) {
            return new Object[]{name, time, region};
        } else {
            return new Object[]{name, time};
        }
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
        ActivityTracker.PlayerActivity activity = ActivityTracker.getActivity(player);
        if (activity == null) return "";
        switch (activity) {
            case BUILDING:
                return ActivityTrackerConfig.getActivityName("building", "Building");
            case GATHERING:
                return ActivityTrackerConfig.getActivityName("gathering", "Gathering");
            case HUNTING:
                return ActivityTrackerConfig.getActivityName("hunting", "Hunting");
            case MINING:
                return ActivityTrackerConfig.getActivityName("mining", "Mining");
            case FISHING:
                return ActivityTrackerConfig.getActivityName("fishing", "Fishing");
            case FARMING:
                return ActivityTrackerConfig.getActivityName("farming", "Farming");
            case EXPLORING_NETHER:
                return ActivityTrackerConfig.getActivityName("nether_distance", "Exploring Nether");
            case AFK:
                return ActivityTrackerConfig.getActivityName("afk", "AFK");
            default:
                return activity.name();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Format Player's Current Region
    ////////////////////////////////////////////////////////////////////////////////
    private String getFormattedRegion(Player player) {
        String region = regionFinder.getRegion(player);
        if (region == null || region.equals("NOTFOUND")) return "";
        return region.replace("_", " ");
    }
}
