package org.vertyx.autoWhitelist.GoogleDrive;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import org.vertyx.autoWhitelist.config.ConfigManager;

import java.io.*;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Read-only access to Google Drive for downloading whitelist CSV files.
 * Uses Application Default Credentials (ADC) for authentication.
 * The plugin downloads CSV from Drive but does not upload changes.
 * Administrators must edit the CSV directly on Google Drive.
 */
public class DriveAccess {
    private static Logger logger;
    private static final String PLUGIN_NAME = "AutoWhitelist";
    private static final String APPLICATION_NAME = "AutoWhitelist Drive Access";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final Drive service;

    /**
     * Initializes the Drive API service with Application Default Credentials.
     * Searches for credentials in the following order:
     * 1. GOOGLE_APPLICATION_CREDENTIALS environment variable (path to service account JSON)
     * 2. Service account JSON in the plugin config directory (credentials.json)
     * 3. Gcloud SDK credentials
     * 4. Google Cloud environment credentials
     *
     * @throws GeneralSecurityException if there's a security error
     * @throws IOException if credentials cannot be loaded
     */
    public DriveAccess(Logger logger) throws GeneralSecurityException, IOException {
        DriveAccess.logger = logger;
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredentials credentials = getCredentials();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Tests access to Google Drive API.
     * @return true if access is successful
     * @throws Exception if access test fails
     */
    public boolean testAccess() throws Exception {
        try {
            // Test basic Drive API access with shared drives support
            service.files().list()
                    .setSupportsAllDrives(true)
                    .setIncludeItemsFromAllDrives(true)
                    .setPageSize(1)
                    .setFields("files(id, name)")
                    .execute();

            logger.info("Successfully accessed Google Drive API");
            return true;

        } catch (IOException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";

            if (errorMsg.contains("401")) {
                throw new Exception("Drive access denied (401). Check service account permissions.", e);
            } else if (errorMsg.contains("403")) {
                throw new Exception("Drive access forbidden (403). Check folder sharing.", e);
            } else {
                throw new Exception("Failed to access Google Drive: " + errorMsg, e);
            }
        }
    }

    /**
     * Finds a file by folder name and file name in Google Drive.
     * Searches across all drives including shared drives.
     *
     * @param folderName The name of the folder containing the file
     * @param fileName The name of the file to find
     * @return The file ID or null if not found
     */
    public String findFileByFolderAndName(String folderName, String fileName) {
        try {
            // First find the folder by name
            logger.info("Searching for folder: " + folderName);
            String folderQuery = "name='" + folderName.replace("'", "\\'") + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";

            FileList folderResult = service.files().list()
                    .setSupportsAllDrives(true)
                    .setIncludeItemsFromAllDrives(true)
                    .setQ(folderQuery)
                    .setPageSize(10)
                    .setFields("files(id, name)")
                    .execute();

            List<File> folders = folderResult.getFiles();
            if (folders == null || folders.isEmpty()) {
                logger.severe("Folder not found: " + folderName);
                return null;
            }

            // Search in all matching folders (in case of duplicates)
            for (File folder : folders) {
                String folderId = folder.getId();

                String fileQuery = "name='" + fileName.replace("'", "\\'") + "' and '" + folderId + "' in parents and trashed=false";

                FileList fileResult = service.files().list()
                        .setSupportsAllDrives(true)
                        .setIncludeItemsFromAllDrives(true)
                        .setQ(fileQuery)
                        .setPageSize(1)
                        .setFields("files(id, name)")
                        .execute();

                List<File> files = fileResult.getFiles();
                if (files != null && !files.isEmpty()) {
                    String fileId = files.getFirst().getId();
                    logger.info("Found file '" + fileName + "' in folder (" + fileId + ")");
                    return fileId;
                }
            }

            return null;

        } catch (IOException e) {
            logger.severe("Failed to search for file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Downloads a file from Google Drive by file ID to a local path.
     * @param fileId The Google Drive file ID
     * @param localPath The local file path to save to
     * @return true if successful
     */
    public boolean downloadFileById(String fileId, String localPath) {
        try {
            File driveFile = service.files().get(fileId)
                    .setSupportsAllDrives(true)
                    .setFields("id, name, mimeType")
                    .execute();

            try (OutputStream outputStream = new FileOutputStream(localPath)) {
                service.files().get(fileId)
                        .setSupportsAllDrives(true)
                        .executeMediaAndDownloadTo(outputStream);
            }

            logger.info("Successfully downloaded file from drive");
            return true;

        } catch (IOException e) {
            logger.severe("Failed to download file (ID: " + fileId + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * Downloads a file from Google Drive by folder and file name.
     * @param folderName The name of the folder containing the file
     * @param fileName The name of the file to download
     * @param localPath The local file path to save to
     * @return true if successful
     */
    public boolean downloadFile(String folderName, String fileName, String localPath) {
        String fileId = findFileByFolderAndName(folderName, fileName);
        if (fileId == null) {
            logger.severe("Cannot download file - not found in Drive");
            return false;
        }

        return downloadFileById(fileId, localPath);
    }

    /**
     * Loads credentials using Application Default Credentials (ADC).
     * Tries multiple credential sources in order:
     * 1. Explicit service account JSON from plugin config (credentials.json)
     * 2. GOOGLE_APPLICATION_CREDENTIALS environment variable
     * 3. Default application credentials (gcloud, environment, etc.)
     *
     * @return GoogleCredentials with Drive read-only scope
     * @throws IOException if credentials cannot be loaded
     */
    private static GoogleCredentials getCredentials() throws IOException {
        GoogleCredentials credentials = null;

        // Try loading from explicit credentials.json file first
        java.io.File credentialsFile = ConfigManager.getCredentialsFile();
        if (credentialsFile.exists()) {
            try (InputStream is = new FileInputStream(credentialsFile)) {
                credentials = GoogleCredentials.fromStream(is)
                        .createScoped(Collections.singleton(DriveScopes.DRIVE_READONLY));
                logger.info("Loaded credentials from: " + credentialsFile.getAbsolutePath());
                return credentials;
            } catch (IOException e) {
                logger.warning("Failed to load credentials from " + credentialsFile.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        // Fall back to Application Default Credentials
        try {
            credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singleton(DriveScopes.DRIVE_READONLY));
            logger.info("Loaded credentials from Application Default Credentials");
            return credentials;
        } catch (IOException e) {
            throw new IOException(
                    "Could not find credentials. Please ensure one of the following:\n" +
                    "1. Place service account JSON at: " + credentialsFile.getAbsolutePath() + "\n" +
                    "2. Set GOOGLE_APPLICATION_CREDENTIALS environment variable\n" +
                    "3. Use gcloud auth application-default login\n" +
                    "Error: " + e.getMessage(), e);
        }
    }
}
