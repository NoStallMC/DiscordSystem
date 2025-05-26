package main.java.org.matejko.discordsystem.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.GetterHandler;
import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.utils.ActivityTracker;
import main.java.org.matejko.discordsystem.utils.RegionFinder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PlayerListUpdater implements Listener {
	
    ////////////////////////////////////////////////////////////////////////////////
    // Variables Initialization
    ////////////////////////////////////////////////////////////////////////////////
    private final int playersPerPage = 20;
    private final long updateIntervalTicks = 1200L;
    private final long regionCheckIntervalTicks = 100L;
    private final PlaytimeManager playtimeManager;
    private final RegionFinder regionFinder = new RegionFinder();
    private final PlayerListBuilder playerListBuilder;
    private final MessageCacheManager messageCacheManager;
    private final Map<UUID, String> playerRegions = new HashMap<>();
    private final Map<UUID, ActivityTracker.PlayerActivity> playerActivities = new HashMap<>();
    private int repeatingTaskId = -1;
    private boolean isUpdating = false;
	private static Config config;

    public PlayerListUpdater(PlaytimeManager playtimeManager, Config config) {
    	PlayerListUpdater.config = config;
        this.playtimeManager = playtimeManager;
        this.playerListBuilder = new PlayerListBuilder(playtimeManager, regionFinder);
        this.messageCacheManager = new MessageCacheManager(playersPerPage, playerListBuilder);
    }

	////////////////////////////////////////////////////////////////////////////////
    // Start Updating Process
    ////////////////////////////////////////////////////////////////////////////////
    public void startUpdating() {
        String channelId = GetterHandler.configuration().statusChannelId();
        TextChannel channel = GetterHandler.jda().getTextChannelById(channelId);
        if (channel == null) return;

        // Fetch messageIDs
        List<String> ids = new ArrayList<>();
        for (Message msg : messageCacheManager.getMessageCache()) {
            ids.add(msg.getId());
        }
        messageCacheManager.fetchMessages(channel, ids);

        // Schedule the initial message preparation
        Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordPlugin.instance(), () -> {
            messageCacheManager.prepareMessageCache(channel);
            updateMessages();
        }, 10L);

        // Schedule the repeating update task
        repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            DiscordPlugin.instance(), this::updateMessagesSafe, 0L, updateIntervalTicks
        );

        // Schedule region check task
        Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordPlugin.instance(), this::checkPlayerRegionsAndActivities, 0L, regionCheckIntervalTicks);
        
        // Debug: Log playtimes every 20 seconds (400 ticks)
        if (config.debugEnabled()) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordPlugin.instance(), () -> {
            System.out.println("[DEBUG] Player playtimes:");
            for (Player player : Bukkit.getOnlinePlayers()) {
                String minutes = playtimeManager.getSessionTime(player);
                System.out.println(" - " + player.getName() + ": " + minutes + " min");
            }
        }, 0L, 400L);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Update Messages for Player List
    ////////////////////////////////////////////////////////////////////////////////
    void updateMessages() {
        List<Player> online = new ArrayList<Player>(Arrays.asList(Bukkit.getOnlinePlayers()));
        online.sort(Comparator.comparing(p -> ChatColor.stripColor(p.getDisplayName().toLowerCase())));
        int max = Bukkit.getMaxPlayers();
        List<List<Player>> pages = playerListBuilder.paginatePlayers(online, playersPerPage);
        int messageCount = messageCacheManager.getMessageCache().size();
        for (int i = 0; i < messageCount; i++) {
            Message msg = messageCacheManager.getMessageCache().get(i);
            String newContent;
            if (i == 0) {
                newContent = playerListBuilder.buildPage(!pages.isEmpty() ? pages.get(0) : Collections.emptyList(), online.size(), max);
            } else if (i < pages.size()) {
                newContent = playerListBuilder.buildOverflowPage(pages.get(i));
            } else {
                newContent = "\u200B";
            }
            if (!msg.getContentRaw().equals(newContent)) {
                try {
                    msg.editMessage(newContent).submit().get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Check Player Regions and Activities
    ////////////////////////////////////////////////////////////////////////////////
    private void checkPlayerRegionsAndActivities() {
        boolean regionChanged = false;
        boolean activityChanged = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            String currentRegion = regionFinder.getRegion(player);
            String lastRegion = playerRegions.get(uuid);
            if (!Objects.equals(currentRegion, lastRegion)) {
                playerRegions.put(uuid, currentRegion);
                regionChanged = true;
            }
            ActivityTracker.PlayerActivity currentActivity = ActivityTracker.getActivity(player);
            ActivityTracker.PlayerActivity lastActivity = playerActivities.get(uuid);
            if (!Objects.equals(currentActivity, lastActivity)) {
                playerActivities.put(uuid, currentActivity);
                activityChanged = true;
            }
        }
        if (regionChanged || activityChanged) {
            updateMessages();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Update Messages Immediately
    ////////////////////////////////////////////////////////////////////////////////
    public synchronized void updateMessagesNow() {
        if (repeatingTaskId != -1) {
            Bukkit.getScheduler().cancelTask(repeatingTaskId);
        }
        updateMessages();
        repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            DiscordPlugin.instance(), this::updateMessagesSafe, updateIntervalTicks, updateIntervalTicks
        );
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Safe Update Messages
    ////////////////////////////////////////////////////////////////////////////////
    private synchronized void updateMessagesSafe() {
        if (isUpdating) return;
        isUpdating = true;
        try {
            updateMessages();
        } finally {
            isUpdating = false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Handle Player Join
    ////////////////////////////////////////////////////////////////////////////////
    @EventHandler
    public void handleJoin(PlayerJoinEvent e) {
    	Player player = e.getPlayer();
        playtimeManager.handleJoin(e);
        playerRegions.put(e.getPlayer().getUniqueId(), regionFinder.getRegion(e.getPlayer()));
        playerActivities.put(e.getPlayer().getUniqueId(), ActivityTracker.getActivity(e.getPlayer()));
        if (config.debugEnabled()) {
    	System.out.println("[DEBUG] " + player + " joined!");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    //Handle Player Quit
    ////////////////////////////////////////////////////////////////////////////////
    @EventHandler
    public void handleQuit(PlayerQuitEvent e) {
    	Player player = e.getPlayer();
    	playtimeManager.handleQuit(e);
    	playerRegions.remove(player.getUniqueId());
    	ActivityTracker.clearActivity(player);
    	if (config.debugEnabled()) {
    	System.out.println("[DEBUG] " + player + " left!");
    	}
    }
}
