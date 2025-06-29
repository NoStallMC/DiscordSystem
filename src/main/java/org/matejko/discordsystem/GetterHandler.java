package main.java.org.matejko.discordsystem;

import club.minnced.discord.webhook.WebhookClient;
import main.java.org.matejko.discordsystem.configuration.ActivityTrackerConfig;
import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.listener.BlacklistManager;
import main.java.org.matejko.discordsystem.utils.EmojiSetGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.bukkit.plugin.java.JavaPlugin;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

public final class GetterHandler {
    private static JDA jda;
    private static JavaPlugin plugin;
    private static Config config;
    private static WebhookClient webhookClient;
    private static BlacklistManager blacklistManager;
    
    public static void initialize(JavaPlugin pluginInstance, DiscordPlugin p) {
        plugin = pluginInstance;
        config = new Config(p);
        config.loadConfig();
        if ("YourBotToken".equals(config.token())) {
            p.getLogger().severe("Discord token is not set! Disabling plugin.");
            p.getLogger().severe("Discord token is not set! Disabling plugin.");
            p.getLogger().severe("Discord token is not set! Disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }
        try {
            ActivityTrackerConfig.setPlugin((DiscordPlugin) plugin);
            ActivityTrackerConfig.load();
        } catch (IOException e) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }
        blacklistManager = new BlacklistManager(plugin, config);
        try {
            JDALogger.setFallbackLoggerEnabled(false);
            jda = JDABuilder.createDefault(config.token(), EnumSet.of(
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS
            ))
            .disableCache(EnumSet.of(
                    CacheFlag.VOICE_STATE,
                    CacheFlag.EMOJI,
                    CacheFlag.STICKER,
                    CacheFlag.SCHEDULED_EVENTS
            ))
            .build();
            jda.awaitReady();

        } catch (Exception e) {
            throw new RuntimeException("Error initializing JDA: ", e);
        }
        String[] parts = config.webhookUrl().replace("https://discord.com/api/webhooks/", "").split("/");
        webhookClient = WebhookClient.withId(Long.parseLong(parts[0]), parts[1]);
        MessageData startMsg = getServerStartMessage();
        if (config.webhookEnabled()) {
            sendEmbedFromMessageData(startMsg);
        } else {
            String messageraw = config.getNormalServerStartMessages()
                    .replace("%ServerName%", config.serverName());
            String message = EmojiSetGetter.translateEmojis(messageraw);
            sendPlainMessage(message);
        }
    }

    public static void shutdown() {
        if (jda == null) return;
        MessageData shutdownMsg = getServerShutdownMessage();
        if (config.webhookEnabled()) {
            sendEmbedFromMessageData(shutdownMsg);
        } else {
            String messageraw = config.getNormalServerShutdownMessages()
                    .replace("%ServerName%", config.serverName());
            String message = EmojiSetGetter.translateEmojis(messageraw);
            sendPlainMessage(message);
        }
        jda.shutdown();
    }
    
    private static void sendPlainMessage(String content) {
        if (jda == null) return;
        TextChannel textChannel = jda.getTextChannelById(config.messageChannelId());
        if (textChannel != null) {
            textChannel.sendMessage(content).queue();
        }
    }

    private static void sendEmbedFromMessageData(MessageData msg) {
        if (msg == null || jda == null) return;
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(msg.author)
                .setTitle(msg.title)
                .setDescription(msg.description)
                .setColor(msg.color)
                .setTimestamp(Instant.now());
        send(builder);
    }

    private static MessageData getServerStartMessage() {
        return readMessageDataFromConfig("messages.rich-server-start-message",
                new MessageData("Server", "Information", "Server started.", Color.GREEN));
    }

    private static MessageData getServerShutdownMessage() {
        return readMessageDataFromConfig("messages.rich-server-shutdown-message",
                new MessageData("Server", "Information", "Server stopped.", Color.RED));
    }

    private static MessageData readMessageDataFromConfig(String path, MessageData def) {
        String authorraw = config.getString(path + ".author");
        String author = EmojiSetGetter.translateEmojis(authorraw);
        if (author == null) author = def.author;
        String titleraw = config.getString(path + ".title");
        String title = EmojiSetGetter.translateEmojis(titleraw);
        if (title == null) title = def.title;
        List<String> descLines = null;
        try {
            descLines = config.getStringList(path + ".description");
        } catch (Exception ignored) {}
        String description;
        if (descLines != null && !descLines.isEmpty()) {
            description = String.join("\n", descLines);
        } else {
            String singleDescraw = config.getString(path + ".description");
            String singleDesc = EmojiSetGetter.translateEmojis(singleDescraw);
            if (singleDesc != null) {
                description = singleDesc;
            } else {
                description = def.description;
            }
        }
        String colorName = config.getString(path + ".color");
        Color color = parseColor(colorName, def.color);
        return new MessageData(author, title, description, color);
    }

    private static Color parseColor(String name, Color def) {
        if (name == null) return def;
        try {
            Field field = Color.class.getField(name.toUpperCase());
            return (Color) field.get(null);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static void send(EmbedBuilder builder) {
        if (jda == null) return;
        TextChannel textChannel = jda.getTextChannelById(config.messageChannelId());
        if (textChannel != null) {
            textChannel.sendMessageEmbeds(builder.build()).queue();
        }
    }

    public static Config configuration() {
        return config;
    }

    public static WebhookClient webhookClient() {
        return webhookClient;
    }

    public static BlacklistManager getblacklistManager() {
        return blacklistManager;
    }

    public static JDA jda() {
        return jda;
    }

    private static class MessageData {
        public final String author;
        public final String title;
        public final String description;
        public final Color color;

        public MessageData(String author, String title, String description, Color color) {
            this.author = author;
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }
}
