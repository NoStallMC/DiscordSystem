package main.java.org.matejko.discordsystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;
import java.util.*;

public class ServerShellSender implements CommandSender {
    private final List<String> output = new ArrayList<>();
    private final DiscordPlugin plugin;
    public ServerShellSender(DiscordPlugin plugin) {
        this.plugin = plugin;
    }
    ///////////////////////////////////////////////////////////////////////////
    // Output handling
    ///////////////////////////////////////////////////////////////////////////

    public List<String> getOutput() {
        return output;
    }

    @Override
    public void sendMessage(String message) {
        String cleaned = ChatColor.stripColor(message);
        output.add(cleaned);
        plugin.getLogger().info("[DiscordShell] " + cleaned);
    }
    public void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // CommandSender identity
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public String getName() {
        return "DiscordShell";
    }

    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Permission handling — Full access
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean value) {
    }

    @Override
    public boolean isPermissionSet(String name) {
        return true;
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return true;
    }

    @Override
    public boolean hasPermission(String name) {
        return true;
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Permission attachments — Not used, only here to fill out method requirements
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return null;
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
    }

    @Override
    public void recalculatePermissions() {
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return Collections.emptySet();
    }
}
