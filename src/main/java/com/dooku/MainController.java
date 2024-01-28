package com.dooku;

import java.io.IOException;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import javafx.stage.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import javafx.scene.text.Font;
public class MainController {       // Loose coupling between UI(client) and logic(server)

    private static Stage stage = new Stage();
    
    @FXML
    private MainScene root = null;
    @FXML
    private GridPane board = null;
    @FXML
    private HBox options = null;
    private final int dim = SettingsController.getSetting("dimensions", Integer.class);
    // if a new instance is created for each time a scene graph from main.fxml is constructed, then this final modifier should be harmless
    private final String color = SettingsController.getSetting("bgcolor", String.class);
    private Board lBoard = new Board(dim);   //// incorrrectded

    private static Alert closingAlert = new Alert(AlertType.CONFIRMATION);
    static {
        closingAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        closingAlert.setContentText("Go back to Menu?");
        
        stage.setOnHidden(e -> closingAlert.show());        
        // For some reason alert.showAndWait() created all sorts of problems (MainScene being visible but unresponsive if we decided to stay after the alert pops up,  stage simply not being visible if we pressed NO and never showing until the entire application was restarted and so on...)
        closingAlert.setOnHidden(e -> {     // My best guess is that since in showAndWait it waits for the stage to be closed before returning to caller,  when alert was hidden, control could be transferred to caller, or to alert's hidden handler,  and this might have led to some kind of obfuscation  (Maybe control went from stage onHidden event handler to the alert stage being shown and wait,  to the alert's on hidden as soon as it was closed,  thus stage is shown,  then control back to stage's onHidden event handler?????)
            if (closingAlert.getResult() == ButtonType.NO)
                stage.show();
        });
    }

    @FXML
    private void initialize() throws IOException{

        double hOther = 40 + 30;    // spacing (40)  +  30 for the buttons set min height (a rough estimate based on how they need to look w/ respect to current insets and whatnot) 
        
        root.setMinHeight(App.screenHeight/2);      root.setMinWidth(App.screenHeight/2-hOther);       // Min we go for these dimensions; Currently setting the minHeight to ScreenH/2 as that seems about right    (minWidth is width of grid)
        root.setMaxHeight(App.screenHeight);        root.setMaxWidth(App.screenHeight - hOther);
        stage.setMinHeight(root.getMinHeight()*1.1);        stage.setMinWidth(root.getMinWidth()*1.1);

        VBox.setVgrow(options, Priority.NEVER);     
        addTiles();

        //double min = (App.screenHeight>App.screenWidth ? App.screenWidth : App.screenHeight);
        Scene scene = new Scene(root, App.screenHeight/2, App.screenHeight/2-hOther);//, 0.75*min/10*dim*dim, min/10*dim*dim+50);
        stage.setScene(scene);
        stage.setTitle("SUDOKU SOLVER");

        stage.show();
    }

    @FXML
    private void switchToMenu() throws IOException {
        stage.hide();
    }

    
    private Tile[][][][] tiles = new Tile[dim][dim][dim][dim];

    private void addTiles() {
        for (int i=0; i<dim; i++) {
            for (int j=0; j<dim; j++) {
                GridPane subGrid = new GridPane();
                subGrid.setStyle("-fx-border-width: 2; -fx-border-style: solid; -fx-border-color: "+color);
                for (int k=0; k<dim; k++)
                    for (int l=0; l<dim; l++) {
                        Tile tile = new Tile(i,j,k,l);
                        subGrid.add(tile, k, l);
                        tiles[i][j][k][l] = tile;
                    }
                board.add(subGrid, i, j);
            }
        }
    }

    @FXML
    private void solve() {  
        lBoard.solve();
        // while(!lBoard.observableState.isEmpty()) {
        //     int[] trace = lBoard.observableState.pollFirst();
        //     ( (Tile) ((GridPane) board.getChildren().get(dim*trace[0]+trace[1])).getChildren().get(dim*trace[2]+trace[3]) ).setText(""+trace[4]);
        // }
        for (int i=0; i<dim; i++) {
            for (int j=0; j<dim; j++) {
                for (int k=0; k<dim; k++){
                    for (int l=0; l<dim; l++) {
                        tiles[i][j][k][l].setText(""+lBoard.board[i][j][k][l]);
                        tiles[i][j][k][l].setStyle("-fx-font-fill: black;");
                        // tiles[i][j][k][l].setEditable(false);
                        tiles[i][j][k][l].setMouseTransparent(true);
                    }
                }
            }
        }
    }


    class Tile extends TextField {
        private int row, col, srow, scol;
        private String lastEntered = new String();      // Initially the default empty string

        final String defaultStyle = "-fx-border-color: black; -fx-border-width: 1; -fx-border-style: solid; -fx-text-fill: " + color;

        // @Override
        // protected double computePrefWidth(double height) {      // The preferred width computed for said region at any given height will be height, which means that excess width will not mean that the width will be more than height in theory
        //     return this.getHeight();
        // }

        // @Override
        // protected double computePrefHeight(double width) {      // always computes the height dim that can be fit along the minimum dimension, hence width property can be bound to height
        //     if (root.getWidth() < root.getHeight()) {
        //         return ( root.getWidth() -20 -10 -4*dim )/(dim*dim) - 2;
        //     }
        //     else {
        //         return ( root.getHeight() - 60 - 30 -10 -4*dim )/(dim*dim) - 2;
        //     }
        // }        

        Tile(int row, int col, int srow, int scol) {
            this.setStyle(defaultStyle);
            this.row = row;
            this.col = col;
            this.srow = srow;
            this.scol = scol;

            this.setAlignment(Pos.CENTER);
            // this.setMinSize(20, 20);
            double minTileDim = (root.getMinHeight() - 60 - 30 -10 -4*dim)/(dim*dim) - 2;
            double maxTileDim = (root.getMaxHeight() - 60 - 30 - 10 -4*dim)/(dim*dim) - 2;
            this.setMinSize(minTileDim, minTileDim);
            this.setMaxSize(maxTileDim, maxTileDim);

            this.prefHeightProperty().bind(root.minDimensionProperty());
            this.prefWidthProperty().bind(root.minDimensionProperty());
            
            this.heightProperty().addListener( (ObservableValue<? extends Number> observable, Number oldValue, Number newValue ) -> {
                this.setFont(new Font(newValue.doubleValue()*0.4));
            });
            // this.prefWidthProperty().bind(root.widthProperty().add(-20-10-4*dim).divide(dim*dim).add(-2));

            // //****TEST CODE*********** */
            // this.widthProperty().addListener( (ObservableValue<? extends Number> observable, Number oldWidth, Number newWidth) -> {
            //     if (newWidth.doubleValue()<this.getHeight()) {
            //         this.setPrefHeight(newWidth.doubleValue());
            //     }
            // });

            //this.minDimensionProperty.addListener(null);

            // this.setPrefSize(100, 100);

            // this.textProperty().addListener( (ObservableValue<? extends String> observable, String oldString, String newString) -> {       // Whenever observable value textProperty cahnges, the listener is notified (as it is registered to it using addListener()) and the changed method is called // ObservableValue<T> interface, wraps value of T as Observable
            //     try {
            //         int num = Integer.parseInt(newString);
            //         if (num>0 && num<=dim*dim) {

            //             if (lBoard.placeDigit(row, col, srow, scol, num) || lBoard.board[row][col][srow][scol]==num)
            //                 return;
            //         }
            //     }
            //     catch (NumberFormatException e) {
            //     }
                
            //     // this.setStyle("-fx-border-color: red; -fx-border-width: 5; -fx-border-style: solid;");
            //     this.clear();
            //     // this.setStyle(defaultStyle);
            //     // System.out.println("Height: " + this.getHeight() + "\tPreferred Width: " + this.computePrefWidth(this.getHeight()) + "\tWidth: " + this.getWidth() + "\tMin Dimension: " + minTileDim);
            //     System.out.println("Width: " + this.getWidth() + "\tPreferred Height: " + this.computePrefHeight(this.getWidth()) + "\tHeight: " + this.getHeight() + "\tMin Dimension: " + minTileDim);
            // });

            //***************************************** */
            this.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldFocus, Boolean newFocus) -> {         // User Focus EventListener added to each tile
                if (oldFocus==true && newFocus==false) {
                    String text=this.getText();
                    System.out.println("Height: " + this.getHeight() + "\tPrefHeight" + this.getPrefHeight() + "\tWidth: " + this.getWidth() + "\tPrefWidth: " + this.getPrefWidth());
                    if (text.equals(lastEntered)) return;      // == tests for reference equality!!          // If nothing done, then do nothing 
                    try {
                        int num = Integer.parseInt(text);
                        if (num>0 && num<=dim*dim) {
                            if (lBoard.placeDigit(row, col, srow, scol, num)) {  // Text to be input is successfully placed on board
                                lastEntered = text;
                                return;
                            }
                            // Focus is UserFocus which means that this checks solely for userinput
                        }
                    }
                    
                    catch (NumberFormatException e) {
                    }
                    lastEntered = "";       // LastEntered Set to empty string
                    this.clear();
                }

            });
        }
    }
}