package main.java.org.matejko.discordsystem;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import main.java.org.matejko.discordsystem.configuration.Config;
import org.bukkit.Bukkit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class DeathLoggerHandler {
    @SuppressWarnings("unused")
	private static Config config;

    public static void register(Config config) {
        DeathLoggerHandler.config = config;
        if (config.debugEnabled()) {
            System.out.println("[DeathLogger] Registering custom logger handler...");
        }

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
                String username = parts[0];
                String content = parts[1];
                String boldUsername = "**" + username + "**";
                try {
                    if (config.webhookEnabled()) {
                        if (config.debugEnabled()) {
                            System.out.println("[DeathLogger] Sending webhook for " + username);
                        }
                        WebhookMessageBuilder builder = new WebhookMessageBuilder()
                                .setAvatarUrl("http://minotar.net/helm/" + username + "/100.png")
                                .setUsername(username)
                                .setContent(boldUsername + " " + content);
                        GetterHandler.webhookClient().send(builder.build());
                    } else {
                        if (config.debugEnabled()) {
                            System.out.println("[DeathLogger] Webhook disabled, sending fallback message for " + username);
                        }
                        GetterHandler.jda()
                                .getTextChannelById(config.messageChannelId())
                                .sendMessage(boldUsername + " " + content)
                                .queue();
                    }
                } catch (Exception e) {
                    if (config.debugEnabled()) {
                        System.out.println("[DeathLogger] Error sending death message: " + e.getMessage());
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
}
