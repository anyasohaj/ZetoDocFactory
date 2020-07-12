package com.bagirapp;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

public class TrafficData {
    private Logger logger = Logger.getLogger(TrafficData.class.getName());
    private String sheetHtml;
    private String docHtml;
    private String sheetName;
    private String targetFolderId;
    private  String imageFolder;
    private LocalDate date;
    private ArrayList<Integer> rows;
    private boolean replaceImage;
    private boolean isPDFNeeded;
    private boolean isAutomatGeneration;

    public static final String SHEET_URL = "sheet";
    public static final String DOC_URL = "doc";
    public static final String SHEET_NAME = "sheetName";
    public static final String TARGET_FOLDER = "targetFolder";
    public static final String IMAGE_FOLDER = "imageFolder";

    private static Preferences preferences = Preferences.userNodeForPackage(TrafficData.class);

    private static TrafficData trafficData = new TrafficData(preferences.get(SHEET_URL, ""), preferences.get(DOC_URL, ""), preferences.get(IMAGE_FOLDER, "c:"), preferences.get(TARGET_FOLDER, ""), preferences.get(SHEET_NAME, "Sheet"));

    private TrafficData(String sheetHtmlInput, String docHtmlInput, String imageFolderPath,  String targetFolderInput, String sheetNameInput) {
        this.sheetHtml = sheetHtmlInput;
        this.docHtml = docHtmlInput;
        this.sheetName = sheetNameInput;
        this.imageFolder = imageFolderPath;
        this.targetFolderId = getGoogleFolderId(targetFolderInput);
        this.date = LocalDate.now();
        this.rows = new ArrayList<>();
        this.replaceImage = false;
        this.isPDFNeeded = false;
        this.isAutomatGeneration = true;

        preferences.addPreferenceChangeListener(new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                if (evt.getKey().equals(TARGET_FOLDER)) {
                    targetFolderId = preferences.get(evt.getKey(), evt.getNewValue());
                } else if (evt.getKey().equals(SHEET_URL)) {
                    sheetHtml = preferences.get(evt.getKey(), evt.getNewValue());
                } else if (evt.getKey().equals(DOC_URL)) {
                    docHtml = preferences.get(evt.getKey(), evt.getNewValue());
                } else if (evt.getKey().equals(SHEET_NAME)) {
                    sheetName = preferences.get(evt.getKey(), evt.getNewValue());
                }else if (evt.getKey().equals(IMAGE_FOLDER)){
                    imageFolder = preferences.get(evt.getKey(), evt.getNewValue());
                }
            }
        });

    }

    public static TrafficData getInstance() {
        return trafficData;
    }

    public String getImageFolder() {
        return imageFolder;
    }

    public void setImageFolder(String imageFolder) {
        this.imageFolder = imageFolder;
    }

    private String getGoogleId(String html) {
        if (html.contains("/d/")) {
            String[] firstCut = html.split("/d/");
            String[] secondCut = firstCut[1].split("/edit");
            logger.log(Level.INFO, "The document ID is {0}" , secondCut[0]);
            return secondCut[0].trim();
        }else{
            logger.log(Level.INFO, "There was not a url. GoogleId: {0}", html);
            return html;
        }
    }

    private String getGoogleFolderId(String html){
        if (html.contains("folders/")) {
            String[] cutOffStrings = html.split("folders/");
            logger.log(Level.INFO, "FolderId is {0}", cutOffStrings[1]);
            return cutOffStrings[1];
        }else{
            if (html.isBlank()){
                logger.log(Level.WARNING, "Target folder is not adjusted!");
            }
            logger.log(Level.INFO, "There was not a url. GoogleFolderId: {0}", html);
            return html;
        }

    }

    public String setRows(ArrayList<Integer> rows){
        StringBuilder response = new StringBuilder();

        this.rows.clear();
        this.rows.addAll(rows);

        response.append("Rows are ");
        for (int i : rows){
            response.append(i);
            response.append(", ");
        }
        return response.toString();
    }

    public String setRows(String input) {
        rows.clear();
        String response = "The rows can NOT be interpreted\n";
        String[] ranges = input.trim().split(",");
        for (int i = 0; i < ranges.length; i++) {
            String[] nums = ranges[i].trim().split("-");
            if (nums.length > 2) {
                logger.log(Level.WARNING, "Error occured while setting rows: range length longer than 2 (there are more than one dash)");
                return response;
            } else if (nums.length == 1) {
                int number = toInteger(nums[0]);
                if (number > 0) {
                    rows.add(number);
                } else {
                    logger.log(Level.WARNING, "Error occured while setting rows: number <= 0");
                    return response;
                }
            } else if (nums.length == 2) {
                int startRange = toInteger(nums[0]);
                int endRange = toInteger(nums[1]);
                if (startRange > 0 && endRange > 0 && startRange <= endRange) {
                    for (int j = startRange; j <= endRange; j++) {
                        rows.add(j);
                    }
                } else {
                    logger.log(Level.WARNING, "Error occured while setting rows: startRange or endRange is invalid");
                    return response;
                }
            } else {
                logger.log(Level.WARNING, "Error occured while setting rows: range is  less than one");
                return response;
            }
        }
        // Removing duplicate rows and sort it
        Set<Integer> cleandata = new HashSet<>();
        cleandata.addAll(rows);
        rows.clear();
        rows.addAll(cleandata);
        Collections.sort(rows);

        return null;
    }

    private int toInteger(String input) {
        String formattedInput = input.trim();
        try {
            return Integer.parseInt(formattedInput);
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Typed row number is not an integer.");
            return -1;
        }
    }


    public ArrayList<Integer> getRows() {
        return rows;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getSheetID() {
        return getGoogleId(sheetHtml);
    }

    public String getDocID() {
        return getGoogleId(docHtml);
    }

    public String getSheetHtml() {
        return sheetHtml;
    }

    public void setSheetHtml(String sheetHtml) {
        this.sheetHtml = sheetHtml;
    }

    public String getDocHtml() {
        return docHtml;
    }

    public void setDocHtml(String docHtml) {
        this.docHtml = docHtml;
    }

    public String getTargetFolderId() {
        return targetFolderId;
    }

    public void setTargetFolderId(String targetFolderInput) {
        this.targetFolderId = getGoogleFolderId(targetFolderInput);
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        logger.log(Level.INFO, "Date has been set to {0}", date);
        this.date = date;
    }

    public boolean isPDFNeeded() {
        return isPDFNeeded;
    }

    public void setPDFNeeded(boolean pdfNeeded) {
        isPDFNeeded = pdfNeeded;
    }

    public boolean isAutomatGeneration() {
        return isAutomatGeneration;
    }

    public void setAutomatGeneration(boolean automatGeneration) {
        isAutomatGeneration = automatGeneration;
    }

    public boolean isReplaceImage() {
        return replaceImage;
    }

    public void setReplaceImage(boolean replaceImage) {
        this.replaceImage = replaceImage;
    }

    public static Preferences getPreferences() {
        return preferences;
    }

}
