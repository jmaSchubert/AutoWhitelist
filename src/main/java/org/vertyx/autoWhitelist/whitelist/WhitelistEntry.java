package org.vertyx.autoWhitelist.whitelist;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Represents a single whitelist entry with player information and monthly access records.
 *
 * CSV Schema:
 * - Playername: Minecraft username
 * - 01.12.2025, 01.01.2026, 01.02.2026, etc.: Boolean values (True/False) for each month
 */
public class WhitelistEntry {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH);

    private final String playerName;
    private final Map<YearMonth, Boolean> monthlyAccess;

    /**
     * Creates a new whitelist entry.
     * @param playerName The player's Minecraft username
     */
    public WhitelistEntry(String playerName) {
        this.playerName = playerName;
        this.monthlyAccess = new LinkedHashMap<>();
    }

    /**
     * Sets access for a specific month.
     * @param month The YearMonth
     * @param hasAccess Whether the player has access for this month
     */
    public void setAccessForMonth(YearMonth month, boolean hasAccess) {
        monthlyAccess.put(month, hasAccess);
    }

    /**
     * Gets access status for a specific month.
     * @param month The YearMonth
     * @return true if the player has access for this month
     */
    public boolean hasAccessForMonth(YearMonth month) {
        return monthlyAccess.getOrDefault(month, false);
    }

    /**
     * Checks if the player has access for the current month.
     * @return true if the player has access for the current month
     */
    public boolean hasCurrentAccess() {
        YearMonth currentMonth = YearMonth.now();
        return hasAccessForMonth(currentMonth);
    }

    /**
     * Gets all months for which access is recorded.
     * @return Set of YearMonth entries
     */
    public Set<YearMonth> getMonths() {
        return new LinkedHashSet<>(monthlyAccess.keySet());
    }

    /**
     * Gets the monthly access map.
     * @return Map of YearMonth to Boolean access status
     */
    public Map<YearMonth, Boolean> getMonthlyAccess() {
        return new LinkedHashMap<>(monthlyAccess);
    }

    /**
     * Parses a month column header (e.g., "01.12.2025") into a YearMonth.
     * @param columnHeader The column header string
     * @return YearMonth or null if parsing fails
     */
    public static YearMonth parseMonthColumn(String columnHeader) {
        try {
            LocalDate date = LocalDate.parse(columnHeader, MONTH_FORMATTER);
            return YearMonth.from(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Formats a YearMonth as a column header (e.g., "01.12.2025").
     * Always uses the 1st day of the month.
     * @param month The YearMonth to format
     * @return Formatted string
     */
    public static String formatMonth(YearMonth month) {
        return MONTH_FORMATTER.format(month.atDay(1));
    }

    /**
     * Parses a boolean value from a CSV cell.
     * @param value The string value from CSV
     * @return true if "True" (case-insensitive), false otherwise
     */
    public static boolean parseBoolean(String value) {
        return value != null && value.trim().equalsIgnoreCase("True");
    }

    /**
     * Formats a boolean value for CSV output.
     * @param value The boolean value
     * @return "True" or "False"
     */
    public static String formatBoolean(boolean value) {
        return value ? "True" : "False";
    }

    // Getters
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhitelistEntry entry = (WhitelistEntry) o;
        return playerName.equalsIgnoreCase(entry.playerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName.toLowerCase());
    }

    @Override
    public String toString() {
        return "WhitelistEntry{" +
                "playerName='" + playerName + '\'' +
                ", monthlyAccess=" + monthlyAccess +
                '}';
    }
}
