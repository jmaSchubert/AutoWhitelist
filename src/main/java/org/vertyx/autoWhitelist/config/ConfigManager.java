package org.vertyx.autoWhitelist.config;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * Manages configuration files for AutoWhitelist plugin.
 * Automatically creates the config directory and handles credentials.json file.
 */
public class ConfigManager {

    private static Logger logger;

    private static final String PLUGIN_NAME = "AutoWhitelist";
    public static final String CREDENTIALS_FILE = "credentials.json";
    public static final String DEFAULT_FOLDER_NAME = "AutoWhitelistPlugin";
    public static final String DEFAULT_CSV_FILENAME = "whitelist.csv";
    public static final String LOCAL_CSV_NAME = "whitelist.csv";

    public static File configDirectory;
    private static File credentialsFile;
    public static File cacheDirectory;
    private static File configFile;

    /**
     * Initializes the config manager and creates necessary directories.
     * Should be called during plugin startup.
     */
    public static void initialize(Logger logger) {
        try {
            ConfigManager.logger = logger;
            // Determine the plugins directory
            // When running in a Spigot/Paper server, this will be the 'plugins' folder
            String pluginsPath = System.getProperty("user.dir") + File.separator + "plugins"
                + File.separator + PLUGIN_NAME;

            configDirectory = new File(pluginsPath);

            // Create directories if they don't exist
            if (!configDirectory.exists()) {
                configDirectory.mkdirs();
            }

            credentialsFile = new File(configDirectory, CREDENTIALS_FILE);

            // Check if credentials.json exists
            if (!credentialsFile.exists()) {
                logger.warning(CREDENTIALS_FILE + " not found at: " + credentialsFile.getAbsolutePath());
            } else {
                logger.info(CREDENTIALS_FILE + " found at: " + credentialsFile.getAbsolutePath());
            }

            // Create a cache directory to download the csv file locally
            cacheDirectory = new File(configDirectory, "cache");
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs();
            }

            // Create config.yml if it doesn't exist
            configFile = new File(configDirectory, "config.yml");
            if (!configFile.exists()) {
                try {
                    String defaultConfig = "# AutoWhitelist Configuration\n" +
                            "# This plugin is READ-ONLY - it downloads the whitelist CSV from Google Drive\n" +
                            "# To make changes, edit the CSV file directly on Google Drive\n" +
                            "\n" +
                            "# The Google Drive folder name containing the whitelist CSV\n" +
                            "# This folder must be shared with your service account email\n" +
                            "drive-folder-name: \"" + DEFAULT_FOLDER_NAME + "\"\n" +
                            "\n" +
                            "# The CSV filename in the Google Drive folder\n" +
                            "drive-csv-filename: \"" + DEFAULT_CSV_FILENAME + "\"\n" +
                            "\n" +
                            "# Local name of the CSV file in the cache directory\n" +
                            "local-csv-name: \"" + LOCAL_CSV_NAME + "\"\n";

                    Files.writeString(configFile.toPath(), defaultConfig);
                    logger.info("Default config.yml created at: " + configFile.getAbsolutePath());
                } catch (IOException e) {
                    logger.severe("Failed to create config.yml: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.severe("Error initializing ConfigManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the credentials.json file.
     * @return The File object representing credentials.json
     */
    public static File getCredentialsFile() {
        if (credentialsFile == null) {
            initialize(ConfigManager.logger);
        }
        return credentialsFile;
    }

    /**
     * Gets the config.yml file.
     * @return The File object representing config.yml
     */
    public static File getConfigFile() {
        if (configFile == null) {
            initialize(ConfigManager.logger);
        }
        return configFile;
    }

    /**
     * Gets the local CSV file path in cache directory.
     * @return Path to the local whitelist CSV
     */
    public static File getLocalCsvFile() {
        return new File(cacheDirectory, LOCAL_CSV_NAME);
    }
}
