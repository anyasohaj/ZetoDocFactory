module com.bagirapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires com.google.api.client.auth;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;
    requires google.api.client;
    requires com.google.api.client;
    requires com.google.api.client.json.jackson2;
    requires google.api.services.sheets.v4.rev610;
    requires google.api.services.docs.v1.rev52;
    requires jdk.httpserver;
    requires google.api.services.drive.v3.rev195;
    requires com.google.common;
    requires com.fasterxml.jackson.core;
    requires java.logging;

    opens com.bagirapp to javafx.fxml;
    exports com.bagirapp;
}