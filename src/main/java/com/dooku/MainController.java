package com.dooku;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
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
import javafx.util.Duration;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

import javafx.scene.text.Font;


import javafx.animation.*;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.Bindings;


public class MainController {       // Loose coupling between UI(client) and logic(server)

    private static Stage stage = new Stage();
    static {
        stage.initModality(Modality.APPLICATION_MODAL);
    }
    private static double prevHeight;  private static double prevWidth;   // keep track of sizing of main_scene between instances
    static {
        prevHeight = App.screenHeight/2;    prevWidth = App.screenWidth/2 - 70;     // Initially the dimensions I thought should look decent
    }
    
    @FXML
    private MainScene root = null;
    @FXML
    private GridPane board = null;
    @FXML
    private HBox options = null;
    
    private final int dim = SettingsController.getSetting("dimensions", Integer.class);     // if a new instance is created for each time a scene graph from main.fxml is constructed, then this final modifier should be harmless
    private final String color = SettingsController.getSetting("bgcolor", String.class);
    private Board lBoard = new Board(dim);   //// incorrrectded

    // Alerts
    private Alert closingAlert = new Alert(AlertType.CONFIRMATION);
        
    @FXML
    private void initialize() throws IOException{

        double hOther = 40 + 30;    // spacing (40)  +  30 for the buttons set min height (a rough estimate based on how they need to look w/ respect to current insets and whatnot) 
        
        root.setMinHeight(App.screenHeight/2);      root.setMinWidth(App.screenHeight/2-hOther);       // Min we go for these dimensions; Currently setting the minHeight to ScreenH/2 as that seems about right    (minWidth is width of grid)
        root.setMaxHeight(App.screenHeight);        root.setMaxWidth(App.screenHeight - hOther);
        stage.setMinHeight(root.getMinHeight()*1.1);        stage.setMinWidth(root.getMinWidth()*1.1);

        VBox.setVgrow(options, Priority.NEVER);     // fixing options' dimensions   

        Scene scene = new Scene(root, prevWidth, prevHeight);//, 0.75*min/10*dim*dim, min/10*dim*dim+50);

        scene.getStylesheets().add(getClass().getResource("style/" + color + ".css").toExternalForm());
        scene.setOnKeyPressed( e -> {
            switch (e.getCode()) {
                case UP:
                case DOWN:
                case LEFT:
                case RIGHT:
                case W:
                case A:
                case S:
                case D:
                    if (lastTileFocused != null)
                        navigateToTile(lastTileFocused[0], lastTileFocused[1], lastTileFocused[2], lastTileFocused[3]);
                    break;
                default:
                    return;
            }
            
        });

        stage.setScene(scene);
        stage.setTitle("SUDOKU SOLVER");
        stage.sizeToScene();

        stage.show();
        addTiles();

        ///////////////////////////////////
        closingAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        closingAlert.setContentText("Go back to Menu?");
        stage.setOnCloseRequest(e -> {  // When possibly refreshing instance, store all info that will carry over to next instance
            prevHeight = root.getHeight();  prevWidth = root.getWidth();
            e.consume();
            closingAlert.show();
        });        
        // For some reason alert.showAndWait() created all sorts of problems (MainScene being visible but unresponsive if we decided to stay after the alert pops up,  stage simply not being visible if we pressed NO and never showing until the entire application was restarted and so on...)
        closingAlert.setOnHidden(e -> {     // My best guess is that since in showAndWait it waits for the stage to be closed before returning to caller,  when alert was hidden, control could be transferred to caller, or to alert's hidden handler,  and this might have led to some kind of obfuscation  (Maybe control went from stage onHidden event handler to the alert stage being shown and wait,  to the alert's on hidden as soon as it was closed,  thus stage is shown,  then control back to stage's onHidden event handler?????)
            if (closingAlert.getResult() == ButtonType.YES)
                stage.hide();
        });
    }

    @FXML
    private void clear() throws IOException{    // New instances of scene,controller only when refreshing (closing->opening XOR clearing). Keep track of all information between instances
        prevHeight = root.getHeight();  prevWidth = root.getWidth();
        new MainScene(dim);
    }

    
    private MainScene.Tile[][][][] tiles = new MainScene.Tile[dim][dim][dim][dim];
    int[] lastTileFocused = null;      // Last tile focused

    private void addTiles() {
        board.getStyleClass().add("grid-pane");

        int heightOther = 60+30+10+4*dim +20;   int widthOther = 20+10+4*dim +20;
        DoubleBinding tileBinding = Bindings.createDoubleBinding(
            () -> {
                return Math.min(root.getHeight()-heightOther, root.getWidth()-widthOther)/(dim*dim);
            }, root.widthProperty(), root.heightProperty()
        );

        for (int i=0; i<dim; i++) {
            for (int j=0; j<dim; j++) {
                GridPane subGrid = new GridPane();
                subGrid.getStyleClass().add("grid-pane");
                

                for (int k=0; k<dim; k++)
                    for (int l=0; l<dim; l++) {

                        MainScene.Tile tile = root.new Tile(i,j,k,l);
                        tiles[i][j][k][l] = tile;    
                        
                        // UI Listeners
                        // Dimension binding
                        tile.minHeightProperty().bind(tileBinding);
                        tile.prefHeightProperty().bind(tileBinding);
                        tile.maxHeightProperty().bind(tileBinding);
                        tile.minWidthProperty().bind(tileBinding);
                        tile.prefWidthProperty().bind(tileBinding);
                        tile.maxWidthProperty().bind(tileBinding);
                        // Cool hovering mouse input
                        tile.setOnMouseEntered( e -> {
                            tile.requestFocus();
                        });
                        tile.setOnMouseExited( e -> {
                            tile.getScene().getRoot().requestFocus();      // Remove focus from this tile
                        });
                        // Keyboard navigation if on a tile
                        tile.setOnKeyPressed( e -> {
                            int idx = (tile.row*dim + tile.srow)*dim*dim + (tile.col*dim + tile.scol);
                            int nidx = dim*dim*dim*dim;
                            switch (e.getCode()) {
                                case UP:
                                    idx = (idx - dim*dim + nidx) % nidx;
                                    break;
                                case DOWN:
                                    idx = (idx + dim*dim) % nidx;
                                    break;
                                case LEFT:
                                    idx = (idx - 1 + nidx) % nidx;
                                    break;
                                case RIGHT:
                                    idx = (idx + 1) % nidx;
                                    break;
                                // case W:
                                //     navigateToTile((tile.row-1)%dim, tile.col, tile.srow, tile.scol);
                                //     break;
                                // case S:
                                //     navigateToTile((tile.row+1)%dim, tile.col, tile.srow, tile.scol);
                                //     break;
                                // case A:
                                //     navigateToTile(tile.row, (tile.col-1)%dim, tile.srow, tile.scol);
                                //     break;
                                // case D:
                                //     navigateToTile(tile.row, (tile.col+1)%dim, tile.srow, tile.scol);
                                //     break;

                                case ENTER:
                                    root.requestFocus();   // Remove focus from this tile
                                default:
                                    break;
                            }
                            int row = (idx / (dim*dim)) / dim;   int col = (idx % (dim*dim)) / dim;   int srow = (idx / (dim*dim)) % dim;   int scol = (idx % (dim*dim)) % dim;
                            navigateToTile(row, col, srow, scol);
                        });
                        // Font size binding
                        tile.heightProperty().addListener( (ObservableValue<? extends Number> observable, Number oldValue, Number newValue ) -> {
                            tile.setFont(new Font(newValue.doubleValue()*0.4));
                        });

                        // Last focused tile tracking
                        tile.focusedProperty().addListener( (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                            if (newValue) {     // Gained focus
                                lastTileFocused = new int[]{tile.row, tile.col, tile.srow, tile.scol};
                            }
                        });

                        // Input listener
                        tile.focusedProperty().addListener( (ObservableValue<? extends Boolean> observable, Boolean oldFocus, Boolean newFocus) -> {         // User Focus EventListener added to each tile
                            
                            if (!root.isLocked && oldFocus==true && newFocus==false) {    // An input may have been provided
                                                                
                                if (tile.lastEntered.equals( tile.getText() ))     // We either hovered, or thought of editing, but no change in input!
                                    return;
                                // System.out.printf("We're still here\n");
                                boolean isPlaced=false;
                                int input;
                                try {
                                    input = Integer.parseInt(tile.getText());
                                    if (lBoard.placeDigit(tile.row, tile.col, tile.srow, tile.scol, input))
                                        isPlaced=true;
                                }
                                catch (NumberFormatException e) {
                                }
                                if (!isPlaced) {    // Invalid input
                                    tile.clear();
                                    lBoard.removeDigit(tile.row, tile.col, tile.srow, tile.scol);
                                }
                                tile.lastEntered = tile.getText();
                            }
                
                        });

                        subGrid.add(tile, l, k);    // default col and wo span to 1
                    }
            
                board.add(subGrid, j, i);   // col, row
            }
        }
    }

    // For ui navigation
    private void navigateToTile(int i, int j, int k, int l) {
        if (i<0 || i>=dim || j<0 || j>=dim || k<0 || k>=dim || l<0 || l>=dim)
            return;
        tiles[i][j][k][l].requestFocus();
    }
    

    @FXML
    private void solve() {
        if (root.isLocked)
            return;  
        root.isLocked=true;

        for (int i=0; i<dim; i++)
            for (int j=0; j<dim; j++)
                for (int k=0; k<dim; k++)
                    for (int l=0; l<dim; l++)
                        tiles[i][j][k][l].setEditable(false);   // Can't be edited by user no more

        // Thread logicThread = new Thread(new Runnable() {
        //     public void run() {
        //         lBoard.solve();
        //     }
        // });
        // logicThread.start();
        lBoard.solve();

        // for (int i=0; i<dim; i++) {
        //     for (int j=0; j<dim; j++) {
        //         for (int k=0; k<dim; k++){
        //             for (int l=0; l<dim; l++) {
        //                 if (tiles[i][j][k][l].getText()=="") {
        //                     tiles[i][j][k][l].setText(""+lBoard.board[i][j][k][l]);
        //                     tiles[i][j][k][l].setStyle("-fx-text-fill: black;");
        //                 }
        //                 // tiles[i][j][k][l].setEditable(false);
        //                 // tiles[i][j][k][l].setMouseTransparent(true);
        //             }
        //         }
        //     }
        // }

        while (!lBoard.observableState.isEmpty()) {
            int[] step = lBoard.observableState.pollFirst();    // Ordered tuple having all information regarding step that we took
            int value = step[0];    int i, j, k, l;
            i=step[1];  j=step[2];  k=step[3];  l=step[4];

            PauseTransition p = new PauseTransition( Duration.millis(5) );
            p.setOnFinished( e -> {
                MainScene.Tile t = tiles[i][j][k][l];
                t.setStyle("-fx-text-fill: black;");
                t.setText(""+ ((value!=0) ? value : ""));   // value=0 in step means backtrack, else extension try
            });
            solveAnimation.getChildren().add(p);
        }

        solveAnimation.play();
    }

    // Animations

    private SequentialTransition solveAnimation = new SequentialTransition();
}