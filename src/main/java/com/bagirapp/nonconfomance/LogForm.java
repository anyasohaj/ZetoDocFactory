package com.bagirapp.nonconfomance;

import com.bagirapp.DriveConnection;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogForm {
    private Logger logger;

    private static final String LOGFORM_SHEETID = "1GKe8vQEHh5GA9tOUs6DQUEZDbNbLIBZAXdVmvADOE64";
    private static final String SHEET_NAME = "Nonconformance log";
    private static final int TITLE_ROW = 1;

    private DriveConnection connetction;
    private Sheets sheetservice;
    private Map<Columns, Integer> columnIndecies;
    private ArrayList<Integer> rowNumbers;



    public LogForm(String rowNumbersString){
        this.logger = Logger.getLogger(LogForm.class.getName());
        this.connetction = new DriveConnection(DriveConnection.SHEET);
        try {
            this.sheetservice = (Sheets) connetction.getService();
        } catch (IOException | GeneralSecurityException e) {
            logger.log(Level.SEVERE, " An error occured while creating connection with sheet... ");
        }
        this.columnIndecies = new EnumMap<>(Columns.class);
        initColumnIndecies();
        rowNumbers = new ArrayList<>();
        getRowNumbersFromString(rowNumbersString);
    }

    private String getRowNumbersFromString(String input) {
            rowNumbers.clear();
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
                        rowNumbers.add(number);
                    } else {
                        logger.log(Level.WARNING, "Error occured while setting rows: number <= 0");
                        return response;
                    }
                } else if (nums.length == 2) {
                    int startRange = toInteger(nums[0]);
                    int endRange = toInteger(nums[1]);
                    if (startRange > 0 && endRange > 0 && startRange <= endRange) {
                        for (int j = startRange; j <= endRange; j++) {
                            rowNumbers.add(j);
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
            cleandata.addAll(rowNumbers);
            rowNumbers.clear();
            rowNumbers.addAll(cleandata);
            Collections.sort(rowNumbers);

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

    public ArrayList<Item> getItems() {
        ArrayList<Item> items = new ArrayList<>();
        List<String> valueRanges = createValueRanges(getRows());
        BatchGetValuesResponse result = getResponse(valueRanges);
        if (result.isEmpty()) {
            logger.log(Level.WARNING, "It returns empty part list, because BatchGetValuesResponse is empty ");
            return items;
        }

        for (int i = 0; i < valueRanges.size(); i++) {
            Item item = new Item();
            item.setRowNumberInSpreadSheet(getRows().get(i));
            List<List<Object>> rowArray = result.getValueRanges().get(i).getValues();
            if (rowArray.size() <= 1) {
                item = retrieveItemFromRow(rowArray);
            } else {
                logger.log(Level.WARNING, "The rowArray variable is NOT a single row. Returns an empty part.");
            }
            items.add(item);
        }
        return items;
    }

    private Item retrieveItemFromRow(List<List<Object>> rowArray) {
        Item item = new Item();
        for (int j = 0; j < rowArray.get(0).size(); j++) {
            String string = (String) rowArray.get(0).get(j);
            if (string.isBlank()) {
                continue;
            }
            for (Map.Entry<Columns, Integer> entry : columnIndecies.entrySet()) {
                if (entry.getValue().equals(j)) {
                    logger.log(Level.INFO, string);
                    item.addData(entry.getKey(), string);
                }
            }
        }
        return item;
    }

    private BatchGetValuesResponse getResponse(List<String> valueRanges) {
        BatchGetValuesResponse response = new BatchGetValuesResponse();
        try {
            response = sheetservice.spreadsheets().values().batchGet(LOGFORM_SHEETID).setRanges(valueRanges).execute();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Something went wrong with batchGetValuesResponse");
        }
        return response;
    }

    private List<String> createValueRanges(ArrayList<Integer> rowNumbers) {
        List<String> ranges = new ArrayList<>();
        for (int rowIndex : rowNumbers) {
            String range = (SHEET_NAME + "!A" + (rowIndex) + ":Z" + (rowIndex));
            ranges.add(range);
        }
        return ranges;
    }

    private void initColumnIndecies(){
        String titleRow = SHEET_NAME + "!" + "A" + TITLE_ROW + ":Z" + TITLE_ROW;
        ValueRange result;
        try {
            result = sheetservice.spreadsheets().values().get(LOGFORM_SHEETID, titleRow).execute();
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
            for (Columns column : Columns.values()) {
                if (cell == null) {
                    continue;
                }
                String cellValue = cell.toString().trim();
                getColumnIndexOfField(column, cellValue, i);
            }
            i++;
        }
        for (Map.Entry index : columnIndecies.entrySet()) {
            System.out.println("Key: " + index.getKey() + " value: " + index.getValue());
        }
    }

    private void getColumnIndexOfField(Columns column, String cellValue, int i) {
        System.out.println(cellValue + " contra " + column + ".getFieldText() = " + column.getFieldText());
        if ((cellValue.equals(column.getFieldText().trim()) ) ) {
            System.out.println("there is an equality");
            columnIndecies.put(column, i);

        }
    }

    private ArrayList<Integer> getRows(){
        return rowNumbers;
    }
}
