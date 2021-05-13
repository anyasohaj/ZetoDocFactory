package com.bagirapp.nonconfomance;

import com.bagirapp.DriveConnection;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportForm {
    private Logger logger;

    public static final String REPORTFORM_SHEETID = "1XfkktM4ZEnnnbZYPfcslhaLRJlEAJGBXdpez-aUs0oA";
    private static final String SHEET_NAME = "NC Report";
    private static final String RANGE = "A1:F32";

    private DriveConnection connetction;
    private Sheets sheetservice;
    private Item item;
    private String fileId;

    public ReportForm(Item item, String fileID) {
        logger = Logger.getLogger(ReportForm.class.getName());
        this.connetction = new DriveConnection(DriveConnection.SHEET);
        try {
            this.sheetservice = (Sheets) connetction.getService();
        } catch (IOException | GeneralSecurityException e) {
            logger.log(Level.SEVERE, " An error occured while creating connection with sheet... ");
        }
        this.item = item;
        this.fileId = fileID;
    }

    public void addCellValue(String data) throws IOException {
        List<List<Object>> singleData = new ArrayList<>();
        List<Object> singleCell = new ArrayList<>();
        singleCell.add(data);
        singleData.add(singleCell);
        ValueRange body = new ValueRange()
                .setValues(getContent());

        UpdateValuesResponse result =
                sheetservice.spreadsheets().values().update(fileId, SHEET_NAME + "!" + RANGE, body)
                        .setValueInputOption("RAW")
                        .execute();

        logger.log(Level.INFO, "{0} cells updated.", result.getUpdatedCells());
    }

    private List<List<Object>> getContent() {
        List<List<Object>> savedContent = new ArrayList<>();
        String range = SHEET_NAME + "!" + RANGE;
        ValueRange result;
        try {
            result = sheetservice.spreadsheets().values().get(fileId, range).execute();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Something went wrong with connecting to the spreadsheet. ");
            return savedContent;
        }

        if (result.getValues() == null) {
            logger.log(Level.WARNING, "Sheet content is null");
            return savedContent;
        }
        savedContent = result.getValues();

        int i = 0;
        for (List<Object> row : result.getValues()) {
            int j = 0;
            for (Object cell : row) {
                String cellValue = cell.toString().trim();

                for (Columns column : Columns.values()) {
                    if (cellValue.contains(column.getPlaceholder())) {
                        if (item.hasDataFor(column)) {
                            String newValue = cellValue.replace(column.getPlaceholder(), item.getData(column));
                            savedContent.get(i).set(j, newValue);
                        } else {
                            savedContent.get(i).set(j, cellValue.replace(column.getPlaceholder(), ""));
                        }
                    }
                }
                if (item.hasDataFor(Columns.DISPOSITION) && cellValue.contains(item.getData(Columns.DISPOSITION))) {
                        savedContent.get(i).set(j + 1, Boolean.TRUE);
                }

                if (item.hasDataFor(Columns.AFFECTED)) {
                    if (item.getData(Columns.AFFECTED).contains("Yes") && cellValue.equals("Product")) {
                            savedContent.get(i).set(j + 1, Boolean.TRUE);
                    } else if (item.getData(Columns.AFFECTED).contains("No") && cellValue.equals("Non-product")) {
                        savedContent.get(i).set(j + 1, Boolean.TRUE);
                    } else if ((item.getData(Columns.AFFECTED).contains("Both") && (cellValue.contains("Both")))) {
                        savedContent.get(i).set(j + 1, Boolean.TRUE);
                    }
                }

                if (cellValue.contains("FALSE")) {
                    savedContent.get(i).set(j, "");
                }
                j++;
            }
            i++;
        }
        return savedContent;
    }

}
