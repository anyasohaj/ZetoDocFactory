package com.bagirapp;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpreadSheet {
    private Logger logger = Logger.getLogger(SpreadSheet.class.getName());
    private TrafficData trafficData;
    private DriveConnection connetction;
    private Sheets sheetservice;

    private final String sheetID;
    private static final int TITLE_ROW = 3;
    private Map<Fields, Integer> columnIndecies;

    public SpreadSheet() {
        trafficData = TrafficData.getInstance();
        this.connetction = new DriveConnection(DriveConnection.SHEET);
        try {
            this.sheetservice = (Sheets) connetction.getService();
        } catch (IOException | GeneralSecurityException e) {
            logger.log(Level.SEVERE, " An error occured while creating connection with sheet... ");
        }
        this.sheetID = trafficData.getSheetID();
        this.columnIndecies = new EnumMap<>(Fields.class);
        initColumnIndecies();
    }

    public ArrayList<Part> getParts() {
        ArrayList<Part> parts = new ArrayList<>();
        List<String> valueRanges = createValueRanges(trafficData.getRows());
        BatchGetValuesResponse result = getResponse(valueRanges);
        if (result.isEmpty()){
            logger.log(Level.WARNING, "It returns empty part list, because BatchGetValuesResponse is empty ");
            return parts;
        }

        for (int i = 0; i < valueRanges.size(); i++) {
            Part part = new Part();
            part.setRowNumberInSpreadSheet(trafficData.getRows().get(i));
            List<List<Object>> rowArray = result.getValueRanges().get(i).getValues();
            if (rowArray.size() <= 1) {
                part = retrievePartFromRow(rowArray);
            }else{
                logger.log(Level.WARNING, "The rowArray variable is NOT a single row. Returns an empty part.");
            }
            parts.add(part);
        }
        return parts;
    }

    private BatchGetValuesResponse  getResponse(List<String> valueRanges){
        BatchGetValuesResponse response = new BatchGetValuesResponse();
        try {
            response =  sheetservice.spreadsheets().values().batchGet(sheetID).setRanges(valueRanges).execute();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Something went wrong with batchGetValuesResponse");
        }
        return response;
    }

    private Part retrievePartFromRow(List<List<Object>> rowArray) {
        Part part = new Part();
        for (int j = 0; j < rowArray.get(0).size(); j++) {
            String string = (String) rowArray.get(0).get(j);
            if (string.isBlank()) {
                continue;
            }
            for (Map.Entry<Fields, Integer> entry : columnIndecies.entrySet()) {
                if (entry.getValue().equals(j)) {
                    part.addPartData(entry.getKey(), string);
                }

            }
        }
        if (!part.hasDataFor(Fields.TYPE)){
            part.addPartData(Fields.TYPE, Part.UNKNOWN);
        }

        if (!part.hasDataFor(Fields.PURPOSE)){
            part.addPartData(Fields.PURPOSE, withArticle(part.getPartData().get(Fields.NAME)));
        }

        return part;
    }

    private List<String> createValueRanges(ArrayList<Integer> rowNumbers) {
        List<String> ranges = new ArrayList<>();
        for (int rowIndex : rowNumbers) {
            String range = (trafficData.getSheetName() + "!A" + (rowIndex) + ":Z" + (rowIndex));
            ranges.add(range);
        }
        return ranges;
    }

    private void initColumnIndecies() {
        String titleRow = trafficData.getSheetName() + "!" + "A" + TITLE_ROW + ":Z" + TITLE_ROW;
        ValueRange result;
        try {
            result = sheetservice.spreadsheets().values().get(sheetID, titleRow).execute();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Something went wrong with connecting to the spreadsheet. ");
            return;
        }

        if (result.getValues().get(0) == null) {
            logger.log(Level.WARNING, "Title row is null");
            return;
        }
        int i = 0;
        for (Object cell : result.getValues().get(0)) {
            for (Fields field : Fields.values()) {
                if (cell == null) {
                    continue;
                }
                String cellValue = cell.toString().toUpperCase().trim();
                getColumnIndexOfField(field, cellValue, i);
            }
            i++;
        }
        for (Map.Entry index : columnIndecies.entrySet()){
            System.out.println("Key: " + index.getKey() + " value: " + index.getValue());
        }
    }

    private void getColumnIndexOfField(Fields field, String cellValue, int i){
        if ((cellValue.contains(field.toString()) && !field.equals(Fields.NAME)) || cellValue.equals(field.getUppercaseFieldText())) {

            if (field.equals(Fields.INSPECTION)) {
                columnIndecies.put(Fields.METHOD, i);
            } else {
                columnIndecies.put(field, i);
            }
        } else if ((field.equals(Fields.PARTNUMBER) && cellValue.contains("FINAL PART")
                || (field.equals(Fields.FIGURE) && (cellValue.contains("PICTURE"))))) {
            columnIndecies.put(field, i);
            Object[] msg = {field.toString(), i};
            logger.log(Level.INFO, "Column {0} index {1}", msg);
        }
    }

    public void addCellValue(String data, int row) throws IOException {
        List<List<Object>> singleData = new ArrayList<>();
        List<Object> singleCell = new ArrayList<>();
        singleCell.add(data);
        singleData.add(singleCell);
        ValueRange body = new ValueRange()
                .setValues(singleData);
        UpdateValuesResponse result =
                sheetservice.spreadsheets().values().update(sheetID, "Parts master list!" + "AA" + row + ":" + "AA" + row, body)
                        .setValueInputOption("RAW")
                        .execute();

        logger.log(Level.INFO, "{0} cells updated.", result.getUpdatedCells());
    }

    private String withArticle(String text){
        if ((text.toLowerCase().charAt(0) == 'a') || (text.toLowerCase().charAt(0) == 'e') || (text.toLowerCase().charAt(0) == 'i') || (text.toLowerCase().charAt(0) == 'o') || (text.toLowerCase().charAt(0) == 'u')){
            return "an " + text;
        }else{
            return "a " + text;
        }
    }
}
