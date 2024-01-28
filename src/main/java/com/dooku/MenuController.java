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
  
        new MainScene();
        // FXMLLoader.load(getClass().getResource("main.fxml"));
    }

    @FXML
    private void switchToSettings() throws IOException {

        // App.setRoot("settings");

        //// App.setRoot(new Settings());

        FXMLLoader.load(getClass().getResource("settings.fxml"));
    }


}
