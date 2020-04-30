package com.bagirapp;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DriveConnection {
    private Logger logger = Logger.getLogger(DriveConnection.class.getName());
    public static final String SHEET = "sheet";
    public static final String DOC = "doc";
    public static final String DRIVE = "drive";
    private final String type;

    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPE = Arrays.asList(SheetsScopes.SPREADSHEETS, DocsScopes.DOCUMENTS, DriveScopes.DRIVE);
    private static final String APPLICATION_NAME = "ZetoDocFactory";


    public DriveConnection(String type) {
        this.type = type;
        logger.log(Level.INFO, "DriveConnect instance is successfully created.");
    }

    private Credential authorize() throws IOException, GeneralSecurityException {
        InputStream in = DriveConnection.class.getResourceAsStream("/client_secret_803394122411-it1ir00clfiomgaks0mo9bg6el0p3jlq.apps.googleusercontent.com.json");

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), clientSecrets, SCOPE)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        LocalServerReceiver localServerReceiver = new LocalServerReceiver.Builder().build();

        String redirectUrl;

        try {
            redirectUrl = localServerReceiver.getRedirectUri();
            logger.log(Level.INFO,  "LocalserverReciever redirectUri: {0}" ,  redirectUrl);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Somewhat Exception has been caused by getRedirect() method");
        }

       AuthorizationCodeInstalledApp installedApp = new AuthorizationCodeInstalledApp(flow, localServerReceiver);
            //Bypass

        Credential credential = null;
        try {
            credential = installedApp.authorize("milanbalazs");
            logger.log(Level.INFO, "Credential is not null");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occured while creating credential");
        }
        return credential;
    }


    public AbstractGoogleJsonClient getService() throws IOException, GeneralSecurityException {
        logger.log(Level.FINE, "Starting to connect driveService");
        Credential credential = authorize();
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        if (this.type.equals(SHEET)) {
            return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } else if (this.type.equals(DOC)) {
            return new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } else if (this.type.equals(DRIVE)) {
            return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

        } else {
            logger.log(Level.SEVERE, "Service returns with null");
            return null;
        }
    }


}