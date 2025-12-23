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

## ⚠️ Security Warning

**Storing credentials on a remote server can be a security risk.** For this reason:

- **Create a dedicated Google account** specifically for this plugin's service account
- Do not use your personal Google account
- Keep the `credentials.json` file secure and never commit it to version control
- Consider using environment-specific credentials and rotating them periodically

## Installation

1. Download the AutoWhitelist plugin JAR file
2. Place it in your server's `plugins/` directory
3. Start your server (this will create the config directory structure)
4. Upload your `credentials.json` file to the automatically created config directory:
   ```
   plugins/AutoWhitelist/credentials.json
   ```
5. Configure the plugin with your Google Drive whitelist file details
6. Restart the server

## Configuration

After first launch, a configuration file will be created in the `plugins/AutoWhitelist/` directory. Edit this file to specify:
- Your Google Drive whitelist CSV file ID or name
- Any plugin-specific settings

## Whitelist CSV Format

Your whitelist CSV file on Google Drive should follow this format:

```csv
Playername,01.12.2025,01.01.2026,01.02.2026
Steve,True,True,True
Alex,False,False,False
```

**Column Details:**
- `Playername`: The player's Minecraft username
- Subsequent columns represent the ongoing months with `True` or `False` values indicating whether the player is whitelisted for that month

**Note:** Only the `username` column is mandatory. Additional columns can be added as needed.

## Usage

### Automatic Whitelist Enforcement

The whitelist is automatically enforced:
1. **On server startup** - The plugin loads the latest whitelist from Google Drive
2. **On manual reload** - Using the `/autowhitelist reload` command

### Command-Based Whitelist Updates

You can also manually trigger a whitelist update using the provided command:
```
/autowhitelist reload
```

This will immediately fetch the latest whitelist from Google Drive and update the server whitelist.

## Troubleshooting

- Ensure the service account has read access to the shared Google Drive CSV file
- Verify that `credentials.json` is placed in the correct directory
- Check server logs for any API errors or authentication issues
- Make sure the CSV file format matches the expected structure

## Support

For issues or questions, please open an issue on the project repository.

