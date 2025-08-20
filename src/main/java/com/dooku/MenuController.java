package com.dooku;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.stage.*;
import javafx.scene.*;

public class MenuController {
    
    public static Stage stage;

    @FXML
    private void switchToMain() throws IOException {
        int dim = SettingsController.getSetting("dimensions", Integer.class);
        new MainScene(dim);
        
        // FXMLLoader.load(getClass().getResource("fxml/main.fxml"));
    }

    @FXML
    private void switchToSettings() throws IOException {

        // App.setRoot("settings");

        FXMLLoader.load(getClass().getResource("fxml/settings.fxml"));
    }


}
