package main.java.org.matejko.discordsystem;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import main.java.org.matejko.discordsystem.configuration.CensorshipRulesManager;
import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.utilis.Utilis;
import main.java.org.matejko.utilis.Managers.SleepingManager;
import main.java.org.matejko.utilis.UtilisCore.UtilisGetters;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

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
        TextChannel channel = GetterHandler.jda().getTextChannelById(channelId);
        if (channel == null) return;
        channel.sendMessage(message).queue();
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
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                int online = Bukkit.getOnlinePlayers().length;
                int maxPlayers = Bukkit.getMaxPlayers();
                String playerName = event.getPlayer().getName();
                String displayName = sanitizeDisplayName(event.getPlayer().getDisplayName());

                String message = config.joinMessage()
                    .replace("%PlayerName%", playerName)
                    .replace("%DisplayName%", displayName)
                    .replace("%onlineCount%", String.valueOf(online))
                    .replace("%maxCount%", String.valueOf(maxPlayers));

                if (webhookEnabled()) {
                    WebhookMessageBuilder builder = new WebhookMessageBuilder()
                        .setAvatarUrl("http://minotar.net/helm/" + playerName + "/100.png")
                        .setUsername(displayName)
                        .setContent(message);
                    GetterHandler.webhookClient().send(builder.build());
                } else {
                    sendBotMessage(message);
                }

                GetterHandler.jda().getPresence().setActivity(
                    Activity.playing(config.serverName() + " with " + online + " players")
                );
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        int online = Bukkit.getOnlinePlayers().length - 1;
        int maxPlayers = Bukkit.getMaxPlayers();
        String playerName = event.getPlayer().getName();
        String displayName = sanitizeDisplayName(event.getPlayer().getDisplayName());

        String message = config.quitMessage()
            .replace("%PlayerName%", playerName)
            .replace("%DisplayName%", displayName)
            .replace("%onlineCount%", String.valueOf(online))
            .replace("%maxCount%", String.valueOf(maxPlayers));

        GetterHandler.jda().getPresence().setActivity(
            Activity.playing(config.serverName() + " with " + online + " players")
        );

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
        String content = config.chatMessage()
            .replace("%PlayerName%", playerName)
            .replace("%DisplayName%", displayName)
            .replace("%message%", censoredMessage);

        if (webhookEnabled()) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setAvatarUrl("http://minotar.net/helm/" + playerName + "/100.png")
                .setUsername(displayName)
                .setContent(censoredMessage);
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

    // Your new method:
    public void registerSleepListener() {
    	if (!config.smEnabled()) {
    		return;
    	}
        Plugin rawPlugin = Bukkit.getPluginManager().getPlugin("Utilis");
        if (!(rawPlugin instanceof Utilis)) {
            plugin.getLogger().warning("[DiscordPlugin] Utilis plugin not found or not valid! Sleep messages wont work!");
            plugin.getLogger().warning("[DiscordPlugin] Utilis plugin not found or not valid! Sleep messages wont work!");
            plugin.getLogger().warning("[DiscordPlugin] Utilis plugin not found or not valid! Sleep messages wont work!");
            return;
        }
        Utilis utilis = (Utilis) rawPlugin;
        UtilisGetters getters = utilis.getUtilisGetters();
        if (getters == null) {
            plugin.getLogger().warning("[DiscordPlugin] UtilisGetters is null! Cannot hook sleep messages.");
            return;
        }
        SleepingManager sleepingManager = getters.getSleepingManager();
        if (sleepingManager == null) {
            plugin.getLogger().warning("[DiscordPlugin] SleepingManager instance is null! Cannot hook sleep messages.");
            return;
        }
        sleepingManager.addSleepMessageListener(new SleepingManager.SleepMessageListener() {
            @Override
            public void onSleepMessage(String message) {
                // Strip Minecraft color codes
            	String cleanMsg = message.replaceAll("§[0-9a-fA-Fklmnor]", "");
            	// Sanitize for Discord
            	cleanMsg = cleanMsg
            	    .replaceAll("@everyone", "everyone")
            	    .replaceAll("@here", "here")
            	    .replaceAll("@", "(at)")
            	    .replace("*", "\\*");
            	// Wrap in thick italics
            	cleanMsg = "***" + cleanMsg + "***";
                if (webhookEnabled()) {
                    WebhookMessageBuilder builder = new WebhookMessageBuilder()
                        .setContent(cleanMsg);
                    GetterHandler.webhookClient().send(builder.build());
                } else {
                    String channelId = config.messageChannelId();
                    if (channelId != null && !channelId.isEmpty()) {
                        TextChannel channel = GetterHandler.jda().getTextChannelById(channelId);
                        if (channel != null) {
                            channel.sendMessage(cleanMsg).queue();
                        } else {
                            plugin.getLogger().warning("[DiscordPlugin] Discord channel not found: " + channelId);
                        }
                    } else {
                        plugin.getLogger().warning("[DiscordPlugin] No Discord channel ID configured.");
                    }
                }
            }
        });
        if (config.debugEnabled()) {
        	plugin.getLogger().info("[DiscordPlugin] Registered sleep message listener with Utilis SleepingManager.");
        }
    }
}
