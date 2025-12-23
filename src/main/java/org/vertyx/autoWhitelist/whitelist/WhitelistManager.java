package org.vertyx.autoWhitelist.whitelist;

import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.YearMonth;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manages the in-memory representation of whitelisted players from a CSV file.
 * Handles parsing and validation of whitelist entries.
 *
 * CSV Schema (with headers):
 * Playername,Dec.2025,Jan.2026,Feb.2026,...
 *
 * Each row contains a player name followed by True/False values for each month.
 */
public class WhitelistManager {

    private static Logger logger;
    private static final String PLUGIN_NAME = "AutoWhitelist";

    private final Map<String, WhitelistEntry> whitelistMap;

    /**
     * Creates a new WhitelistManager with an empty whitelist.
     */
    public WhitelistManager(Logger logger) {
        this.whitelistMap = new LinkedHashMap<>();
        WhitelistManager.logger = logger;
    }

    /**
     * Loads the whitelist from a CSV file. Creates a default CSV if the file doesn't exist.
     *
     * @param csvFile The CSV file to load
     * @throws IOException if file reading fails
     */
    public void loadFromCSV(File csvFile) throws IOException {
        if (!csvFile.exists()) {
            logger.info("Whitelist CSV not found at: " + csvFile.getAbsolutePath());
            logger.info("Creating default CSV file...");
            createDefaultCSV(csvFile);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(csvFile.toPath(), StandardCharsets.UTF_8);

            if (lines.isEmpty()) {
                logger.warning("CSV file is empty, creating default structure...");
                createDefaultCSV(csvFile);
                return;
            }

            // Parse header to get month columns
            String headerLine = lines.getFirst().trim();
            String[] headers = headerLine.split(",");

            if (headers.length < 2 || !headers[0].trim().equalsIgnoreCase("Playername")) {
                logger.warning("Invalid CSV header. Expected 'Playername' as first column");
                logger.warning("Creating new default CSV...");
                createDefaultCSV(csvFile);
                return;
            }

            // Parse month columns from header
            List<YearMonth> monthColumns = new ArrayList<>();
            for (int i = 1; i < headers.length; i++) {
                YearMonth month = WhitelistEntry.parseMonthColumn(headers[i].trim());
                if (month != null) {
                    monthColumns.add(month);
                } else {
                    logger.warning("Could not parse month column: " + headers[i]);
                    monthColumns.add(null); // Keep index alignment
                }
            }

            logger.info("Found " + monthColumns.size() + " month columns");

            // Parse data rows
            int successCount = 0;
            int failCount = 0;

            for (int i = 1; i < lines.size(); i++) {
                int lineNumber = i + 1;
                String line = lines.get(i).trim();

                if (line.isEmpty()) {
                    continue; // Skip empty lines
                }

                try {
                    String[] values = line.split(",", -1); // -1 to keep trailing empty strings

                    if (values.length < 1) {
                        logger.warning("Line " + lineNumber + " has no values");
                        failCount++;
                        continue;
                    }

                    String playerName = values[0].trim();
                    if (playerName.isEmpty()) {
                        logger.warning("Line " + lineNumber + " has empty player name");
                        failCount++;
                        continue;
                    }

                    WhitelistEntry entry = new WhitelistEntry(playerName);

                    // Parse access for each month
                    for (int j = 1; j < Math.min(values.length, monthColumns.size() + 1); j++) {
                        YearMonth month = monthColumns.get(j - 1);
                        if (month == null) continue; // Skip columns we couldn't parse

                        String value = values[j].trim();
                        boolean hasAccess = WhitelistEntry.parseBoolean(value);
                        entry.setAccessForMonth(month, hasAccess);
                    }

                    whitelistMap.put(playerName.toLowerCase(), entry);
                    successCount++;

                } catch (Exception e) {
                    logger.warning("Failed to parse line " + lineNumber + ": " + e.getMessage());
                    failCount++;
                }
            }

            logger.info("Loaded whitelist from CSV: " + successCount + " entries, " + failCount + " failed");

        } catch (IOException e) {
            logger.severe("Failed to read CSV file: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a default CSV file with header and example entries.
     * Creates 12 months starting from current month.
     *
     * @param csvFile The CSV file to create
     */
    private void createDefaultCSV(File csvFile) {
        try {
            csvFile.getParentFile().mkdirs();

            List<String> lines = new ArrayList<>();

            // Create header with 12 months starting from current month
            YearMonth currentMonth = YearMonth.now();
            StringBuilder header = new StringBuilder("Playername");
            List<YearMonth> months = new ArrayList<>();

            for (int i = 0; i < 12; i++) {
                YearMonth month = currentMonth.plusMonths(i);
                months.add(month);
                header.append(",").append(WhitelistEntry.formatMonth(month));
            }
            lines.add(header.toString());

            // Add two example entries
            StringBuilder example1 = new StringBuilder("ExamplePlayer1");
            StringBuilder example2 = new StringBuilder("ExamplePlayer2");

            for (int i = 0; i < 12; i++) {
                // ExamplePlayer1 has access for first 3 months
                example1.append(",").append(i < 3 ? "True" : "False");
                // ExamplePlayer2 has access for all 12 months
                example2.append(",").append("True");
            }

            lines.add(example1.toString());
            lines.add(example2.toString());

            Files.write(csvFile.toPath(), lines, StandardCharsets.UTF_8);
            logger.info("Created default CSV at: " + csvFile.getAbsolutePath());

            // Parse the examples for in-memory representation
            WhitelistEntry entry1 = new WhitelistEntry("ExamplePlayer1");
            WhitelistEntry entry2 = new WhitelistEntry("ExamplePlayer2");

            for (int i = 0; i < 12; i++) {
                YearMonth month = months.get(i);
                entry1.setAccessForMonth(month, i < 3);
                entry2.setAccessForMonth(month, true);
            }

            whitelistMap.put("exampleplayer1", entry1);
            whitelistMap.put("exampleplayer2", entry2);

        } catch (IOException e) {
            logger.severe("Failed to create default CSV: " + e.getMessage());
        }
    }

    /**
     * Saves the whitelist to a CSV file.
     *
     * @param csvFile The CSV file to save to
     * @throws IOException if file writing fails
     */
    public void saveToCSV(File csvFile) throws IOException {
        try {
            csvFile.getParentFile().mkdirs();

            List<String> lines = new ArrayList<>();

            // Collect all unique months from all entries, sorted
            Set<YearMonth> allMonths = new TreeSet<>();
            for (WhitelistEntry entry : whitelistMap.values()) {
                allMonths.addAll(entry.getMonths());
            }

            // Build header
            StringBuilder header = new StringBuilder("Playername");
            List<YearMonth> monthsList = new ArrayList<>(allMonths);
            for (YearMonth month : monthsList) {
                header.append(",").append(WhitelistEntry.formatMonth(month));
            }
            lines.add(header.toString());

            // Build data rows
            for (WhitelistEntry entry : whitelistMap.values()) {
                StringBuilder row = new StringBuilder(entry.getPlayerName());
                for (YearMonth month : monthsList) {
                    boolean hasAccess = entry.hasAccessForMonth(month);
                    row.append(",").append(WhitelistEntry.formatBoolean(hasAccess));
                }
                lines.add(row.toString());
            }

            Files.write(csvFile.toPath(), lines, StandardCharsets.UTF_8);
            logger.info("Saved whitelist to CSV: " + whitelistMap.size() + " entries");

        } catch (IOException e) {
            logger.severe("Failed to save CSV file: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Applies the whitelist entries to the server's whitelist.
     */
    public void applyToServerWhitelist(org.bukkit.Server server) {
        org.bukkit.OfflinePlayer[] currentWhitelistedPlayers = server.getWhitelistedPlayers().toArray(new org.bukkit.OfflinePlayer[0]);
        Set<String> currentWhitelistSet = new HashSet<>();
        for (org.bukkit.OfflinePlayer player : currentWhitelistedPlayers) {
            currentWhitelistSet.add(Objects.requireNonNull(player.getName()).toLowerCase());
        }

        // Add players who should be whitelisted
        for (WhitelistEntry entry : whitelistMap.values()) {
            if (entry.hasCurrentAccess()) {
                String playerName = entry.getPlayerName();
                if (!currentWhitelistSet.contains(playerName.toLowerCase())) {
                    org.bukkit.OfflinePlayer player = server.getOfflinePlayer(playerName);
                    player.setWhitelisted(true);
                    logger.info("Added to server whitelist: " + playerName);
                }
            }
        }

        // Remove players who should not be whitelisted
        for (org.bukkit.OfflinePlayer player : currentWhitelistedPlayers) {
            String playerName = player.getName();
            assert playerName != null;
            WhitelistEntry entry = getEntry(playerName);
            if (entry == null || !entry.hasCurrentAccess()) {
                // Kick if online
                if (player.isOnline()) {
                    org.bukkit.entity.Player onlinePlayer = player.getPlayer();
                    if (onlinePlayer != null) {
                        onlinePlayer.kick(Component.text("You have been removed from the whitelist."));
                    }
                }
                player.setWhitelisted(false);
                logger.info("Removed from server whitelist: " + playerName);
            }
        }
    }

    /**
     * Retrieves a whitelist entry by player name.
     *
     * @param playerName The player's name (case-insensitive)
     * @return The WhitelistEntry or null if not found
     */
    public WhitelistEntry getEntry(String playerName) {
        return whitelistMap.get(playerName.toLowerCase());
    }

    /**
     * Checks if a player is whitelisted and has access for the current month.
     *
     * @param playerName The player's name
     * @return true if the player exists and has access for current month
     */
    public boolean isWhitelisted(String playerName) {
        WhitelistEntry entry = getEntry(playerName);
        return entry != null && entry.hasCurrentAccess();
    }

    /**
     * Gets all whitelisted entries.
     *
     * @return Collection of all WhitelistEntry objects
     */
    public Collection<WhitelistEntry> getAllEntries() {
        return new ArrayList<>(whitelistMap.values());
    }

    /**
     * Gets the number of entries in the whitelist.
     *
     * @return Entry count
     */
    public int getEntryCount() {
        return whitelistMap.size();
    }

    /**
     * Checks if an entry exists for a player.
     *
     * @param playerName The player's name
     * @return true if entry exists
     */
    public boolean hasEntryForPlayer(String playerName) {
        return whitelistMap.containsKey(playerName.toLowerCase());
    }

    /**
     * Clears all entries.
     */
    public void clear() {
        whitelistMap.clear();
    }
}

