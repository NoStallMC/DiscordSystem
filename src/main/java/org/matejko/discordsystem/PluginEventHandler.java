package main.java.org.matejko.discordsystem;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
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
                String rawName = event.getPlayer().getDisplayName();
                String name = config.useDisplayName() ? sanitizeDisplayName(rawName) : event.getPlayer().getName();
                String message = config.joinMessage()
                    .replace("%username%", name)
                    .replace("%onlineCount%", String.valueOf(online))
                    .replace("%maxCount%", String.valueOf(maxPlayers));
                if (webhookEnabled()) {
                    WebhookMessageBuilder builder = new WebhookMessageBuilder()
                        .setAvatarUrl("http://minotar.net/helm/" + event.getPlayer().getName() + "/100.png")
                        .setUsername(name)
                        .setContent(message);
                    GetterHandler.webhookClient().send(builder.build());
                } else {
                    sendBotMessage(message);
                }
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
        String rawName = event.getPlayer().getDisplayName();
        String name = config.useDisplayName() ? sanitizeDisplayName(rawName) : event.getPlayer().getName();
        String message = config.quitMessage()
            .replace("%username%", name)
            .replace("%onlineCount%", String.valueOf(online))
            .replace("%maxCount%", String.valueOf(maxPlayers));

        GetterHandler.jda().getPresence().setActivity(
            Activity.playing(config.serverName() + " with " + online + " players")
        );
        if (webhookEnabled()) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setAvatarUrl("http://minotar.net/helm/" + event.getPlayer().getName() + "/100.png")
                .setUsername(name)
                .setContent(message);
            GetterHandler.webhookClient().send(builder.build());
        } else {
            sendBotMessage(message);
        }
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.isCancelled()) return;

        String rawName = event.getPlayer().getDisplayName();
        String name = config.useDisplayName() ? sanitizeDisplayName(rawName) : event.getPlayer().getName();
        String content = config.chatMessage()
            .replace("%messageAuthor%", name)
            .replace("%message%", sanitizeMessage(event.getMessage()));
        if (webhookEnabled()) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setAvatarUrl("http://minotar.net/helm/" + event.getPlayer().getName() + "/100.png")
                .setUsername(name)
                .setContent(sanitizeMessage(event.getMessage()));
            GetterHandler.webhookClient().send(builder.build());
        } else {
            sendBotMessage(content);
        }
    }
}
