package com.dooku;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

import java.util.prefs.Preferences;
import java.util.prefs.AbstractPreferences; // May use this to clear the preferences set in the node for this package
import java.util.prefs.BackingStoreException;

import javafx.stage.*;
/**
 * JavaFX App
 */

public class App extends Application {          // Corresponds the actual javafx application
    private static Rectangle2D vScreen = Screen.getPrimary().getVisualBounds();
    static double screenWidth = vScreen.getWidth();
    static double screenHeight = vScreen.getHeight();       //******    TEST CODE   *************/

    private static Scene scene;
    final static Preferences prefs = Preferences.userNodeForPackage(App.class);

    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        // scene = new Scene(loadFXML("menu"), 440, 600);      //+200
        scene = new Scene(loadFXML("menu"), screenWidth/4, screenHeight/4);      // length * breadth ie width*height
        stage.setScene(scene);
        
        Alert alert = new Alert(AlertType.CONFIRMATION, "Would you like to quit the application?", ButtonType.YES, ButtonType.NO);

        alert.setOnHidden(e -> {
            if (alert.getResult()==ButtonType.NO) {
                stage.show();
            }
        });

        stage.setOnHidden(e -> alert.show());
        
        stage.setResizable(false);
        // stage.minWidthProperty().bind(scene.heightProperty());
        // stage.minHeightProperty().bind(scene.heightProperty());
        stage.show();       // Only when stage is hidden does this method terminate, as alert object would have been marked for garbage collection since the only reference to it is in the local variable poin

    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    static void setRoot(Parent root) {
        scene.setRoot(root);
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/"+fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();           // The main thread waits for launch to end before proceeding with further instructions
        System.out.println("Application has ended...");     
    }

}