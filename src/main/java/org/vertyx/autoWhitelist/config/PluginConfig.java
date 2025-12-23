package org.vertyx.autoWhitelist.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Reads and parses the plugin's config.yml file.
 * Read-only configuration for downloading CSV from Google Drive.
 */
public class PluginConfig {

    private static Logger logger;

    private String driveFolderName;
    private String driveCsvFilename;
    private String localCsvName;

    /**
     * Loads configuration from config.yml.
     */
    public PluginConfig(Logger logger) {
        PluginConfig.logger = logger;
        loadConfig();
    }

    /**
     * Loads the configuration from the config.yml file.
     */
    private void loadConfig() {
        // Set defaults
        driveFolderName = ConfigManager.DEFAULT_FOLDER_NAME;
        driveCsvFilename = ConfigManager.DEFAULT_CSV_FILENAME;
        localCsvName = ConfigManager.LOCAL_CSV_NAME;

        File configFile = ConfigManager.getConfigFile();
        if (!configFile.exists()) {
            logger.warning("config.yml not found, using defaults");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse key-value pairs
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String value = parts.length > 1 ? parts[1].trim() : "";

                    // Remove quotes from values
                    value = value.replaceAll("^\"|\"$", "");

                    switch (key) {
                        case "drive-folder-name":
                            if (!value.isEmpty()) {
                                driveFolderName = value;
                            }
                            break;
                        case "drive-csv-filename":
                            if (!value.isEmpty()) {
                                driveCsvFilename = value;
                            }
                            break;
                        case "local-csv-name":
                            if (!value.isEmpty()) {
                                localCsvName = value;
                            }
                            break;
                    }
                }
            }

        } catch (IOException e) {
            logger.severe("Failed to read config.yml: " + e.getMessage());
        }
    }

    public String getDriveFolderName() {
        return driveFolderName;
    }

    public String getDriveCsvFilename() {
        return driveCsvFilename;
    }

    public String getLocalCsvName() {
        return localCsvName;
    }
}
