# the-discord-music-bot

A modern Discord music bot, rewritten from scratch in the spirit of [jagrosh/MusicBot](https://github.com/jagrosh/MusicBot) (JMusicBot).

- **Java 25 (LTS)**: records, sealed interfaces, pattern matching, virtual threads
- **JDA 6** with **JDave** for DAVE, Discord's mandatory end-to-end encrypted voice protocol
- **lavaplayer 2** with the maintained **youtube-source** for YouTube
- **Gradle 9** (Kotlin DSL, version catalog, configuration cache), **JUnit 6**, AssertJ, Mockito

## Commands

Commands are case-insensitive (`!play`, `!P` and `!PLAY` all work).

| Command                | Alias | Description                                                                                                    |
| ---------------------- | ----- | -------------------------------------------------------------------------------------------------------------- |
| `!play <title \| URL>` | `!p`  | Searches YouTube or plays a link; playlist links are queued in full. An attached audio file can be played too. |
| `!skip`                | `!s`  | Votes to skip the current track. The person who requested it skips instantly.                                  |
| `!forceskip`           | `!fs` | Skips without a vote (DJ only).                                                                                |
| `!remove <position>`   |       | Removes a track from the queue. Anyone can remove their own tracks; DJs can remove anyone's.                   |

DJs are the bot owner (`ownerId`), members with the **Manage Server** permission, and members with the `djRoleId` role.

Like JMusicBot, the queue is **fair**: tracks from different users are interleaved so a single user cannot take over the queue.

## Setup

1. Create an application and a bot in the [Discord Developer Portal](https://discord.com/developers/applications).
2. Under **Bot → Privileged Gateway Intents**, enable **MESSAGE CONTENT INTENT**. Prefix commands do not work without it.
3. Invite the bot with these permissions: View Channels, Send Messages, Read Message History, Connect, Speak.
4. Copy `config.example.yml` to `config.yml` and fill in your token, or set the `DISCORD_TOKEN` environment variable.

### Configuration

| Key                         | Default  | Description                                                                                                                                                 |
| --------------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `token`                     | required | Bot token. The `DISCORD_TOKEN` environment variable takes precedence; with it set, the config file is optional.                                             |
| `prefix`                    | `!`      | Command prefix. Must not contain whitespace.                                                                                                                |
| `ownerId`                   | `0`      | Your Discord user ID. The owner is always a DJ. `0` disables this.                                                                                          |
| `djRoleId`                  | `0`      | Role whose members are DJs. `0` disables this.                                                                                                              |
| `skipRatio`                 | `0.55`   | Share of listeners that must vote with `!skip` to skip a track, between 0 and 1. `0` lets anyone skip instantly.                                            |
| `proxy` | empty | HTTP proxy used for all Discord traffic (REST and gateway/voice websockets), as `host:port`. For example `127.0.0.1:8080` for a local [SpoofDPI](https://github.com/xvzc/SpoofDPI). |
| `youtube.ytDlpPath` | bundled | Path to your own [yt-dlp](https://github.com/yt-dlp/yt-dlp). Empty uses the copy bundled in the jar. |
| `youtube.remoteCipherUrl` | empty | URL of a [yt-cipher](https://github.com/kikkia/yt-cipher) server for the clients that need deciphering. Not needed normally. |
| `youtube.remoteCipherPassword` | empty | The `API_TOKEN` of the yt-cipher server, if it has one. |
| `youtube.oauth` | `false` | Sign in to YouTube with a Google account. Not needed normally; if you use it, use a burner account. Without a refresh token, the bot logs a code to enter at google.com/device on startup and then logs the refresh token. |
| `youtube.oauthRefreshToken` | empty    | The refresh token logged after the first sign-in. Setting it skips the sign-in flow and implies `youtube.oauth: true`. |

The config file location can be changed with the `CONFIG_PATH` environment variable. Unknown keys are rejected to catch typos.

### YouTube

The jar needs nothing else installed. Playback goes through these, in order:

1. A built-in visionOS client, which gets plain stream URLs for most videos, including label-owned music videos that the stock youtube-source clients cannot play.
2. The other youtube-source clients.
3. [yt-dlp](https://github.com/yt-dlp/yt-dlp), bundled for macOS, Linux (x64 and arm64) and Windows. On first start it is unpacked to `~/.cache/the-discord-music-bot` (or `$XDG_CACHE_HOME`) and updates itself in the background on every start.

When all of them fail, the bot posts the reason in the channel the command came from.

### Running

```bash
java --enable-native-access=ALL-UNNAMED -jar the-discord-music-bot-0.1.0-SNAPSHOT-all.jar
```

The jar reads `config.yml` from the current working directory.

From source:

```bash
./gradlew run          # development
./gradlew shadowJar    # builds build/libs/*-all.jar
```

With Docker:

```bash
docker build -t music-bot .
docker run -e DISCORD_TOKEN=... music-bot
```

The build downloads the pinned yt-dlp binaries from GitHub and verifies their checksums, so the jar is about 190 MB.

> Discord is blocked in Turkey. To run the bot from there, start a DPI bypass proxy such as SpoofDPI and set `proxy: "127.0.0.1:8080"`; Java does not pick up proxies configured for browsers. A VPN or a server abroad works too.

## Architecture

```
io.github.egeayan.musicbot
├── Main                 entry point: loads config, starts the bot
├── config/              YAML + environment variables, validated on startup
├── music/               pure domain: FairQueue, MusicSession, port interfaces (no JDA/lavaplayer)
├── audio/               lavaplayer adapters: track loader, playback, send handler
├── command/             prefix parser, command registry, DJ policy, CommandContext
│   └── music/           play, skip, forceskip, remove
└── discord/             JDA setup, message listener, per-guild session registry
```

Dependencies point one way: `discord` → `command` / `audio` → `music`. The playback logic (`MusicSession`) knows nothing about Discord or lavaplayer; it reaches the audio engine through the `AudioPlayback` and `TrackLoader` interfaces. Commands see Discord only through `CommandContext`, so tests run against fakes without a Discord connection.

## Development

```bash
./gradlew test             # tests
./gradlew spotlessApply    # formatting (palantir-java-format)
./gradlew build            # format check + tests + jar
```
