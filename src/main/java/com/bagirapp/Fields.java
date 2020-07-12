package com.bagirapp;

import java.util.HashSet;
import java.util.Set;

public enum Fields {
    PARTNUMBER("Partnumber"),
    NAME("English name"),
    // For mechanical parts
    PROCESS("Process"),
    MATERIAL("Material"),
    COLOR("Color"),
    TOLERANCE("Tolerance"),
    RISK("Risk"),
    METHOD("Method"),
    INSPECTION("Receiving inspection"),
    MOUNTING("Mounting"),
    FILES("Files"),
    EXTENSION3D("EXTENSION3D"),
    DESCRIPTION("Description"),
    COMMENT("Comment"),
    //For electrical parts
    PURPOSE("Purpose"),
    ELECTRICAL("Electrical requirements"),
    OPTICAL("Optical requirements"),
    MECHANICAL("Mechanical requirements"),
    FIGURE("Mechanical dimensions"),
    PICTURE ("picture or drawing"),
    COMPONENTS("Accepted components"),
    MANUFACTURER("Manufacturer"),
    PTNUM("Part number"),
    DATASHEET("Datasheet link"),
    HANDLING("Special handling"),
    TYPE("Part type"),

    MODEL("3D model"),
    READY("Ready");

    private final String fieldText;
    public static String imagePlaceholder = "<" + FIGURE.fieldText.toUpperCase() + ">";


    Fields(String fieldText) {
        this.fieldText = fieldText;
    }

    public static Set<Fields> getMechanicalFields(){
        Set<Fields> mechanicalFields = new HashSet<Fields>();
        mechanicalFields.add(MECHANICAL);
        mechanicalFields.add(FIGURE);
        mechanicalFields.add(EXTENSION3D);
        mechanicalFields.add(COLOR);
        mechanicalFields.add(MATERIAL);
        mechanicalFields.add(PROCESS);
        mechanicalFields.add(TOLERANCE);
        mechanicalFields.add(MOUNTING);
        mechanicalFields.add(MODEL);
        return mechanicalFields;
    }

    public String getPlaceholderText() {
        if (this.equals(FIGURE)) {
            return "<" + this.toString() + ">";
        } else if (this.equals(EXTENSION3D)) {
            return "<" + MODEL.fieldText.toUpperCase() + ">";
        }
        return "<" + fieldText.toUpperCase() + ">";

    }

    public String getFieldText() {
        return fieldText;
    }

    public String getUppercaseFieldText() {
        return fieldText.toUpperCase();
    }

}
