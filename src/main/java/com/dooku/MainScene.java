package com.dooku;


import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.Node;

import java.io.IOException;

import javafx.fxml.FXMLLoader;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;


public class MainScene extends VBox {      // Is the actual main display scene graph after the dynamic elements have been added
    // VBox is the generic template, Display is the constrained, formatted VBox element
    
    protected DoubleProperty minDimensionProperty = new SimpleDoubleProperty();
    private final int dim = SettingsController.getSetting("dimensions", Integer.class);
    public DoubleProperty minDimensionProperty() {
        return minDimensionProperty;
    }


    public MainScene() throws IOException{             // Display constructor with dynamic size of grid
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        loader.setRoot(this);           // The instance of MainScene gains those children, but only when the loader.load() is called, since that is when the fxml is parsed
        loader.load();

        this.heightProperty().addListener( (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue.doubleValue()-70 < this.getWidth())
                minDimensionProperty.set( ( newValue.doubleValue()- 60 - 30 -10 -4*dim )/(dim*dim) - 2);
            else 
                minDimensionProperty.set((this.getWidth()- 20 -10 -4*dim)/(dim*dim) - 2);
        });

        this.widthProperty().addListener( (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue.doubleValue() < this.getHeight()-70)
                minDimensionProperty.set( (newValue.doubleValue()- 20 -10 -4*dim )/(dim*dim) - 2);
            else 
                minDimensionProperty.set(( this.getHeight()- 60 - 30 -10 -4*dim )/(dim*dim) - 2);
        });
    }

}
