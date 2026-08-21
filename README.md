# Modpack Checker

A lightweight Fabric mod for Minecraft servers to verify that clients are using the correct modpack version.

## Features

- **Lightweight Version Checking**: Simple version file based verification instead of complex modlist comparison
- **Easy Configuration**: Single TOML file with server and client sections
- **Better Disconnection Messages**: Ability to set custom helpful disconnection messages
- **Reload Command**: Easily reload the configuration by using `/modpackchecker-reload` command
- **LAN Multiplayer Support**: Works with both dedicated servers and LAN multiplayer

## Installation

### Server Setup

1. Install the mod on your server
2. The configuration file will be automatically created at `config/modpack-checker.toml`
3. Edit the configuration file to set your desired settings
4. Use `/modpackchecker-reload` to apply configuration changes

### Client Setup

1. Include this mod in your modpack
2. The configuration file will be automatically created at `config/modpack-checker.toml`
3. Edit the configuration file to set the current modpack version
4. Distribute the modpack to your players

## Configuration

#### Combined Configuration (`config/modpack-checker.toml`)

Example configuration available in example-config.toml on github.  
Default generated configuration:

```toml
# Modpack Checker Configuration
# This file contains both server and client configuration

# Modpack version of the server and client, that must match
# version "development" always allows joining
version = "1.0.0"

# Server Configuration
[server]
# Enable or disable modpack version checking
enable = true

# Kick messages for different scenarios
[messages]
# Message shown when client doesn't have the mod installed
no_mod = "Modpack not installed! \n\n Please install modpack version \"{version}\" \n <your-modpack-link>"

# Message shown when client has wrong version (use {version} as placeholder)
wrong_version = "Wrong modpack version installed! \n\n Please install modpack version \"{version}\" \n <your-modpack-link>"

# Message shown when there's a server configuration error
server_error = "Server configuration error. Please contact an administrator."
```

### Error Messages

(Based on the example-config.toml)

- **No Mod**: "Modpack not installed! Please install Create Empire version 1.2.3 from: https://triibu.tech/minecraft"
- **Wrong Version**: "Wrong modpack version installed! Please install Create Empire version 1.2.3 from: https://triibu.tech/minecraft"
- **Server Error**: "Server configuration error. Please contact triibu@triibu.tech or triibunupsik on discord."

### Configuration Options

- `version` - The version that server checks and clients send
- `server.enable` - Enable or disable version checking (true/false)
- `messages.no_mod` - Message shown when client doesn't have the mod (to display the version from config use {version} placeholder)
- `messages.wrong_version` - Message shown when client has wrong version ({version} placeholder can be used here as well)
- `messages.server_error` - Message shown for server configuration errors

## Technical details

1. When a player connects, the server sends a version check request
2. If the client has the mod installed, it reads its configuration file and sends the version back
3. The server compares the client's version with the expected version from its configuration
4. If versions don't match or the client didn't respond (because it's missing the mod), the player is disconnected with a helpful message

### Libraries

- night-config (Licensed under LGPL) for robust TOML configuration parsing
