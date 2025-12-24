package main.java.org.matejko.discordsystem;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import main.java.org.matejko.discordsystem.configuration.Config;
import org.bukkit.Bukkit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class DeathLoggerHandler {
    @SuppressWarnings("unused")
    private static Config config;
    @SuppressWarnings("unused")
	private static DiscordPlugin plugin;

    public static void register(Config config, DiscordPlugin plugin) {
    	DeathLoggerHandler.plugin = plugin;
        DeathLoggerHandler.config = config;
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record == null) return;
                String msg = record.getMessage();
                if (msg == null) return;
                if (!msg.contains("[HeroicDeath]")) return;
                if (msg.toLowerCase().contains("enabled")) return;
                if (msg.toLowerCase().contains("disabled")) return;
                String cleaned = msg.replace("[HeroicDeath] ", "").trim();
                String[] parts = cleaned.split(" ", 2);
                if (parts.length < 2) return;
                String username = sanitizeArgs(parts[0]);
                String content = sanitizeArgs(parts[1]);
                String boldUsername = "**" + username + "**";
                try {
                    if (config.webhookEnabled()) {
                        if (config.debugEnabled()) {
                        	plugin.getLogger().info("[DeathLogger] Sending webhook for " + username);
                        }
                        WebhookMessageBuilder builder = new WebhookMessageBuilder()
                                .setAvatarUrl("http://minotar.net/helm/" + username + "/100.png")
                                .setUsername(username)
                                .setContent(boldUsername + " " + content);
                        GetterHandler.webhookClient().send(builder.build());
                    } else {
                        if (config.debugEnabled()) {
                        	plugin.getLogger().info("[DeathLogger] Webhook disabled, sending fallback message for " + username);
                        }
                        GetterHandler.jda()
                                .getTextChannelById(config.messageChannelId())
                                .sendMessage(boldUsername + " " + content)
                                .queue();
                    }
                } catch (Exception e) {
                    if (config.debugEnabled()) {
                    	plugin.getLogger().warning("[DeathLogger] Error sending death message: " + e.getMessage());
                    }
                }
            }
            @Override
            public void flush() {}
            @Override
            public void close() throws SecurityException {}
        };

        Bukkit.getLogger().addHandler(handler);
    }
    private static String sanitizeArgs(String displayName) {
        if (displayName == null) return "";
        return displayName.replaceAll("§[0-9a-fA-Fk-orK-OR]", "").trim();
    }
}
