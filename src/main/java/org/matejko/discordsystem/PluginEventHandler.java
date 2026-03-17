package main.java.org.matejko.discordsystem;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import main.java.org.matejko.discordsystem.configuration.CensorshipRulesManager;
import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.listener.MessageReceiveListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
    public void onDiscordStart(JDA jda) {
        jda.addEventListener(new MessageReceiveListener(config, plugin));
        initPresence();
        plugin.startJdaDependentTasks();
    }
    public void initPresence() {
    	if (GetterHandler.jda() == null) return;
        int maxPlayers = Bukkit.getMaxPlayers();
        String activity = config.bact()
        .replace("%onlineCount%", String.valueOf("0"))
        .replace("%ServerName%", config.serverName())
        .replace("%maxCount%", String.valueOf(maxPlayers));
        GetterHandler.jda().getPresence().setActivity(
            Activity.playing(activity)
        );
    }
    public boolean webhookEnabled() {
        return config.webhookEnabled();
    }
    private void sendBotMessage(String message) {
    	if (GetterHandler.jda() == null) return;
        String channelId = config.messageChannelId();
        if (channelId == null || channelId.isEmpty()) return;
        TextChannel channel = GetterHandler.jda().getTextChannelById(channelId);
        if (channel == null) return;
        channel.sendMessage(message).queue();
    }
    private String sanitizeDisplayName(String displayName) {
        if (displayName == null) return "";
        return displayName.replaceAll("§[0-9a-fA-Fklmnor]", "").trim();
    }
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
            	if (GetterHandler.jda() == null) return;
                int online = Bukkit.getOnlinePlayers().length;
                int maxPlayers = Bukkit.getMaxPlayers();
                String playerName = event.getPlayer().getName();
                String displayName = sanitizeDisplayName(event.getPlayer().getDisplayName());
                String message = config.joinMessage()
                    .replace("%PlayerName%", playerName)
                    .replace("%DisplayName%", displayName)
                    .replace("%onlineCount%", String.valueOf(online))
                    .replace("%maxCount%", String.valueOf(maxPlayers));
                String activity = config.bact()
                    .replace("%onlineCount%", String.valueOf(online))
                    .replace("%ServerName%", config.serverName())
                    .replace("%maxCount%", String.valueOf(maxPlayers));
                GetterHandler.jda().getPresence().setActivity(
                    Activity.playing(activity)
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
        }, 1L);
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
    	if (GetterHandler.jda() == null) return;
        int online = Bukkit.getOnlinePlayers().length - 1;
        int maxPlayers = Bukkit.getMaxPlayers();
        String playerName = event.getPlayer().getName();
        String displayName = sanitizeDisplayName(event.getPlayer().getDisplayName());
        String message = config.quitMessage()
            .replace("%PlayerName%", playerName)
            .replace("%DisplayName%", displayName)
            .replace("%onlineCount%", String.valueOf(online))
            .replace("%maxCount%", String.valueOf(maxPlayers));
        String activity = config.bact()
        	.replace("%onlineCount%", String.valueOf(online))
        	.replace("%ServerName%", config.serverName())
        	.replace("%maxCount%", String.valueOf(maxPlayers));
        GetterHandler.jda().getPresence().setActivity(
            Activity.playing(activity)
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
    	if (GetterHandler.jda() == null) return;
        if (event.isCancelled()) return;
        String playerName = event.getPlayer().getName();
        String displayName = sanitizeDisplayName(event.getPlayer().getDisplayName());
        String originalMessage = event.getMessage();
        String sanitizedMessage = sanitizeMessage(originalMessage);
        CensorshipRulesManager rules = DiscordPlugin.instance().getCensorshipRules();
        String[] words = sanitizedMessage.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String check = word.toLowerCase();
            if (rules.getBlacklist().contains(check) && !rules.getWhitelist().contains(check)) {
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
    public void registerSleepListener() {
        try {
            Plugin raw = Bukkit.getPluginManager().getPlugin("Utilis");
            if (raw == null) {
                plugin.getLogger().info("[DiscordPlugin] Utilis not present");
                return;
            }
            Method getSleepingManager = raw.getClass().getMethod("getSleepingManager");
            Object sleepingManager = getSleepingManager.invoke(raw);
            if (sleepingManager == null) {
                plugin.getLogger().info("[DiscordPlugin] SleepingManager is null (not enabled yet)");
                return;
            }
            Class<?> listenerInterface = Class.forName(
                "main.java.org.matejko.utilis.Managers.SleepingManager$SleepMessageListener"
            );
            Object listener = Proxy.newProxyInstance(
                listenerInterface.getClassLoader(),
                new Class<?>[]{listenerInterface},
                (proxy, method, args) -> {
                    if (!method.getName().equals("onSleepMessage")) return null;
                    Player sleeper = (Player) args[0];
                    String message = (String) args[1];
                    handleSleepMessage(sleeper, message);
                    return null;
                }
            );
            Method addListener = sleepingManager.getClass()
                .getMethod("addSleepMessageListener", listenerInterface);
            addListener.invoke(sleepingManager, listener);
            plugin.getLogger().info("[DiscordPlugin] Sleep messages hook registered successfully");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[DiscordPlugin] Utilis present but sleep API missing");
        } catch (Throwable t) {
            plugin.getLogger().severe("[DiscordPlugin] Failed to hook sleep messages:");
            t.printStackTrace();
        }
    }
    private void handleSleepMessage(Player sleeper, String message) {
        if (GetterHandler.jda() == null) return;
        String cleanMsg = message.replaceAll("§[0-9a-fA-Fklmnor]", "");
        cleanMsg = cleanMsg
            .replaceAll("@everyone", "everyone")
            .replaceAll("@here", "here")
            .replaceAll("@", "(at)")
            .replace("*", "\\*");
        cleanMsg = config.smFormat().replace("%sleepmessage%", cleanMsg);
        if (webhookEnabled()) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder()
                .setAvatarUrl("http://minotar.net/helm/" + sleeper.getName() + "/100.png")
                .setUsername(sanitizeDisplayName(sleeper.getDisplayName()))
                .setContent(cleanMsg);
            GetterHandler.webhookClient().send(builder.build());
        } else {
            String channelId = config.messageChannelId();
            if (channelId != null && !channelId.isEmpty()) {
                TextChannel channel = GetterHandler.jda().getTextChannelById(channelId);
                if (channel != null) {
                    channel.sendMessage(cleanMsg).queue();
                }
            }
        }
    }
}