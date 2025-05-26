package main.java.org.matejko.discordsystem.listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import main.java.org.matejko.discordsystem.GetterHandler;

import java.util.Map;

public final class SignCommand implements CommandExecutor {

    /////////////////////////////////////////////////////////////////////////////
    // Handle Command Execution
    /////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("signremove")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            SignLoggerListener.loadSignMap();
            sender.sendMessage(ChatColor.GREEN + "Sign map reloaded.");
            return true;
        }

        /////////////////////////////////////////////////////////////////////////////
        // Handle Sign ID Removal
        /////////////////////////////////////////////////////////////////////////////
        String signID = args[0];
        Map<String, Location> signMap = SignLoggerListener.getSignMap();
        if (!signMap.containsKey(signID)) {
            sender.sendMessage(ChatColor.RED + "Sign ID not found.");
            return true;
        }
        Location loc = signMap.get(signID);
        String messageId = SignLoggerListener.getMessageIdFromConfig(signID);
        Block block = loc.getBlock();

        /////////////////////////////////////////////////////////////////////////////
        // Remove Sign from World and Config
        /////////////////////////////////////////////////////////////////////////////
        if (!block.getType().toString().contains("SIGN")) {
            sender.sendMessage(ChatColor.YELLOW + "No sign found at location. Removing record anyway.");
        } else {
            block.setType(Material.AIR);
            sender.sendMessage(ChatColor.GREEN + "Removed sign at X: " + loc.getBlockX() +
                    ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ());
        }
        SignLoggerListener.removeSignFromConfig(signID);
        signMap.remove(signID);

        /////////////////////////////////////////////////////////////////////////////
        // Update Discord Message
        /////////////////////////////////////////////////////////////////////////////
        if (messageId != null &&
            GetterHandler.jda() != null &&
            GetterHandler.jda().getTextChannelById(GetterHandler.configuration().signChannelId()) != null) {
            GetterHandler.jda().getTextChannelById(GetterHandler.configuration().signChannelId())
                .retrieveMessageById(messageId).queue(message -> {
                    String remover = sender.getName();
                    String updated = "**⚠️ REMOVED ⚠️**\n" +
                                     "This sign was removed from the server by `" + remover + "`.\n\n" +
                                     message.getContentRaw().replace("**Original Details:**\n", "");
                    message.editMessage(updated).queue();
                });
        }
        return true;
    }

    /////////////////////////////////////////////////////////////////////////////
    // Display Command Help
    /////////////////////////////////////////////////////////////////////////////
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "Signremove usage:");
        sender.sendMessage(ChatColor.YELLOW + "/signremove <id>" + ChatColor.WHITE + " - Remove a sign by ID.");
        sender.sendMessage(ChatColor.YELLOW + "/signremove reload" + ChatColor.WHITE + " - Reload sign data from file.");
    }
}
