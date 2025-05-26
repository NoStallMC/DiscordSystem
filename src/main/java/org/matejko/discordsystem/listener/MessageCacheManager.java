package main.java.org.matejko.discordsystem.listener;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.GetterHandler;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageCacheManager {
    private final List<Message> messageCache = new ArrayList<>();
    private final PlayerListBuilder playerListBuilder;
    private final int playersPerPage;

    /////////////////////////////////////////////////////////////////////////////
    // Schedules Cleanup Task
    /////////////////////////////////////////////////////////////////////////////
    public MessageCacheManager(int playersPerPage, PlayerListBuilder playerListBuilder) {
        this.playerListBuilder = playerListBuilder;
        this.playersPerPage = playersPerPage;
        scheduleMessageCleanup();
    }

    /////////////////////////////////////////////////////////////////////////////
    // Fetch Messages Based on Cached IDs
    /////////////////////////////////////////////////////////////////////////////
    public void fetchMessages(TextChannel channel, List<String> ids) {
        for (String id : ids) {
            try {
                Message msg = channel.retrieveMessageById(id).complete();
                if (msg != null) {
                    messageCache.add(msg);
                }
            } catch (Exception ignored) {}
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // Prepare and Cache Messages in Discord Channel
    /////////////////////////////////////////////////////////////////////////////
    public void prepareMessageCache(TextChannel channel) {
        // Check if the channel already has bot messages
        channel.getHistory().retrievePast(10).queue(messages -> {
            long botCount = messages.stream().filter(m -> m.getAuthor().isBot()).count();
            if (botCount > 0) {
                return; // Skip if bot messages are already present
            }
            messageCache.clear(); // Clear previous state
            List<String> newIds = new ArrayList<>();
            AtomicInteger created = new AtomicInteger(0);
            String[] placeholders = { "\u200B", "\u200B", "\u200B", "\u200B" };
            for (String placeholder : placeholders) {
                channel.sendMessage(placeholder).queue(msg -> {
                    messageCache.add(msg);
                    newIds.add(msg.getId());
                    if (created.incrementAndGet() == placeholders.length) {
                        /////////////////////////////////////////////////////////////////////////////
                        // Begin: Inline Update Logic for Player List
                        /////////////////////////////////////////////////////////////////////////////
                        List<Player> online = new ArrayList<>(Arrays.asList(Bukkit.getOnlinePlayers()));
                        online.sort(Comparator.comparing(p -> ChatColor.stripColor(p.getDisplayName().toLowerCase())));
                        int max = Bukkit.getMaxPlayers();
                        List<List<Player>> pages = playerListBuilder.paginatePlayers(online, playersPerPage);
                        int messageCount = messageCache.size();
                        for (int i = 0; i < messageCount; i++) {
                            Message m = messageCache.get(i);
                            String newContent;
                            if (i == 0) {
                                newContent = playerListBuilder.buildPage(!pages.isEmpty() ? pages.get(0) : Collections.emptyList(), online.size(), max);
                            } else if (i < pages.size()) {
                                newContent = playerListBuilder.buildOverflowPage(pages.get(i));
                            } else {
                                newContent = "\u200B"; // Empty message
                            }
                            if (!m.getContentRaw().equals(newContent)) {
                                try {
                                    m.editMessage(newContent).submit().get(5, TimeUnit.SECONDS);
                                } catch (Exception e) {
                                    System.err.println("[MessageCacheBuilder] Failed to update message: " + e.getMessage());
                                }
                            }
                        }
                    }
                }, error -> {});
            }
        });
    }

    /////////////////////////////////////////////////////////////////////////////
    // Get Current Message Cache
    /////////////////////////////////////////////////////////////////////////////
    public List<Message> getMessageCache() {
        return messageCache;
    }

    /////////////////////////////////////////////////////////////////////////////
    // Schedule Cleanup of Old Messages Every 10 Minutes
    /////////////////////////////////////////////////////////////////////////////
    private void scheduleMessageCleanup() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordPlugin.instance(), this::cleanUpMessages, 0L, 12000L); // 12000 ticks = 10min
    }

    /////////////////////////////////////////////////////////////////////////////
    // Cleanup Bot Messages in the Channel
    /////////////////////////////////////////////////////////////////////////////
    private void cleanUpMessages() {
        String channelId = GetterHandler.configuration().statusChannelId();
        TextChannel channel = GetterHandler.jda().getTextChannelById(channelId);
        if (channel == null) {
            return; // Skip if the channel is not found
        }
        channel.getHistory().retrievePast(10).queue(messages -> {
            List<Message> botMessages = new ArrayList<>();
            for (Message message : messages) {
                if (message.getAuthor().isBot()) {
                    botMessages.add(message);
                    if (botMessages.size() == 4) break; // Limit deletions to 4
                }
            }
            if (botMessages.isEmpty()) {
                // If no bot messages, clear cache and recreate messages
                prepareMessageCache(channel);
                return;
            }
            // Proceed to delete up to 4 messages, then retry
            AtomicInteger remaining = new AtomicInteger(botMessages.size());
            for (Message botMessage : botMessages) {
                botMessage.delete().queue(
                    success -> {
                        if (remaining.decrementAndGet() == 0) {
                            Bukkit.getScheduler().scheduleAsyncDelayedTask(DiscordPlugin.instance(), this::cleanUpMessages, 20L);
                        }
                    },
                    error -> {
                        if (remaining.decrementAndGet() == 0) {
                            Bukkit.getScheduler().scheduleAsyncDelayedTask(DiscordPlugin.instance(), this::cleanUpMessages, 20L);
                        }
                    }
                );
            }
        });
    }
}
