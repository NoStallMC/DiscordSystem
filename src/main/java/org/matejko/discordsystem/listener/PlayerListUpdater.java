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
import main.java.org.matejko.discordsystem.utils.ActivityManager;
import main.java.org.matejko.discordsystem.utils.ActivityTracker;
import main.java.org.matejko.discordsystem.utils.RegionFinder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.util.*;

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
    private int regionTaskId = -1;
    private int afkTaskId = -1;
    private boolean isUpdating = false;
    @SuppressWarnings("unused")
	private static DiscordPlugin plugin;
	private static Config config;

    public PlayerListUpdater(PlaytimeManager playtimeManager, Config config, DiscordPlugin plugin) {
    	PlayerListUpdater.config = config;
    	PlayerListUpdater.plugin = plugin;
        this.playtimeManager = playtimeManager;
        this.playerListBuilder = new PlayerListBuilder(playtimeManager, regionFinder, config, plugin);
        this.messageCacheManager = new MessageCacheManager(playersPerPage, playerListBuilder);
    }

	////////////////////////////////////////////////////////////////////////////////
    // Start Updating Process
    ////////////////////////////////////////////////////////////////////////////////
    public void startUpdating() {
        String channelId = GetterHandler.configuration().statusChannelId();
        TextChannel channel = GetterHandler.jda().getTextChannelById(channelId);
        if (channel == null) return;
        messageCacheManager.init();
        // Fetch messageIDs
        List<String> ids = new ArrayList<>();
        for (Message msg : messageCacheManager.getMessageCache()) {
            ids.add(msg.getId());
        }
        messageCacheManager.fetchMessages(channel, ids);

        // Schedule the initial message preparation + /reload safeness
        Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordPlugin.instance(), () -> {
            messageCacheManager.prepareMessageCache(channel);
            updateMessages();
            if (Bukkit.getOnlinePlayers().length > 0) {
                startActiveTasks();
            }
        }, 10L);
    }
    private void startActiveTasks() {
        if (repeatingTaskId == -1) {
            repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                DiscordPlugin.instance(), this::updateMessagesSafe, updateIntervalTicks, updateIntervalTicks
            );
        }
        if (regionTaskId == -1) {
            regionTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                DiscordPlugin.instance(), this::checkPlayerRegionsAndActivities, regionCheckIntervalTicks, regionCheckIntervalTicks
            );
        }
        if (afkTaskId == -1) {
            afkTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                DiscordPlugin.instance(), () -> ActivityManager.tickAFKActivity(), 0L, 100L
            );
        }
        if (config.debugEnabled()) {
            PlaytimeManager.getLogger().info("[DEBUG] All active status tasks started (Player online).");
        }
    }
    private void stopActiveTasks() {
        if (repeatingTaskId != -1) {
            Bukkit.getScheduler().cancelTask(repeatingTaskId);
            repeatingTaskId = -1;
        }
        if (regionTaskId != -1) {
            Bukkit.getScheduler().cancelTask(regionTaskId);
            regionTaskId = -1;
        }
        if (afkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(afkTaskId);
            afkTaskId = -1;
        }
        if (config.debugEnabled()) {
            PlaytimeManager.getLogger().info("[DEBUG] All active status tasks stopped (Server empty).");
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
                msg.editMessage(newContent).queue(null, throwable -> {
                    messageCacheManager.getMessageCache().clear();
                });
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
        updateMessages();
        if (Bukkit.getOnlinePlayers().length == 1) {
            startActiveTasks();
        }
        if (config.debugEnabled()) {
        	PlaytimeManager.getLogger().info("[DEBUG] " + player + " joined!");
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
    	updateMessages();
    	if (Bukkit.getOnlinePlayers().length - 1 == 0) {
            stopActiveTasks();
        }
    	if (config.debugEnabled()) {
    		PlaytimeManager.getLogger().info("[DEBUG] " + player + " left!");
    	}
    }
}
