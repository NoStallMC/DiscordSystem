package main.java.org.matejko.discordsystem.listener;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import main.java.org.matejko.discordsystem.DiscordPlugin;
import main.java.org.matejko.discordsystem.GetterHandler;
import main.java.org.matejko.discordsystem.ServerShellSender;
import main.java.org.matejko.discordsystem.configuration.CensorshipRulesManager;
import main.java.org.matejko.discordsystem.configuration.Config;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MessageReceiveListener extends ListenerAdapter {
    private final Config config;

    public MessageReceiveListener(Config config) {
        this.config = config;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() && !config.serverShellAllowBots()) return;
        if (event.isWebhookMessage()) return;

        String userId = event.getAuthor().getId();
        if (userId.equals(config.botid())) return;

        String channelId = event.getChannel().getId();
        String rawContent = event.getMessage().getContentRaw();
        String sanitizedContent = sanitize(rawContent);
        String censoredContent = applyCensorship(sanitizedContent);

        if (channelId.equals(config.shellChannelId())) {
            if (!config.serverShellEnabled()) {
                event.getChannel().sendMessage(":no_entry_sign: Server-shell is disabled in config!").queue();
                return;
            }

            List<?> rawList = config.shellAllowedUsers();
            List<String> allowedUsers = new ArrayList<>();
            for (Object obj : rawList) allowedUsers.add(obj.toString());

            if (!allowedUsers.contains(userId)) {
                event.getChannel().sendMessage(":no_entry_sign: You are not authorized to use the server-shell.").queue();
                return;
            }

            if (GetterHandler.getblacklistManager().isCommandBlacklisted(sanitizedContent)) {
                event.getChannel().sendMessage(":no_entry_sign: This command is blacklisted and cannot be executed.").queue();
                return;
            }

            Bukkit.getLogger().info("[DiscordShell] executing command: " + sanitizedContent);
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordPlugin.instance(), () -> {
                ServerShellSender sender = new ServerShellSender();
                boolean result = Bukkit.dispatchCommand(sender, sanitizedContent);
                List<String> output = sender.getOutput();

                if (!result || output.isEmpty()) {
                    event.getChannel().sendMessage(":x: Unknown or failed command: `" + sanitizedContent + "`").queue();
                    return;
                }

                StringBuilder builder = new StringBuilder(":white_check_mark: **Executed:** `" + sanitizedContent + "`\n```\n");
                for (int i = 0; i < Math.min(output.size(), 10); i++) {
                    builder.append(output.get(i)).append("\n");
                }
                if (output.size() > 10) builder.append("... (truncated)\n");
                builder.append("```");
                event.getChannel().sendMessage(builder.toString()).queue();
            });
            return;
        }

        // Relay message from Discord to Minecraft
        if (channelId.equals(config.messageChannelId())) {
            String authorName = event.getMember() != null
                    ? event.getMember().getEffectiveName()
                    : event.getAuthor().getName();

            // Format for Minecraft server
            String mcMessage = config.messageFormat()
                    .replace("%user%", authorName)
                    .replace("%content%", censoredContent);
            String coloredMessage = translateColorCodes(mcMessage);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(coloredMessage);
            }

            // Webhook enabled: delete original and resend via webhook
            if (config.webhookEnabled()) {
                event.getMessage().delete().queue();

                WebhookMessageBuilder builder = new WebhookMessageBuilder()
                        .setAvatarUrl(event.getAuthor().getEffectiveAvatarUrl())
                        .setUsername(authorName)
                        .setContent(censoredContent);
                GetterHandler.webhookClient().send(builder.build());
            }
        }
    }

    public static String translateColorCodes(String input) {
        if (input == null) return null;

        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&') {
                char c = chars[i + 1];
                if (isColorCodeChar(c)) {
                    chars[i] = '§';
                    chars[i + 1] = Character.toLowerCase(c);
                }
            }
        }
        return new String(chars);
    }

    private static boolean isColorCodeChar(char c) {
        c = Character.toLowerCase(c);
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'k' && c <= 'o') || c == 'r';
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-or]", "")
                .replace("@everyone", "everyone")
                .replace("@here", "here")
                .replace("@", "(at)");
    }

    private String applyCensorship(String input) {
        if (!config.messagesCensorEnabled() || !config.censorshipEnabled() || input == null) return input;

        DiscordPlugin plugin = (DiscordPlugin) Bukkit.getPluginManager().getPlugin("DiscordSystem");
        if (plugin == null) return input;

        CensorshipRulesManager rules = plugin.getCensorshipRules();
        Set<String> blacklist = rules.getBlacklist();
        Set<String> whitelist = rules.getWhitelist();

        String[] words = input.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String rawWord = words[i].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (blacklist.contains(rawWord) && !whitelist.contains(rawWord)) {
                words[i] = repeatHashtag(rawWord.length());
            }
        }
        return String.join(" ", words);
    }

    private String repeatHashtag(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append("#");
        }
        return builder.toString();
    }
}
