package com.bagirapp.nonconfomance;

import com.bagirapp.Part;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Item {
    private Logger logger = Logger.getLogger(Part.class.getName());
    private int rowNumberInSpreadSheet;
    private Map<Columns, String> itemData;

    public Item(){
        this.itemData = new HashMap<>();
    }

    public void setRowNumberInSpreadSheet(int rowNumber){
        this.rowNumberInSpreadSheet = rowNumber;
    }

    public void addData(Columns key, String data){
        itemData.put(key, data);
    }

    public String getData(Columns columnName){
        return itemData.get(columnName);
    }

    public boolean hasDataFor(Columns column){
        boolean hasData = true;
        if (!this.itemData.containsKey(column)){
            hasData = false;
        }else if (this.itemData.get(column).isBlank()){
            hasData = false;
        }
        return hasData;
    }
}
