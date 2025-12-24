package main.java.org.matejko.discordsystem;

import main.java.org.matejko.discordsystem.configuration.Config;
import main.java.org.matejko.discordsystem.utils.ImgBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class CheckCommand extends ListenerAdapter {
    private final Config config;

    public CheckCommand(Config config) {
        this.config = config;
    }
    public static void registerCommand(JDA jda, Config config) {
        jda.upsertCommand("check", "Show player's inventory")
                .addOption(OptionType.STRING, "player", "Minecraft player name", true)
                .queue();
        jda.addEventListener(new CheckCommand(config));
    }
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("check")) return;
        if (!config.checkEnabled()) {
            event.reply("The check command is disabled.").setEphemeral(true).queue();
            return;
        }
        String userId = event.getUser().getId();
        if (userId.equals(config.botid())) return;
        String playerName = event.getOption("player").getAsString();
        event.deferReply(true).queue();
        List<?> rawUserList = config.checkAllowedUsers();
        List<String> allowedUsers = new ArrayList<>();
        for (Object obj : rawUserList) allowedUsers.add(obj.toString());
        boolean isUserAllowed = allowedUsers.contains(userId);
        boolean isRoleAllowed = false;
        if (event.getMember() != null) {
            List<String> allowedRoles = config.checkAllowedRoles();
            isRoleAllowed = event.getMember().getRoles().stream()
                    .anyMatch(role -> allowedRoles.contains(role.getName()) || allowedRoles.contains(role.getId()));
        }
        if (!isUserAllowed && !isRoleAllowed) {
            event.getHook()
                 .sendMessage(":no_entry_sign: You are not authorized to use the check command.")
                 .setEphemeral(true)
                 .queue();
            return;
        }
        try {
            if (!InventoryManager.playerExists(playerName)) {
                event.getHook()
                     .sendMessage("Player **" + playerName + "** not found.")
                     .setEphemeral(true)
                     .queue();
                return;
            }
            PlayerData playerData = InventoryManager.getPlayerData(playerName);
            byte[] png = ImgBuilder.renderInventory(playerData);
            event.getHook()
                 .sendFiles(FileUpload.fromData(new ByteArrayInputStream(png), playerName + "_inv.png"))
                 .queue();
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook()
                 .sendMessage("Failed to render inventory for " + playerName + ": " + e.getMessage())
                 .setEphemeral(true)
                 .queue();
        }
    }
}
