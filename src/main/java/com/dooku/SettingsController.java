package com.dooku;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

import javafx.scene.text.*;
import javafx.scene.*;

import javafx.stage.*;

import java.util.Collection;
import java.util.HashMap;
public class SettingsController {
    
    private static Stage stage = new Stage();       // There shoyld be a single stage field however times the settings.fxml object graph is rendered

    @FXML
    private Parent root = null;
    @FXML
    private ComboBox dropDown1 = null;
    @FXML 
    private ComboBox dropDown2 = null;

    static class Choice<T> extends Text {
        T content;
        Choice(T content, String text) {
            this.content = content;
            this.setText(text);
        }
    }

    static class Choice2<T,U> extends Node {
        T content;
    }

    private static Choice[][] ddChoices = {        // Using Choice instead of object so that we have access to content field
        {new Choice<Object>(null,"--")},
        {new Choice<Integer>(2, "4 x 4"), new Choice<Integer>(3, "9 x 9"), new Choice<Integer>(4,"16 x 16"), new Choice<Integer>(5,"25 x 25") },
        {new Choice<String>("black","Black"), new Choice<String>("red","Red"), new Choice<String>("blue","Blue"), new Choice<String>("green","Green"), new Choice<String>("orange", "Orange")}

    };



    // The constructor is called first, then any @FXML annotated fields are populated, then initialize() is called.
    // This means the constructor does not have access to @FXML fields referring to components defined in the .fxml file, while initialize() does have access to them.
    // https://stackoverflow.com/questions/34785417/javafx-fxml-controller-constructor-vs-initialize-method
    @FXML
    public void initialize() {      // try private as wells
        // dropDown1.getItems().addAll(dropDownChoices[1]);
        // dropDown2.getItems().addAll(dropDownChoices[2]);
        dropDown1.getItems().addAll(ddChoices[1]);
        dropDown2.getItems().addAll(ddChoices[2]);

        stage.setScene(new Scene(root));
        stage.show();
    }

    private static HashMap<String, Object> optionMap = new HashMap<>();

    // public SettingsController() {       //maps the objects to their value to be stored in prefs
    static {
        for (Choice[] l : ddChoices) {
            for (Choice c : l) {
                optionMap.put(c.toString(), c.content);
            }
        }
    }

    @FXML
    private void switchToMenu() throws IOException {
        stage.hide();
    }

    @FXML
    private void applyChanges() {
        App.prefs.put("dimensions", dropDown1.getValue().toString());
        App.prefs.put("bgcolor", dropDown2.getValue().toString());

        System.out.println("Changes Applied");
    }


    public static <T> T getSetting(String key, Class<T> type) {        // key in the package's pregerence node
        Choice<Integer> default1 = ddChoices[1][1];
        Choice<String> default2 = ddChoices[2][0];

        String omapKey;
        if (key=="dimensions") {
            omapKey = App.prefs.get(key, default1.toString());
        }
        else if (key=="bgcolor") {
            omapKey = App.prefs.get(key, default2.toString());
        }
        else {
            return null;
        }
        return type.cast(optionMap.get(omapKey));
    }
}

