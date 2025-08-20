package com.dooku;


import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.Node;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.lang.Math;

public class MainScene extends VBox {      // Is the actual main display scene graph after the dynamic elements have been added    

    private DoubleProperty prefTileDimensionProperty = new SimpleDoubleProperty();     // Preferred for max visibility

    private final int dim;

    int heightOther;   
    int widthOther;  // difference between both insets is 70

    public MainScene(int dim) throws IOException{             // Display constructor with dynamic size of grid

        this.dim = dim;
        heightOther = 60+30+10+4*dim +20;   widthOther = 20+10+4*dim +20;

        this.heightProperty().addListener( (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            double pref = Math.min(newValue.doubleValue()-heightOther, this.getWidth()-widthOther)/(dim*dim);   // Min of grid_height and grid_width
            prefTileDimensionProperty.set(pref);
        });

        // We need to pass an object which may be used through the ChangeListener<T> functional interface. Since the object passed is to be used only through a functional interface, we do not care about its class, only that it has a changed() implementation
        this.widthProperty().addListener( (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            double pref = Math.min(newValue.doubleValue()-widthOther, this.getHeight()-heightOther)/(dim*dim);
            prefTileDimensionProperty.set(pref);
        });

        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/main.fxml"));
        loader.setRoot(this);           // The instance of MainScene gains those children, but only when the loader.load() is called, since that is when the fxml is parsed
        loader.load();
    }


    class Tile extends TextField {      // Tile for MainScende
        int row, col, srow, scol;   
        String lastEntered = new String();      // Initially the default empty string
    
        Tile(int row, int col, int srow, int scol) {            // Tile only requires information about the row, col, srow, scol lables attached to it
            this.row = row;
            this.col = col;
            this.srow = srow;
            this.scol = scol;
    
            this.setAlignment(Pos.CENTER);
    
    
            this.prefHeightProperty().bind(prefTileDimensionProperty);
            this.prefWidthProperty().bind(prefTileDimensionProperty);
            
            this.heightProperty().addListener( (ObservableValue<? extends Number> observable, Number oldValue, Number newValue ) -> {
                this.setFont(new Font(newValue.doubleValue()*0.4));
            });

            // Cool hovering mouse input
            this.setOnMouseEntered( e -> {
                this.requestFocus();
            });

            this.setOnMouseExited( e-> {
                this.getScene().getRoot().requestFocus();
            });


            // styling
            this.getStyleClass().add("tile");
    
        }
    }
}

