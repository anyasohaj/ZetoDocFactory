package com.bagirapp.nonconfomance;

public enum Columns {
    NCID ("NC id", "Non conforming Report number:"),
    RECIEVED("NC received date", "Date:"),
    INICIATOR("NC iniciator", "Name person who noticed non-conformity:"),
    OWNER("NC owner", "NON"),
    SUMMARY("Summary of the problem", "Description of Non-Conformity:"),
    AREA("Affected area", "NON"),
    AFFECTED("Product affected", "A6:F6"),
    NAME("Product name", "Product name:"),
    RISK("Risk", "NON" ),
    ANALYZIS("Risk analysis report ID", "Risk analysis report ID:"),
    STATUS("Status", "NON"),
    DISPOSITION("Disposition", "B22:B28"),
    CORRECTION("Corrective action", "NON"),
    CORRECTIONDATE("Corrective Action Date", "NON"),
    COMPLETED("Corrective Action Date Completed", "NON"),
    DECISION("Final Decision", "Final decision:"),
    CLOSURE ("NCR Closure Date", "NON" ),
    COMMENT("Comment", "Comments");

    private final String fieldText;
    private final String placeInReportForm;
    Columns(String fieldText, String placeInReportForm){
        this.fieldText = fieldText;
        this.placeInReportForm = placeInReportForm;
    }

    public String getFieldText() {
        return fieldText;
    }
    public String getPlaceInReportForm(){return placeInReportForm;}
    public String getPlaceholder(){return "<"+fieldText+">";}
}
