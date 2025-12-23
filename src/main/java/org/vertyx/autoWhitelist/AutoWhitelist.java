package org.vertyx.autoWhitelist;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.vertyx.autoWhitelist.config.ConfigManager;
import org.vertyx.autoWhitelist.config.PluginConfig;
import org.vertyx.autoWhitelist.GoogleDrive.DriveAccess;
import org.vertyx.autoWhitelist.whitelist.WhitelistManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class AutoWhitelist extends JavaPlugin implements TabCompleter {

    private WhitelistManager whitelistManager;
    private DriveAccess driveAccess;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        // Initialize configuration manager to create necessary directories
        ConfigManager.initialize(getLogger());

        // Load plugin configuration
        pluginConfig = new PluginConfig(getLogger());

        // Initialize DriveAccess
        try {
            driveAccess = new DriveAccess(getLogger());
            boolean accessOk = driveAccess.testAccess();
            if (!accessOk) {
                throw new RuntimeException("Cannot access Google Drive with provided credentials");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Google Drive access: " + e.getMessage());
            driveAccess = null;
        }

        // Download remote whitelist CSV before loading
        File localCsvFile = ConfigManager.getLocalCsvFile();
        if (driveAccess != null) {
            boolean downloaded = driveAccess.downloadFile(
                    pluginConfig.getDriveFolderName(),
                    pluginConfig.getDriveCsvFilename(),
                    localCsvFile.getAbsolutePath()
            );

            if (!downloaded) {
                getLogger().warning("Failed to download CSV from Drive Will use local cached copy if available");
            }
        }

        // Initialize WhitelistManager with local CSV
        try {
            whitelistManager = new WhitelistManager(getLogger());
            whitelistManager.loadFromCSV(localCsvFile);
            getLogger().info("WhitelistManager loaded with " + whitelistManager.getEntryCount() + " entries");
        } catch (Exception e) {
            getLogger().severe("Failed to load whitelist from CSV: " + e.getMessage());
            whitelistManager = null;
        }

        // For all player names in the whitelist, update the server whitelist
        if (whitelistManager != null) {
            whitelistManager.applyToServerWhitelist(getServer());
            getLogger().info("Server whitelist updated.");
        }

        runStartupSync();

        // Register tab completer
        this.getCommand("autowhitelist").setTabCompleter(this);
    }

    private boolean runStartupSync() {
        // Reload config each time we sync
        pluginConfig = new PluginConfig(getLogger());

        // Initialize Drive access
        try {
            driveAccess = new DriveAccess(getLogger());
            if (!driveAccess.testAccess()) {
                throw new RuntimeException("Cannot access Google Drive with provided credentials");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Google Drive access: " + e.getMessage());
            driveAccess = null;
        }

        File localCsvFile = ConfigManager.getLocalCsvFile();

        // Try to download remote CSV first
        if (driveAccess != null) {
            boolean downloaded = driveAccess.downloadFile(
                    pluginConfig.getDriveFolderName(),
                    pluginConfig.getDriveCsvFilename(),
                    localCsvFile.getAbsolutePath()
            );
            if (!downloaded) {
                getLogger().warning("Failed to download CSV from Drive. Using local cached copy if available.");
            }
        }

        // Load whitelist and apply
        try {
            whitelistManager = new WhitelistManager(getLogger());
            whitelistManager.loadFromCSV(localCsvFile);
            whitelistManager.applyToServerWhitelist(getServer());
            getLogger().info("Whitelist reload complete. Entries: " + whitelistManager.getEntryCount());
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to reload whitelist: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("autowhitelist")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("AutoWhitelist: reloading whitelist...");
            boolean ok = runStartupSync();
            sender.sendMessage(ok ? "AutoWhitelist: reload successful." : "AutoWhitelist: reload failed. Check console.");
            return true;
        }

        sender.sendMessage("Usage: /autowhitelist reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("autowhitelist")) {
            return new ArrayList<>();
        }

        // Return suggestions for first argument
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String input = args[0].toLowerCase();

            if ("reload".startsWith(input)) {
                suggestions.add("reload");
            }

            return suggestions;
        }

        return new ArrayList<>();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
