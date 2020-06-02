package com.bagirapp;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.bagirapp.Fields.*;

public class GoogleDocument {
    private static final Logger logger = Logger.getLogger(GoogleDocument.class.getName());

    public static final int DOC_ID = 0;
    public static final int PDF_ID = 1;

    public Docs docService;
    private GoogleDrive googleDrive;
    private TrafficData trafficData;
    private DocScanner scanner;
    private Document createdDocument;
    private String createdDocId;
    public String revisedDate;
    public String effectiveDate;

    List<Request> replaceRequests = new ArrayList<>();
    List<Request> componentsRequest = new ArrayList<>();
    List<Request> indexRelatedRequests = new ArrayList<>();
    private String imageUrl;
    Request removeImageRequest = null;

    private String title;
    private Part part;


    private static final String RELATED_TO_ACCEPTED_COMPONENTS_TEXT = "review of manufacturers";
    private static final String DOWNLOAD_LINKTEXT = "here.";

    private static final String ESD = "Electrostatic discharge sensitive device. Observe precautions for handling.";
    private static final String FIRE_HAZARD = "Flammable material. Handle with precaution.  ";


    public GoogleDocument(Part part) {
        this.trafficData = TrafficData.getInstance();
        this.revisedDate = this.trafficData.getDate().toString();
        this.effectiveDate = this.trafficData.getDate().plusMonths(1).toString();
        this.part = part;
        this.title = "Zeto Part Specification " + part.getPartData().get(Fields.PARTNUMBER) + "-SPEC-R" + revisedDate + "-001-DRAFT";
        this.googleDrive = new GoogleDrive();
    }

    public String getTitle() {
        return title;
    }

    public String[] create() {
        String[] docIds = {"", ""};


        if (connectToService()) {
            this.createdDocId = initDoc();
            logger.log(Level.INFO, "CreatedDocId is {0}", createdDocId);

            searchForPlaceholders();
            replaceRequests.addAll(scanner.scanForTextModification(part));

           if (!indexRelatedRequests.isEmpty()) {
               sendRequests(indexRelatedRequests, createdDocId);
            }

            sendRequests(replaceRequests, createdDocId);
            createDatasheetLinks(scanner.scanForTextElement(DATASHEET.getFieldText()));
            createDatasheetLinks(scanner.scanForTextElement("Part number:"));

            if (!componentsRequest.isEmpty()) {
                sendRequests(componentsRequest, createdDocId);
            }

            if (trafficData.isPDFNeeded()) {
                docIds[PDF_ID] = googleDrive.createPDF(createdDocId, createdDocument.getTitle());
                logger.log(Level.FINE, "Pdf is successfully created.");
            }
        } else {
            return docIds;

        }
        docIds[DOC_ID] = this.createdDocId;
        return docIds;
    }


    private void handlePng(){
        if (part.hasDataFor(PICTURE)) {
            String imageId;
            if (part.getPartData().get(PICTURE).trim().equals(part.getPngFileName())) {
                imageId = googleDrive.uploadImage(trafficData.getImageFolder() + "\\" , part.getPngFileName());

            }else{
                logger.log(Level.WARNING, "The parts master list info about png file name is not the same as generated png file name! " +
                        "\\n According to Parts master list: " + part.getPartData().get(PICTURE).trim() +
                        "\\n\\t\\t generated file name: " + part.getPngFileName());

                imageId = googleDrive.uploadImage(trafficData.getImageFolder() + "\\" , part.getPartData().get(PICTURE));


                //TODO create confirmation dialog
                   /*Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                   alert.setTitle("Warning Dialog");
                   alert.setHeaderText("Imparity!");
                   alert.setContentText("The parts master list info about png file name is not the same as generated png file name! " +
                           "\\n According to Parts master list: " + part.getPartData().get(PICTURE).trim() +
                           "\\n\\t\\t generated file name: " + part.getPngFileName() +
                           "Are you sure that the image file name is identical with the spreadsheet info? "
                   );
                   Optional<ButtonType> result = alert.showAndWait();
                   if (result.get() == ButtonType.OK){
                       googleDrive.uploadImage(trafficData.getImageFolder() + "\\" + part.getPartData().get(PICTURE).trim());
                       imageUrl = getImageUrl();
                       logger.log(Level.INFO, "The url of the uploaded image is {0}", imageUrl);
                   } else {
                       // ... user chose CANCEL or closed the dialog
                   }*/
            }
            imageUrl = getImageUrl(imageId);
            logger.log(Level.INFO, "The url of the uploaded image is {0}", imageUrl);
            replaceRequests.add(replaceImageRequest(imageUrl));
        }
    }


    private void sendRequests(List<Request> requests, String docId) {
        Object[] message = {requests.size(), docId};
        logger.log(Level.INFO, "Sending {0} requests for {1} doc." , message);
        if (requests != null) {
            BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest();
            try {
                docService.documents().batchUpdate(docId, body.setRequests(requests)).execute();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Not managed send batchUpdate() request.");
            }
        } else {
            logger.log(Level.SEVERE, "NOT managed to send request, because requests list is null");
        }
        try {
            scanner = new DocScanner(docService.documents().get(createdDocId).execute());
        } catch (IOException e) {
            System.out.println("Something went wrong with connecting created document");
            e.printStackTrace();
        }
    }

    private void searchForPlaceholders(){
        for (Fields field : Fields.values()){
            if (part.hasDataFor(field)){
                if (field.equals(Fields.HANDLING)) {
                    if (part.getPartData().get(Fields.HANDLING).contains("ESD")) {
                        replaceRequests.add(replaceRequest(field.getPlaceholderText(), ESD));
                    } else if (part.getPartData().get(Fields.HANDLING).contains("Fire hazard")) {
                        replaceRequests.add(replaceRequest(field.getPlaceholderText(), FIRE_HAZARD));
                    } else if (part.getPartData().get(Fields.HANDLING).equals("-".trim())) {
                        part.getPartData().remove(Fields.HANDLING);
                       // replaceRequests.add(replaceRequest(HANDLING.getPlaceholderText(), ""));
                    }
                }else if (field.equals(EXTENSION3D)){
                        indexRelatedRequests.add(0, createLinkRequest(scanner.scanForRange(MODEL.getPlaceholderText()), MODEL.getPlaceholderText()));
                        replaceRequests.add(replaceRequest(MODEL.getPlaceholderText(), DOWNLOAD_LINKTEXT));
                    }else
                    if (field.equals(FIGURE)) {
                        replaceRequests.add(0, createLinkRequest(scanner.scanForRange(MODEL.getPlaceholderText()), part.getPartData().get(FIGURE)));
                        replaceRequests.add(replaceRequest(FIGURE.getPlaceholderText(), DOWNLOAD_LINKTEXT));
                    }

                    if (!part.getPartData().get(field).equals(Part.MULTILINE)) {
                        replaceRequests.add(replaceRequest(field.getPlaceholderText(), part.getPartData().get(field)));
                    } else {
                        replacePlaceholdersWithMultiLineData(field);
                    }
            } else{
                for (String paragraph : searchForRelatedParagraphs(field)){
                    replaceRequests.add(replaceRequest(paragraph, ""));
                }
            }
        }

        // handling image placeholder
        if (!trafficData.isReplaceImage()){
            logger.log(Level.INFO, "Removing image placeholder");
            StructuralElement imageParagraph = scanner.scanForStructuralElement("Note");
            Range imageRange = new Range().setStartIndex(imageParagraph.getStartIndex()).setEndIndex(imageParagraph.getEndIndex());
            indexRelatedRequests.add(  deleteRequest(imageRange));
        }else{
            handlePng();
        }

        replaceRequests.add(replaceRequest("YYYY-MM-DD", revisedDate));
        replaceRequests.add(replaceRequest("YYYY-NM-DD", effectiveDate));
    }

    private List<String> searchForRelatedParagraphs(Fields field){
        List<String> paragraphs = new ArrayList<>();
        String placeholderParagraph = "";
        if (!scanner.scanForTextElement(field.getPlaceholderText()).isEmpty()) {
           placeholderParagraph = scanner.scanForTextElement(field.getPlaceholderText()).getTextRun().getContent();
        }

        paragraphs.add(placeholderParagraph);

        switch (field){
            case ELECTRICAL:
            case OPTICAL: paragraphs.add(scanner.scanForTextElement(field.getFieldText()).getTextRun().getContent());
                break;
            case MECHANICAL: if (!part.getPartData().containsKey(Fields.EXTENSION3D)
                                    && !part.getPartData().get(Fields.TYPE).equals(Part.MECHANICAL)){
                                paragraphs.add(scanner.scanForTextElement(field.getFieldText()).getTextRun().getContent());
                    }
                break;
            case EXTENSION3D: paragraphs.add(scanner.scanForTextElement(MODEL.getPlaceholderText()).getTextRun().getContent());
                break;
            case COMPONENTS: paragraphs.add(scanner.scanForTextElement(field.getFieldText()).getTextRun().getContent());
                             paragraphs.add(scanner.scanForTextElement(RELATED_TO_ACCEPTED_COMPONENTS_TEXT).getTextRun().getContent());
        }
        List<String> relatedParagraphs = new ArrayList<>();
        for (String element: paragraphs){
            if (!element.isEmpty()){
                relatedParagraphs.add(element);
            }
        }
        return relatedParagraphs;
    }


    private void createDatasheetLinks(ParagraphElement paragraphElement) {
        Range componentRange = new Range().setStartIndex(paragraphElement.getStartIndex()).setEndIndex(paragraphElement.getEndIndex());
        if (paragraphElement.getTextRun().getContent().contains("Datasheet link")) {
            logger.log(Level.FINE, "Datasheet link text has been found");
            componentsRequest.add(createLinkRequest(componentRange, part.getPartData().get(Fields.DATASHEET)));
            componentsRequest.add(removeBulletRequest(componentRange));
        }else if (paragraphElement.getTextRun().getContent().contains("Part number: ")) {
            componentsRequest.add(removeBulletRequest(componentRange));
        }
    }

    private void manageContent(StructuralElement element, String text) {
        Range range = new Range().setStartIndex(element.getStartIndex()).setEndIndex(element.getEndIndex());

        for (Fields field : Fields.values()) {
            if (!isMatchingPlaceholder(field, text)) {
                continue;
            }

            if (part.hasDataFor(field)) {
                if (!part.getPartData().get(field).equals(Part.MULTILINE)) {
                    replacePlaceholderWithSingleData(text, field);
                } else {
                    replacePlaceholdersWithMultiLineData(field);
                }
            } else {
                if (text.contains(imagePlaceholder)) {
                    if (!part.hasDataFor(PICTURE)){
                    removeImageRequest = deleteRequest(range);
                    }
                } else {
                    replaceRequests.add(replaceRequest(text, ""));
                }
            }
        }
    }

    //Replace certain areas of the document with constant Strings according to the type of the Part


    private void replacePlaceholderWithSingleData(String text, Fields field) {
        if (text.contains(imagePlaceholder) ) {
           handlePng();
        } else  {
            if (field.equals(Fields.HANDLING)) {
                if (part.getPartData().get(Fields.HANDLING).contains("ESD")) {
                    replaceRequests.add(replaceRequest(field.getPlaceholderText(), ESD));
                } else if (part.getPartData().get(Fields.HANDLING).contains("Fire hazard")) {
                    replaceRequests.add(replaceRequest(field.getPlaceholderText(), FIRE_HAZARD));
                } else if (part.getPartData().get(Fields.HANDLING).equals("-".trim())) {
                    part.getPartData().remove(Fields.HANDLING);
                    replaceRequests.add(replaceRequest(text, ""));
                }
            }
            if (!(field.equals(Fields.EXTENSION3D) || field.equals(Fields.FIGURE))) {
                replaceRequests.add(replaceRequest(field.getPlaceholderText(), DOWNLOAD_LINKTEXT));
            } else {
                replaceRequests.add(replaceRequest(field.getPlaceholderText(), part.getPartData().get(field)));
            }

        }
    }

    private void replacePlaceholdersWithMultiLineData(Fields field) {
        StringBuilder data = new StringBuilder("");
        for (int i = 0; i < part.getData(field).size(); i++) {
            if (i == part.getData(field).size() - 1) {
                data.append(part.getData(field).get(i));
            } else {
                data.append(part.getData(field).get(i));
                data.append("\n");
            }
        }
        replaceRequests.add(replaceRequest(field.getPlaceholderText(), data.toString()));
    }

    private boolean isMatchingPlaceholder(Fields field, String text) {
        return text.contains(field.getPlaceholderText())
                || (field.equals(Fields.FIGURE) && text.contains(imagePlaceholder))
                || (field.equals(Fields.OPTICAL) && text.contains(field.getFieldText()))
                || (field.equals(Fields.MECHANICAL) && text.contains(field.getFieldText()) && !part.getPartData().containsKey(Fields.EXTENSION3D) && !part.getPartData().get(Fields.TYPE).equals(Part.MECHANICAL))
                || (field.equals(Fields.ELECTRICAL) && text.contains(field.getFieldText())
                || (field.equals(Fields.EXTENSION3D) && text.contains(Fields.MODEL.getPlaceholderText()))
                || (field.equals(Fields.COMPONENTS)) && text.contains(field.getFieldText())
                || (field.equals(Fields.COMPONENTS)) && text.contains(RELATED_TO_ACCEPTED_COMPONENTS_TEXT));
    }


    public static Request replaceRequest(String ph, String newValue) {
        return new Request()
                .setReplaceAllText(new ReplaceAllTextRequest()
                        .setContainsText(new SubstringMatchCriteria()
                                .setText(ph)
                                .setMatchCase(true))
                        .setReplaceText(newValue));
    }

    public static Request deleteRequest(Range rangeForDelete) {

        return new Request()
                .setDeleteContentRange(new DeleteContentRangeRequest()
                        .setRange(rangeForDelete));
    }

    public static Request createLinkRequest(Range range, String uri) {

        return new Request()
                .setUpdateTextStyle(new UpdateTextStyleRequest()
                        .setTextStyle(new TextStyle()
                                .setLink(new Link()
                                        .setUrl(uri)))
                        .setFields("link")
                        .setRange(range));
    }

    public static Request removeBulletRequest(Range range) {
        return new Request().setDeleteParagraphBullets(
                new DeleteParagraphBulletsRequest()
                        .setRange(new Range()
                                .setStartIndex(range.getStartIndex())
                                .setEndIndex(range.getEndIndex())));

    }

    private Request replaceImageRequest(String url) {
        String objectId = null;
        for (String key : createdDocument.getInlineObjects().keySet()) {
            objectId = createdDocument.getInlineObjects().get(key).getObjectId();
        }
        return new Request().setReplaceImage(new ReplaceImageRequest()
                .setImageObjectId(objectId)
                .setUri(url));
    }

    public String initDoc() {
        File newFile = googleDrive.copySample(this.title);

        String sampleId = new String();
        if (!newFile.isEmpty()) {

            sampleId = newFile.getId();

            try {
                this.createdDocument = docService.documents().get(sampleId).execute();

            } catch (IOException e) {
                logger.log(Level.SEVERE, "NOT managed to open new document with id: {0}", sampleId);
            }
        }
        scanner  = new DocScanner(createdDocument);
        return sampleId;
    }


    private boolean connectToService() {
        DriveConnection connection = new DriveConnection(DriveConnection.DOC);
        try {
            this.docService = (Docs) connection.getService();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error occured while creating docService (IOException)");
            return false;
        } catch (GeneralSecurityException e) {
            logger.log(Level.SEVERE, "Error occured while creating docService (GeneralSecurityException)");
            return false;
        }
        return true;
    }

    public String getImageUrl(String docId) {

        Document tryDoc = new Document();
        try {
            tryDoc = docService.documents().get(docId).execute();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Not managed to open the image-document");
        }

        String url = "";
        for (Object object : tryDoc.getInlineObjects().keySet()) {
            url = tryDoc.getInlineObjects().get(object).getInlineObjectProperties().getEmbeddedObject().getImageProperties().getContentUri();
            System.out.println("URL " + url);
        }
        return url;
    }


}
