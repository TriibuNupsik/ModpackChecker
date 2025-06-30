# Modpack Checker

A lightweight Fabric mod for Minecraft servers to verify that clients are using the correct modpack version.

## Features

- **Lightweight Version Checking**: Simple version-based verification instead of complex modlist comparison
- **Easy Setup**: Just set the expected version on the server and include version files in client modpacks
- **Automatic Disconnection**: Players with incorrect versions are automatically disconnected with helpful messages
- **Admin Commands**: Server administrators can easily manage version checking

## Installation

### Server Setup

1. Install the mod on your server
2. Use the `/modcheck_setversion <version>` command to set the expected version (e.g., `1.2.3`)
3. The expected version will be saved to `expected_version.txt` in your server directory

### Client Setup

1. Include this mod in your modpack
2. Create a file named `modpack_version.txt` in the `.minecraft` folder with the current modpack version
3. Distribute the modpack to your players

## Commands

- `/modcheck_off` - Temporarily disable version checking (requires OP level 4)
- `/modcheck_on` - Re-enable version checking (requires OP level 4)
- `/modcheck_setversion <version>` - Set the expected version (requires OP level 4)

## How It Works

1. When a player connects, the server sends a version check request
2. If the client has the mod installed, it reads its local `modpack_version.txt` file and sends the version back
3. The server compares the client's version with the expected version from `expected_version.txt`
4. If versions don't match, the player is disconnected with a helpful message

## File Locations

- **Server**: `expected_version.txt` (created automatically when setting version)
- **Client**: `modpack_version.txt` (must be created manually in modpack)

## Example Files

### Client Version File (`modpack_version.txt`)
```
1.2.3
```

### Server Expected Version File (`expected_version.txt`)
```
1.2.3
```

## Error Messages

- **No Mod**: "❌ Please install the ModpackChecker mod: https://triibu.tech/minecraft"
- **Wrong Version**: "❌ Please install modpack version X.X.X: https://triibu.tech/minecraft"
- **Server Error**: "❌ Server configuration error. Please contact an administrator."

## Configuration

The mod automatically creates an `expected_version.txt` file in your server directory when you first set a version. You can also manually create this file with the desired version.

## Singleplayer

The mod automatically detects singleplayer environments and disables version checking to avoid interfering with local gameplay.

## Technical Details

- Uses Fabric's login networking for lightweight version verification
- No complex modlist comparison or checksum calculations
- Minimal network overhead
- Compatible with Fabric API 0.92.2+ for Minecraft 1.20.1