package com.bagirapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Part {
    public static final String MULTILINE = "multiLine";
    public static final String ELECTRICAL = "Electrical";
    public static final String MECHANICAL = "Mechanical";
    public static final String RAW_MATERIAL = "Raw Material";
    public static final String CONSUMABLE = "Consumable";
    public static final String UNKNOWN = "Unknown";

    private Logger logger = Logger.getLogger(Part.class.getName());
    private int rowNumberInSpreadSheet;
    private Map<Fields, String> partData;
    private Map<Fields, ArrayList<String>> partDataMultiLine;


    public Part() {
        partData = new HashMap<>();
        partDataMultiLine = new HashMap<>();
    }

    public int getRowNumberInSpreadSheet() {
        return rowNumberInSpreadSheet;
    }

    public void setRowNumberInSpreadSheet(int rowNumberInSpreadSheet) {
        this.rowNumberInSpreadSheet = rowNumberInSpreadSheet;
    }

    public String getPng(){
        return "https://sites.google.com/a/zetoinc.com/janos-startpage/elektromos-adatlapok/" + this.getPngFileName().replace("\'", "") + "?attredirects=0";
    }

    public String getGeneralFileName() {
        return partData.get(Fields.PARTNUMBER) + "_" + partData.get(Fields.NAME).toLowerCase().replace(" ", "_");
    }

    public ArrayList<String> getData(Fields field){
        ArrayList<String> singleData = new ArrayList<>();
        if (!partData.containsKey(field) || partData.get(field).isBlank()){
          //Nothing should be happened
        }else if (partData.get(field).equals(MULTILINE)) {
            return partDataMultiLine.get(field);
        }else{
            singleData.add(partData.get(field));
        }
        return singleData;
    }

    public String getPngFileName() {
        return getGeneralFileName() + ".png";
    }
    public String get3DFileName() {
        if (partData.get(Fields.EXTENSION3D) != null){
            return getGeneralFileName() + "." + partData.get(Fields.EXTENSION3D);

        }
        return getGeneralFileName() + ".stp";}

    public Map<Fields, String> getPartData() {

        return new HashMap<>(partData);
    }

    public void addPartData(Fields key, String data) {
        String[] multilineData = null;
        if (data.contains("$$")){
            multilineData = data.split("\\$\\$");
            addPartData(key, multilineData);
            data = MULTILINE;
        }
        if (key.equals(Fields.COMPONENTS) && !data.equals(MULTILINE)){
            data = separateManufaturData(data);
        }
        this.partData.put(key, data);
    }

    private void addPartData(Fields key, String[] data){

        ArrayList<String> dataList = new ArrayList<>();
        for (int i = 0; i < data.length; i++){
            dataList.add( data[i].trim());
        }
        if (key.equals(Fields.COMPONENTS)){
            for (int i = 0; i < dataList.size(); i++){
                dataList.set(i, separateManufaturData(dataList.get(i)));
            }
        }
        this.partDataMultiLine.put(key, dataList);

    }

    private String separateManufaturData(String manuData) {

        String[] manuRow = manuData.split(",");
        if (manuRow.length == 3) {
            if (partData.containsKey(Fields.MANUFACTURER) && partData.containsKey(Fields.PTNUM) && partData.containsKey(Fields.DATASHEET)) {
                // Third and up to row, adding additioanl values to the arrayList of the key
                if (partDataMultiLine.containsKey(Fields.MANUFACTURER)) {
                    partDataMultiLine.get(Fields.MANUFACTURER).add(manuRow[0]);
                    partDataMultiLine.get(Fields.PTNUM).add(manuRow[1]);
                    partDataMultiLine.get(Fields.DATASHEET).add(manuRow[2]);
                } else {
                    //Second COMPONENTS row, create key in partDataMultiline and put the single data from partData in it.ú and replace partData value to MULTILINE
                    ArrayList<String> dataList = new ArrayList<>();
                    dataList.add(partData.put(Fields.MANUFACTURER, MULTILINE));
                    dataList.add(manuRow[0]);
                    partDataMultiLine.put(Fields.MANUFACTURER, dataList);
                    dataList = new ArrayList<>();
                    dataList.add(partData.put(Fields.PTNUM, MULTILINE));
                    dataList.add(manuRow[1]);
                    partDataMultiLine.put(Fields.PTNUM, dataList);
                    dataList = new ArrayList<>();
                    dataList.add(partData.put(Fields.DATASHEET, MULTILINE));
                    dataList.add(manuRow[2]);
                    partDataMultiLine.put(Fields.DATASHEET, dataList);
                }
            } else {
                // First COMPONENTS row, create key in partData
                partData.put(Fields.MANUFACTURER, manuRow[0]);
                partData.put(Fields.PTNUM, manuRow[1]);
                partData.put(Fields.DATASHEET, manuRow[2]);
            }
        }else{
           logger.log(Level.WARNING, "Something wrong with manufacturer data");
        }

        String[] component = manuData.split(",");
        manuData = "Manufacturer: " + component[0] + "\n";
        manuData += "\tPart number: " + component[1] + "\n";
        manuData += "\t" + component[2];
        return manuData;
    }




    public ArrayList<String> getTextComponents() {
        ArrayList<String> sepComp = new ArrayList<>();
        if (partDataMultiLine.get(Fields.MANUFACTURER) != null && partDataMultiLine.get(Fields.PTNUM) != null ){
            for (int i = 0; i < partDataMultiLine.get(Fields.MANUFACTURER).size(); i++){
                sepComp.add(partDataMultiLine.get(Fields.MANUFACTURER).get(i) + ", " + partDataMultiLine.get(Fields.PTNUM).get(i) + ", datasheet link " ) ;
            }
        }else if (partData.containsKey(Fields.MANUFACTURER) && partData.containsKey(Fields.PTNUM)) {
            sepComp.add(partData.get(Fields.MANUFACTURER) + ", " + partData.get(Fields.PTNUM) + ", datasheet link ");
        }
        return sepComp;
    }

    public boolean hasDataFor(Fields field){
        boolean hasData = true;
        if (!this.partData.containsKey(field)){
            hasData = false;
        }else if (this.partData.get(field).isBlank()){
            hasData = false;
        }
        return hasData;

    }

}
