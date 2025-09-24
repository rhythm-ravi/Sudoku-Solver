package com.dooku;


import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;


public class MainScene extends VBox {      // Is the actual main display scene graph after the dynamic elements have been added    

    private final int dim;
    boolean isLocked;   // App locks user from editing grid any further after grid has been solved

    public MainScene(int dim) throws IOException{             // Display constructor with dynamic size of grid

        this.dim = dim;
        this.isLocked = false;
        
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
            // styling
            this.getStyleClass().add("tile");
    
        }
    }
}

