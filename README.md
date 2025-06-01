# DiscordSystem

**Author**: ~jkoo

**DiscordSystem** is a powerful and flexible plugin for Minecraft Beta 1.7.3 servers, connecting your in-game chat and server events directly to Discord. From chat bridging and activity tracking to censorship and command execution via Discord, it offers a full suite of tools for seamless integration between your server and your community.

---

### Features

- **Two-Way Chat Sync**: Bridge messages between Minecraft and Discord.
- **Webhook & Bot Modes**: Choose between webhook embeds or normal discord messages.
- **Server Shell**: Allow trusted Discord users to run Minecraft server commands remotly.
- **Sign Logger**: Log in-game sign placements to Discord.
- **Censorship System**: Blacklist/whitelist words with in-game and Discord support.
- **Join/Quit Messages**: Notify Discord when players connect or disconnect.
- **Server Status Embeds**: Auto-send start and shutdown messages to Discord.
- **SleepMessage Toggle**: Customize who receives sleep-related messages.
- **Activity System**: Tracks building, mining, farming, and more.
- **Command Blacklist**: Prevent certain commands from being run via Discord.

---

### Commands

| Command            | Description                                           | Usage                                      | Aliases   |
|--------------------|-------------------------------------------------------|--------------------------------------------|-----------|
| `/signremove`      | Removes sign from server and signs.yml.               | `/signremove <signID>`                     | `/sr`     |
| `/censorship`      | Manage blacklisted and whitelisted words.             | `/censorship <subcommand>`                 | `/cship`  |

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
2. Start the server to generate default `config.yml`.
3. Edit the configuration as needed.
4. Restart the server to apply changes.

---

### Configuration:

Most settings are found in `config.yml`.

#### Discord Bot Settings
```yaml
token: "YourBotToken"
bot-id: "YourBotID"
server-name: "MyServer"
```

#### Message formating
```yaml
webhook:
  enabled: true
  url: "YourWebhookURL"

messages:
  channel-id: "YourMessageChannelID"
  censor: false
  sleepmessages: false
  chat-game-message: "&f[&bDiscord&f] &7%user%: %content%"
  game-chat-message: "**%DisplayName%**: %message%"
  join-message: "**%DisplayName%** Has Joined. [%onlineCount%/%maxCount%]"
  quit-message: "**%DisplayName%** Has Left. [%onlineCount%/%maxCount%]"
```

#### Server Start/Stop Messages
```yaml
server-start-message:
  author: Server
  title: Information
  description: |
    Server is now online!
  color: GREEN

server-shutdown-message:
  author: Server
  title: Information
  description: |
    Server is shutting down.
  color: RED
```

#### Censorship of signs and server-chat.
```yaml
censorship:
  enabled: false
  own: true
```

#### Discord Shell:
```yaml
server-shell:
  enabled: false
  allow-bots: false
  channel-id: "YourShellChannelID"
  allowed-users:
    - discordID1
    - discordID2
```

#### Sign Logging to discord.
```yaml
sign:
  enabled: true
  censor: false
  channel-id: "YourSignChannelID"
```

#### Activity Tracking for status channel.
```yaml
Thresholds:
  building: 5
  gathering: 10
  mining: 5
  farming: 3
  fishing: 2
  hunting: 5
  nether_distance: 30

Activity-Clear: 60
Decay-Time: 360
Decay-Speed: 20
```

#### Command Blacklist, blacklists commands from being ran from discord shell.
```yaml
blacklisted-commands:
  - "-command1"
  - "-command2"
```

#### Debugging, allow only when needed!
```yaml
debug: false
```

---

### Contributing

Feel free to fork and contribute via pull requests. Suggestions and improvements are always welcome.

---

Enjoy using DiscordSystem! For issues or questions, message me directly on Discord (`-matejkoo`) or open an issue in the repository.
