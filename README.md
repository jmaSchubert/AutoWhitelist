# AutoWhitelist

A Minecraft Paper 1.21.11 plugin that automatically manages server whitelist from a Google Drive spreadsheet.

## Overview

AutoWhitelist allows you to maintain your server's whitelist using a Google Drive CSV file. This is particularly useful for cloud-hosted servers where you want centralized whitelist management without manual server restarts for whitelist updates.

## Requirements

### Minecraft Server
- **Minecraft Version:** Paper 1.21.11 or compatible

### Google Cloud Setup

This plugin requires a Google Cloud Console account with the Google Drive API enabled. Follow these setup steps:

1. **Enable Google Drive API**
   - Follow the official guide: [Google Drive API Quickstart for Java](https://developers.google.com/workspace/drive/api/quickstart/java)
   - This will guide you through creating a Google Cloud project and enabling the Drive API

2. **Create a Service Account**
   - Since this plugin typically runs on cloud hosting providers, a service account is required to read the whitelist CSV file
   - Follow the official guide: [Creating and Managing Service Accounts](https://docs.cloud.google.com/iam/docs/service-accounts-create)
   - The service account will be used to authenticate API requests to Google Drive

3. **Download Service Account Credentials**
   - After creating the service account, download the `credentials.json` file
   - This file contains the authentication keys needed to access Google Drive

##  Security Warning

**Storing credentials on a remote server can be a security risk.** For this reason:

- **Create a dedicated Google account** specifically for this plugin's service account
- Do not use your personal Google account
- Keep the `credentials.json` file secure and never commit it to version control

## Installation

1. **Download the AutoWhitelist plugin JAR file**
   - Get the latest release from the GitHub releases page

2. **Place in plugins directory**
   - Copy the JAR file to your server's `plugins/` directory

3. **Start the server**
   - Start your server to generate the config directory structure
   - The plugin will automatically create: `plugins/AutoWhitelist/`

4. **Upload credentials.json**
   - Place your Google Drive service account `credentials.json` file in:
     ```
     plugins/AutoWhitelist/credentials.json
     ```
   - **Important:** Never commit this file to version control or share it publicly.

5. **Configure the plugin**
   - Edit `plugins/AutoWhitelist/config.yml` with your Google Drive whitelist file details
   - Specify the Google Drive file ID or file name to use

6. **Restart the server**
   - Restart to apply the configuration and load the whitelist

## Configuration

After first launch, a configuration file will be created in the `plugins/AutoWhitelist/` directory. Edit this file to specify:
- Your Google Drive whitelist CSV file ID or name
- Any plugin-specific settings

## Whitelist CSV Format

Your whitelist CSV file on Google Drive should follow this format:

```csv
Playername,01.12.2025,01.01.2026,01.02.2026
Steve,True,True,True
Alex,True,False,True
```

**Column Details:**
- `Playername`: The player's Minecraft username (first column, case-sensitive)
- Subsequent columns: Can represent dates, months, or any custom period with `True` or `False` values
  - `True` = Player is whitelisted for that period
  - `False` = Player is NOT whitelisted for that period

**Requirements:**
- The first column **must** be named `Playername`
- Player usernames are case-sensitive (must match exact Minecraft username)
- CSV file should not contain empty rows
- Only rows with `True` in any enabled column will be whitelisted

## Usage

### Automatic Whitelist Enforcement

The whitelist is automatically enforced:
1. **On server startup** - The plugin loads the latest whitelist from Google Drive
2. **On manual reload** - Using the `/autowhitelist reload` command

### Command-Based Whitelist Updates

You can manually trigger a whitelist update using the provided command:

**Command Syntax:**
```
/autowhitelist reload
```

**Command Options:**
- `/autowhitelist reload` - Fetch the latest whitelist from Google Drive and update the server

This will immediately fetch the latest whitelist from Google Drive and update the server whitelist without requiring a restart.

## Troubleshooting

### Getting Help

- Check server logs for detailed error messages
- Verify all files are in the correct directories
- Ensure Google Cloud project is properly configured
- Open an issue on the GitHub repository with relevant log excerpts

### Support
> As this plugin is very minimal and heavily dependent on third-party services, support is limited. 

## Contributing

Contributions are welcome! If you'd like to contribute to this project:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Building from Source

### Requirements
- Java 21 JDK
- Gradle 8.0+

### Build Steps
```bash
# Clone the repository
git clone https://github.com/vertyx/AutoWhitelist.git
cd AutoWhitelist

# Build the plugin
./gradlew shadowJar

# The built JAR will be in: build/libs/AutoWhitelist-*.jar
```

