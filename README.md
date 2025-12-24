# DiscordSystem

**Author**: ~jkoo

**DiscordSystem** is a powerful plugin for Minecraft Beta 1.7.3 servers.  From chat bridging and activity tracking to censorship and remote command execution via Discord, it offers a lot cool of tools for easier moderation of your server!

---

### Features

- **Two-Way Chat Sync**: Bridge messages between Minecraft and Discord.
- **Webhooks**: Choose between webhook embeds or normal discord messages.
- **Server Shell**: Allow trusted Discord users or roles to run Minecraft server commands remotely. (**BE CAREFUL WHO YOU GIVE ACCESS TO!**)
- **Async Handling**: Switched to fully Asynchronous processing; Discord lag will no longer hang your server.
- **Player Check System**: New `/check` command to view online/offline player inventory, position, skin, and more via Discord.
- **Players Activity**: Tracks building, mining, farming, playtime and optionally regions and displays them nicely in your set Status Channel.
- **Sign Logger**: Log ingame sign placements to Discord with optional censorship (if you want sign channel to be public).
- **Censorship System**: Built-in word filtering or integration with ChatGuard.
- **Bot Activity**: Custom Discord "Playing" status with live player counts.
- **Config Updater**: Automatic configuration updating for future releases.

---

### Commands

| Command            | Description                                           | Usage                                      | Aliases   |
|--------------------|-------------------------------------------------------|--------------------------------------------|-----------|
| `/signremove`      | Removes sign from server and signs.yml.               | `/signremove <signID>`                     | `/sr`     |
| `/censorship`      | Manage blacklisted and whitelisted words.             | `/censorship <subcommand>`                 | `/cship`  |
| `/check`           | Check player via Discord.                   | Discord-based command                      | N/A       |

**Censorship Subcommands**:
- `/censorship blacklist word`
- `/censorship unblacklist word`
- `/censorship whitelist word`
- `/censorship unwhitelist word`
- `/censorship reload`
- `/censorship list`

---

### Permissions

| Permission             | Description                                        | Default |
|------------------------|----------------------------------------------------|---------|
| `signremove`           | Allows use of the `/signremove` command.           | `op`    |
| `censorship.manage`    | Allows managing blacklist/whitelist words.         | `op`    |

---

### Installation

1. Drop `DiscordSystem.jar` into your `plugins` folder.
2. Start the server to generate or update `config.yml`.
3. Edit the configuration as needed.
4. Restart the server to apply changes.
> **Note**: If upgrading from older versions, double check new config first!

---

### Configuration

Most settings are found in `config.yml`.

Rest are inside `/plugins/DiscordSystem`.

#### Important Discord Bot Settings
```yaml
token: "YourBotToken"
bot-id: "YourBotID"
server-name: "MyServer"
bot-activity: "%ServerName% with %onlineCount% players online!"
```
### Contributing

Feel free to fork and contribute via pull requests. Suggestions and improvements are always welcome.

---

Enjoy using DiscordSystem! For issues or questions, message me directly on Discord **@matejkoo** or open an issue in the repository.
