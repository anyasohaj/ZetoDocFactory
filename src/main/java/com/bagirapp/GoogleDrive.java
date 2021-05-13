package com.bagirapp;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GoogleDrive {
    private Logger logger = Logger.getLogger(GoogleDrive.class.getName());
    private DriveConnection connection;
    private Drive service;
    private String sampleDocId;
    private String targetFolder;



    public GoogleDrive() {
         TrafficData trafficData = TrafficData.getInstance();
        connection = new DriveConnection(DriveConnection.DRIVE);
        try  {
            service = (Drive) connection.getService();
            logger.log(Level.FINE, "Got driveService");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "It was thrown an IOException while getting connection service", e);
        } catch (GeneralSecurityException e) {
            logger.log(Level.SEVERE, "It was thrown a GeneralSecurityException while getting connection service");
        }
        this.sampleDocId = trafficData.getDocID();
        this.targetFolder = trafficData.getTargetFolderId();
    }

    public File copySample(String newTitle) {
        return copySample(sampleDocId, newTitle);
    }

    public File copySample(String reportFormSample, String newTitle){
        File newFile = new File();

        File copiedFile = new File();
        copiedFile.setName(newTitle);
        if (!targetFolder.isEmpty()){
            List<String> folder = new ArrayList<>();
            folder.add(targetFolder);
            copiedFile.setParents(folder);
        }else{
            System.out.println("target folder is empty");
        }

        try {
            newFile = service.files().copy( reportFormSample, copiedFile).execute();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred while copying doc file: " + e);
        }
        logger.log(Level.INFO, "Returning new file with id {0}", newFile.getId());
        return newFile;
    }

    public String uploadImage(String imageFilePath, String imageFileName)  {
        imageFilePath = imageFilePath + imageFileName;
        File fileMetadata = new File();
        fileMetadata.setName(imageFileName);
        fileMetadata.setMimeType("application/vnd.google-apps.document");

        List<String> folder = new ArrayList<>();
        folder.add(targetFolder);
        fileMetadata.setParents(folder);


        java.io.File filePath = new java.io.File(imageFilePath);
        System.out.println("The filePath is " + filePath);
        FileContent mediaContent = new FileContent("image/png", filePath);
        File file = null;
        try {
            file = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            logger.log(Level.INFO, "Uploading image was successful.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occured while uploading image. {0}", e);
        }
        return file.getId();

    }


    public Map<String, String> findDocs() {
        String pageToken = null;
        FileList result = null;
        try {
            result = service.files().list()
                    .setQ("'1SAvxstisFLSHYFUsEmjBHW5M_D3UNzUJ' in parents and mimeType='application/vnd.google-apps.document' and trashed = false")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, parents)")
                    .setPageToken(pageToken)
                    .execute();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error occured while getting doc files. Result might be null. {0}", e);
            return new HashMap<>();
        }

        List<File> files = result.getFiles();
        logger.log(Level.INFO, "There are {0} files. " ,files.size());

        Map<String, String> docIDs = new HashMap<>();
        for (File file : files) {
            String[] docIDS = {file.getName(), file.getId()};
            logger.log(Level.INFO, "File name: {0}, id: {1}" , docIDS);
            docIDs.put(file.getId(), file.getName());
        }
        return docIDs;
    }

    public String createPDF(String fileId, String name)  {

        File pdfFile = new File();
        String[] pdfFileName = name.split("DRAFT");
        pdfFile.setName(pdfFileName[0] + "APPROVING");

        List<String> folder = new ArrayList<>();
        folder.add(targetFolder);
        pdfFile.setParents(folder);

        File file = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            service.files().export(fileId, "application/pdf")
                    .setFields("*")
                    .executeMediaAndDownloadTo(outputStream);
                file = service.files().create(pdfFile, new ByteArrayContent("",
                        outputStream.toByteArray())).setFields("id").execute();


        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exporting pdf file is unsuccessful. {0}" , outputStream.size()>0?"Outputstream is null":"Not managed to creating file from outputstream");
            return "";
        }

        return file.getName();
    }
}

