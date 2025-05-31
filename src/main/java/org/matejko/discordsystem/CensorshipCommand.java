package main.java.org.matejko.discordsystem;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import main.java.org.matejko.discordsystem.configuration.CensorshipRulesManager;
import java.util.Set;

public class CensorshipCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("censorship.manage")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        DiscordPlugin plugin = (DiscordPlugin) Bukkit.getPluginManager().getPlugin("DiscordSystem");
        CensorshipRulesManager rules = plugin.getCensorshipRules();
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        String word = (args.length >= 2) ? args[1].toLowerCase() : null;
        switch (sub) {
            case "blacklist":
                if (word == null) {
                    sender.sendMessage("§cUsage: /censorship blacklist word");
                    return true;
                }
                if (rules.getBlacklist().contains(word)) {
                    sender.sendMessage("§eAlready in blacklist: §f" + word);
                } else {
                    rules.getBlacklist().add(word);
                    rules.saveRules();
                    sender.sendMessage("§aAdded to blacklist: §f" + word);
                }
                break;
            case "whitelist":
                if (word == null) {
                    sender.sendMessage("§cUsage: /censorship whitelist word");
                    return true;
                }
                if (rules.getWhitelist().contains(word)) {
                    sender.sendMessage("§eAlready in whitelist: §f" + word);
                } else {
                    rules.getWhitelist().add(word);
                    rules.saveRules();
                    sender.sendMessage("§aAdded to whitelist: §f" + word);
                }
                break;
            case "unblacklist":
                if (word == null) {
                    sender.sendMessage("§cUsage: /censorship unblacklist word");
                    return true;
                }
                if (rules.getBlacklist().remove(word)) {
                    rules.saveRules();
                    sender.sendMessage("§aRemoved from blacklist: §f" + word);
                } else {
                    sender.sendMessage("§cWord not found in blacklist.");
                }
                break;
            case "unwhitelist":
                if (word == null) {
                    sender.sendMessage("§cUsage: /censorship unwhitelist word");
                    return true;
                }
                if (rules.getWhitelist().remove(word)) {
                    rules.saveRules();
                    sender.sendMessage("§aRemoved from whitelist: §f" + word);
                } else {
                    sender.sendMessage("§cWord not found in whitelist.");
                }
                break;
            case "reload":
                rules.reloadRules();
                sender.sendMessage("§aCensorship rules reloaded.");
                break;
            case "list":
                sendList(sender, rules);
                break;
            default:
                sender.sendMessage("§cUnknown parameter. Use /censorship for help.");
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§b--- Censorship Command Help ---");
        sender.sendMessage("§e/censorship blacklist word §7- Add word to blacklist");
        sender.sendMessage("§e/censorship whitelist word §7- Add word to whitelist");
        sender.sendMessage("§e/censorship unblacklist word §7- Remove word from blacklist");
        sender.sendMessage("§e/censorship unwhitelist word §7- Remove word from whitelist");
        sender.sendMessage("§e/censorship reload §7- Reload rules from file");
        sender.sendMessage("§e/censorship list §7- Show current rules lists");
    }

    private void sendList(CommandSender sender, CensorshipRulesManager rules) {
        Set<String> blacklist = rules.getBlacklist();
        Set<String> whitelist = rules.getWhitelist();
        sender.sendMessage("§6[Censorship] §fCurrent blacklist:");
        if (blacklist.isEmpty()) {
            sender.sendMessage("§7  (empty)");
        } else {
            for (String word : blacklist) {
                sender.sendMessage("§7  - " + word);
            }
        }
        sender.sendMessage("§6[Censorship] §fCurrent whitelist:");
        if (whitelist.isEmpty()) {
            sender.sendMessage("§7  (empty)");
        } else {
            for (String word : whitelist) {
                sender.sendMessage("§7  - " + word);
            }
        }
    }
}
