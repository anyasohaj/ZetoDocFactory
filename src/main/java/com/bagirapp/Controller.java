package com.bagirapp;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static com.bagirapp.TrafficData.*;

public class Controller {
    @FXML public GridPane mainGridPane;
    @FXML public TextField spreadsheetUrlTextField;
    @FXML public TextField godocUrlTextField;
    @FXML public TextField rowNumbers;
    @FXML public TextField sheetNameTextField;
    @FXML public CheckBox uploadImageCheckBox;
    @FXML public HBox uploadImageHBox;
    @FXML public TextField uploadImageTextField;
    @FXML public Button browseButton;
    @FXML public TextField targetFolderTextField;
    @FXML public CheckBox pdfCheckBox;
    @FXML public DatePicker datePicker;
    @FXML public TextArea response;
    @FXML public ProgressBar progressBar;
    @FXML public Button ok;
    @FXML public Button cancel;

    private TrafficData trafficData;
    private Thread taskThread;

    public void initialize() {
        trafficData = TrafficData.getInstance();
        pdfCheckBox.setSelected(false);
        datePicker.getEditor().setText(trafficData.getDate().format(DateTimeFormatter.ofPattern("YYYY. MM. dd")));
        spreadsheetUrlTextField.setText(trafficData.getSheetHtml());
        godocUrlTextField.setText(trafficData.getDocHtml());
        targetFolderTextField.setText(trafficData.getTargetFolderId());
        sheetNameTextField.setText(trafficData.getSheetName());
        uploadImageTextField.setText(trafficData.getImageFolder());
        progressBar.setVisible(false);
        cancel.setDisable(true);
        ok.setDefaultButton(true);
        uploadImageCheckBox.addEventHandler(EventType.ROOT, new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if (uploadImageCheckBox.isSelected()){
                    uploadImageCheckBox.setText("Upload images from computer.   Image folder:  ");
                    uploadImageHBox.setVisible(true);
                }else{
                    uploadImageCheckBox.setText("Upload images from computer.");
                    uploadImageHBox.setVisible(false);
                }
            }
        });
    }

    @FXML
    public void start() {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                progressBar.setVisible(true);

                task();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisible(false);
                        ok.setDisable(false);
                        cancel.setDisable(true);
                    }
                });
            }
        };
        taskThread = new Thread(runnable);
        taskThread.start();
    }

    @FXML
    private void task() {
        ok.setDisable(true);
        cancel.setDisable(false);
        //Set data according to the user input
        trafficData.getPreferences().put(SHEET_URL, spreadsheetUrlTextField.getText());
        trafficData.getPreferences().put(DOC_URL, godocUrlTextField.getText());
        trafficData.getPreferences().put(TARGET_FOLDER, targetFolderTextField.getText());
        trafficData.getPreferences().put(SHEET_NAME, sheetNameTextField.getText());
        trafficData.getPreferences().put(IMAGE_FOLDER, uploadImageTextField.getText());

        trafficData.setSheetHtml(spreadsheetUrlTextField.getText());
        trafficData.setDocHtml(godocUrlTextField.getText());
        if (datePicker.getValue() != null) {
            trafficData.setDate(datePicker.getValue());
        }
        trafficData.setSheetName(sheetNameTextField.getText());
        trafficData.setImageFolder(uploadImageTextField.getText());
        trafficData.setTargetFolderId(targetFolderTextField.getText());

      /* Creating PDFs from docs
        response.setText("PDF készítő üzemmód...\n");
        GoogleDrive drive = new GoogleDrive();

        response.appendText("Creating drive connection...\n");
        Map<String, String> docs = drive.findDocs();
        response.appendText("Got doc files\n");
        for (String id : docs.keySet()){
            System.out.println("Processing docs in for loop");
            String title = docs.get(id);
            System.out.println("Working on " + title);
            drive.createPDF(id, title );
        }
        response.appendText("Ready");*/

        String rowResponse = trafficData.setRows(rowNumbers.getText());
        if (rowResponse != null) {
            response.appendText(rowResponse);
            return;
        }
        trafficData.setPDFNeeded(pdfCheckBox.isSelected());

        SpreadSheet spreadSheet = new SpreadSheet();

        GoogleDocument document;
        ArrayList<Part> parts = spreadSheet.getParts();
        if (parts.isEmpty()){
            response.appendText("Not managed to retrieve Part from row(s) " + trafficData.getRows() + "\\n");
        }
        for (Part part : parts) {
            if (taskThread.isInterrupted()) {
                return;
            }
            document = new GoogleDocument(part);
            String[] newFileIds = document.create();
            if (!newFileIds[0].isBlank()) {
                response.appendText(document.getTitle() + " has been created.\n");
                String responseCell = "https://docs.google.com/document/d/" + newFileIds[GoogleDocument.DOC_ID] + "/export?format=pdf";
                if (!newFileIds[1].isBlank()) {
                    try {
                        spreadSheet.addCellValue(newFileIds[GoogleDocument.PDF_ID], part.getRowNumberInSpreadSheet());
                        response.appendText("\tPdf link: " + responseCell + "\n\n");
                    } catch (IOException e) {
                        response.appendText("NOT managed to save pdf link to Parts master list\n");
                    }
                } else {
                    response.appendText("\tPdf has not been created.\n");
                }
            } else {
                response.appendText("Can not manage to initialize new document.\n");
            }

        }
    }

    @FXML
    public void interruptProcess() {
        taskThread.interrupt();
        response.appendText("Process has been interrupted by the user. The program tries to stop it as far as possible.\n");
        cancel.setDisable(true);
    }

    @FXML public void browseImageFolder(){
        DirectoryChooser chooser = new DirectoryChooser();
        File fileLocation;
            chooser.setInitialDirectory(new File(trafficData.getImageFolder()));
            fileLocation = chooser.showDialog(mainGridPane.getScene().getWindow());
            if (fileLocation != null) {
                trafficData.setImageFolder(fileLocation.getPath());
                uploadImageTextField.setText(fileLocation.getPath());
            }
    }


}
