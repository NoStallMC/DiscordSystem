package main.java.org.matejko.discordsystem;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import main.java.org.matejko.discordsystem.configuration.CensorshipRulesManager;
import main.java.org.matejko.discordsystem.configuration.Config;
import net.dv8tion.jda.api.entities.Activity;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PluginEventHandler implements Listener {
    private final Config config;
    private DiscordPlugin plugin;

    public PluginEventHandler(Config config, DiscordPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    // Call this once on plugin enable to set default presence (0 players)
    public void initPresence() {
        GetterHandler.jda().getPresence().setActivity(
            Activity.playing(config.serverName() + " with 0 players")
        );
    }

    public boolean webhookEnabled() {
        return config.webhookEnabled();
    }

    // Helper method to send message via JDA bot (not webhook)
    private void sendBotMessage(String message) {
        String channelId = config.messageChannelId();
        if (channelId == null || channelId.isEmpty()) return;
        if (GetterHandler.jda().getTextChannelById(channelId) == null) return;
        GetterHandler.jda().getTextChannelById(channelId).sendMessage(message).queue();
    }

    // Sanitize display name: strip color/formatting codes
    private String sanitizeDisplayName(String displayName) {
        if (displayName == null) return "";
        return displayName.replaceAll("§[0-9a-fA-Fklmnor]", "").trim();
    }

    // Sanitize message content for Discord
    private String sanitizeMessage(String message) {
        if (message == null) return "";
        message = message.replaceAll("§[0-9a-fA-Fklmnor]", "");
        message = message.replaceAll("@everyone", "everyone");
        message = message.replaceAll("@here", "here");
        message = message.replaceAll("@", "(at)");
        return message;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        // Schedule a delayed sync task for 1 tick later
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                int online = Bukkit.getOnlinePlayers().length;
                int maxPlayers = Bukkit.getMaxPlayers();
                String playerName = event.getPlayer().getName();
                String displayName = sanitizeDisplayName(event.getPlayer().getDisplayName());

                // Replace standard placeholders
                String message = config.joinMessage()
                    .replace("%PlayerName%", playerName)
                    .replace("%DisplayName%", displayName)
                    .replace("%onlineCount%", String.valueOf(online))
                    .replace("%maxCount%", String.valueOf(maxPlayers));

                // Send via webhook or bot
                if (webhookEnabled()) {
                    WebhookMessageBuilder builder = new WebhookMessageBuilder()
                        .setAvatarUrl("http://minotar.net/helm/" + playerName + "/100.png")
                        .setUsername(displayName)
                        .setContent(message);
                    GetterHandler.webhookClient().send(builder.build());
                } else {
                    sendBotMessage(message);
                }

                // Update presence with new player count
                GetterHandler.jda().getPresence().setActivity(
                    Activity.playing(config.serverName() + " with " + online + " players")
                );
            }
        }, 1L);  // 1 tick delay
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        int online = Bukkit.getOnlinePlayers().length - 1;
        int maxPlayers = Bukkit.getMaxPlayers();
        String playerName = event.getPlayer().getName();
        String displayName = sanitizeDisplayName(event.getPlayer().getDisplayName());

        // Replace standard placeholders
        String message = config.quitMessage()
            .replace("%PlayerName%", playerName)
            .replace("%DisplayName%", displayName)
            .replace("%onlineCount%", String.valueOf(online))
            .replace("%maxCount%", String.valueOf(maxPlayers));

        // Update presence with new player count
        GetterHandler.jda().getPresence().setActivity(
            Activity.playing(config.serverName() + " with " + online + " players")
        );

        // Send via webhook or bot
        if (webhookEnabled()) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setAvatarUrl("http://minotar.net/helm/" + playerName + "/100.png")
                .setUsername(displayName)
                .setContent(message);
            GetterHandler.webhookClient().send(builder.build());
        } else {
            sendBotMessage(message);
        }
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.isCancelled()) return;
        String playerName = event.getPlayer().getName();
        String displayName = sanitizeDisplayName(event.getPlayer().getDisplayName());
        String originalMessage = event.getMessage();
        String sanitizedMessage = sanitizeMessage(originalMessage);

        CensorshipRulesManager rules = DiscordPlugin.instance().getCensorshipRules();

        String[] words = sanitizedMessage.toLowerCase().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (rules.getBlacklist().contains(word) && !rules.getWhitelist().contains(word)) {
            	words[i] = repeatHashtag(word.length());
            }
        }
        String censoredMessage = String.join(" ", words);
        // Replace placeholders
        String content = config.chatMessage()
            .replace("%PlayerName%", playerName)
            .replace("%DisplayName%", displayName)
            .replace("%message%", censoredMessage);
        if (webhookEnabled()) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setAvatarUrl("http://minotar.net/helm/" + playerName + "/100.png")
                .setUsername(displayName)
                .setContent(censoredMessage); // Webhook gets only the message
            GetterHandler.webhookClient().send(builder.build());
        } else {
            sendBotMessage(content);
        }
    }
    private String repeatHashtag(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append("#");
        }
        return builder.toString();
    }
}
