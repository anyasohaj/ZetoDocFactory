package com.bagirapp;

import com.google.api.services.docs.v1.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.bagirapp.Fields.FIGURE;
import static com.bagirapp.Fields.imagePlaceholder;
import static com.bagirapp.GoogleDocument.createLinkRequest;
import static com.bagirapp.GoogleDocument.replaceRequest;

public class DocScanner {
    private static final String PLACEHOLDER_DIMENSION = "Part dimensions must agree with the dimensions";
    private static final String PLACEHOLDER_COLOR = "Color must agree with the specified color ";
    public static final String ELECTRICAL = "100% functional inspection of electronic and electrical components is performed at the board level when that board is tested onto which the component is soldered. Therefore only supplier labelling / documentation shall be verified at incoming inspection to verify vendor name and vendor part number listed as per the accepted parts of the particular component.\n";
    private static final String RAW_MATERIAL = "100% functional inspection of raw materials is performed at the subassembly level when that subassembly is tested in which the raw material is built-in. Therefore only supplier labelling / documentation shall be verified at incoming inspection to verify vendor name and vendor part number listed as per the accepted parts of the particular component.\n";
    private static final String CONSUMABLES = "Consumable used during workmanship. Supplier label and documentation should be verified at incoming inspection.   \n";

    private Logger logger = Logger.getLogger(DocScanner.class.getName());
    private Document document;


    public DocScanner(Document document) {
        this.document = document;
    }

    public Range scanForRange(String placeholder) {
        Range range = new Range();

        ParagraphElement paragraphElement = scanForTextElement(placeholder);

        if (paragraphElement.getTextRun().getContent().contains(placeholder)) {
            int hereIndex = paragraphElement.getStartIndex() + paragraphElement.getTextRun().getContent().indexOf(placeholder);
            range.setStartIndex(hereIndex).setEndIndex(hereIndex + placeholder.length());
        }
        return range;
    }

    public ParagraphElement scanForTextElement(String placeholder) {

        for (StructuralElement element : document.getBody().getContent()) {
            if (element.getParagraph() == null) {
                continue;
            }
            for (ParagraphElement paragraphElement : element.getParagraph().getElements()) {
                if (paragraphElement.getTextRun() == null) {
                    continue;
                }

                if (paragraphElement.getTextRun().getContent().isBlank()) {
                    continue;
                }

                if (paragraphElement.getTextRun().getContent().contains(placeholder)) {
                    return paragraphElement;
                }
            }
        }
        return new ParagraphElement();
    }

    public StructuralElement scanForStructuralElement(String placeholder){
        for (StructuralElement element : document.getBody().getContent()) {
            if (element.getParagraph() == null) {
                continue;
            }
            for (ParagraphElement paragraphElement : element.getParagraph().getElements()) {
                if (paragraphElement.getTextRun() == null) {
                    continue;
                }

                if (paragraphElement.getTextRun().getContent().isBlank()) {
                    continue;
                }

                if (paragraphElement.getTextRun().getContent().contains(placeholder)) {
                    return element;
                }
            }
        }
        return new StructuralElement();
    }

    public Range getImageRange() {
        int startIndex = scanForTextElement(imagePlaceholder).getStartIndex();
        System.out.println("A text: " + scanForTextElement(FIGURE.getPlaceholderText()).getTextRun().getContent());
        int endIndex = scanForTextElement(FIGURE.getPlaceholderText()).getEndIndex();
        System.out.println("Startindex: " + startIndex + " endindex: " + endIndex);
        return new Range().setStartIndex(startIndex).setEndIndex(endIndex);
    }

    public List<Request> scanForTextModification(Part part){
        List<Request> replaceRequests = new ArrayList<>();
        if (part.getPartData().get(Fields.TYPE).equals(Part.UNKNOWN)) {
            logger.log(Level.WARNING, "Part type is not defined, so type text won't be replaced");
            return replaceRequests;  //returns empty list
        }

        String text = "";
        if (part.getPartData().get(Fields.TYPE).trim().equals(Part.ELECTRICAL)) {
            text = scanForTextElement(PLACEHOLDER_DIMENSION).getTextRun().getContent();
                replaceRequests.add(replaceRequest(text, ELECTRICAL));
            text = scanForTextElement(PLACEHOLDER_COLOR).getTextRun().getContent();
                replaceRequests.add(replaceRequest(text, ""));

        } else if (part.getPartData().get(Fields.TYPE).trim().equals(Part.RAW_MATERIAL)) {
            text = scanForTextElement(PLACEHOLDER_DIMENSION).getTextRun().getContent();
                replaceRequests.add(replaceRequest(text, RAW_MATERIAL));
            text = scanForTextElement(PLACEHOLDER_COLOR).getTextRun().getContent();
                replaceRequests.add(replaceRequest(text, ""));

        } else if (part.getPartData().get(Fields.TYPE).trim().equals(Part.CONSUMABLE)) {
            text = scanForTextElement(PLACEHOLDER_DIMENSION).getTextRun().getContent();
                replaceRequests.add(replaceRequest(text, CONSUMABLES));
            text = scanForTextElement(PLACEHOLDER_COLOR).getTextRun().getContent();
                replaceRequests.add(replaceRequest(text, ""));

        } else if (part.getPartData().get(Fields.TYPE).trim().equals(Part.MECHANICAL) && text.contains(imagePlaceholder)) {
            replaceRequests.add(replaceRequest(text, Fields.FIGURE.getFieldText()));
        }

        return replaceRequests;

    }


    public Request createLink(String placeHolder, String url) {
        return createLinkRequest(scanForRange(placeHolder), url);
    }

    public List<Request> replacePlaceholdersToData() {
        List<Request> replaceRequests = new ArrayList<>();
        return replaceRequests;
    }


}
