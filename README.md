# Modpack Checker

A lightweight Fabric mod for Minecraft servers to verify that clients are using the correct modpack version.

## Features

- **Lightweight Version Checking**: Simple version-based verification instead of complex modlist comparison
- **Easy Configuration**: Single TOML file with separate server and client sections
- **Automatic Disconnection**: Players with incorrect versions are automatically disconnected with helpful messages
- **Vanilla Reload Integration**: Configuration reloads automatically when using vanilla's `/reload` command
- **LAN Multiplayer Support**: Works with both dedicated servers and LAN multiplayer

## Installation

### Server Setup

1. Install the mod on your server
2. The configuration file will be automatically created at `config/modpack-checker.toml`
3. Edit the configuration file to set your desired settings
4. Use `/reload` to apply configuration changes

### Client Setup

1. Include this mod in your modpack
2. The configuration file will be automatically created at `config/modpack-checker.toml`
3. Edit the configuration file to set the current modpack version
4. Distribute the modpack to your players

## Configuration

### Combined Configuration (`config/modpack-checker.toml`)

```toml
# Modpack Checker Configuration
# This file contains both server and client configuration

# Server Configuration
[server]
# Enable or disable modpack version checking
enable = true

# Expected modpack version that clients must have
expected_version = "1.2.3"

# Kick messages for different scenarios
[server.messages]
# Message shown when client doesn't have the mod installed
no_mod = "❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft"

# Message shown when client has wrong version (use {version} as placeholder)
wrong_version = "❌ Please install modpack version {version}: https://triibu.tech/minecraft"

# Message shown when there's a server configuration error
server_error = "❌ Server configuration error. Please contact an administrator."

# Client Configuration
[client]
# Current modpack version - this should match the server's expected version
version = "1.2.3"
```

## How It Works

1. When a player connects, the server sends a version check request
2. If the client has the mod installed, it reads its configuration file and sends the version back
3. The server compares the client's version with the expected version from its configuration
4. If versions don't match, the player is disconnected with a helpful message

## Multiplayer Support

The mod supports different multiplayer environments:

- **Dedicated Servers**: Full modpack checking enabled
- **LAN Multiplayer**: Modpack checking enabled (detected automatically)
- **Singleplayer**: Modpack checking disabled to avoid interference

## Configuration Reload

The mod automatically reloads its configuration when you use the vanilla `/reload` command. This allows you to change settings without restarting the server.

## Error Messages

- **No Mod**: "❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft"
- **Wrong Version**: "❌ Please install modpack version X.X.X: https://triibu.tech/minecraft"
- **Server Error**: "❌ Server configuration error. Please contact an administrator."

## Configuration Options

### Server Options

- `server.enable` - Enable or disable version checking (true/false)
- `server.expected_version` - The version that clients must have
- `server.messages.no_mod` - Message shown when client doesn't have the mod
- `server.messages.wrong_version` - Message shown when client has wrong version (use {version} placeholder)
- `server.messages.server_error` - Message shown for server configuration errors

### Client Options

- `client.version` - The current modpack version

## Singleplayer

The mod automatically detects singleplayer environments and disables version checking to avoid interfering with local gameplay. LAN multiplayer is detected separately and version checking is enabled for it.

## Technical Details

- Uses Fabric's login networking for lightweight version verification
- No complex modlist comparison or checksum calculations
- Minimal network overhead
- Compatible with Fabric API 0.92.2+ for Minecraft 1.20.1
- Configuration files are automatically created with sensible defaults
- Uses night-config:toml (Licensed under LGPL) for robust TOML configuration parsing
- Single configuration file for both server and client settings