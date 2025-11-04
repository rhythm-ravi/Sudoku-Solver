package com.dooku;

import com.dooku.utils.OpenCVUtils;
import com.dooku.vision.VisionRecognitionService;
import com.dooku.vision.RecognitionResult;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_videoio;

import org.bytedeco.javacpp.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScannerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ScannerController.class);

    @FXML
    private ImageView cameraView;
    @FXML
    private Label statusMessage;
    @FXML
    private Button backButton, snapButton;
    @FXML
    private Pane overlayPane;

    private VideoCapture capture;
    private boolean cameraActive = false;
    private ScheduledExecutorService frameTimer;
    private VisionRecognitionService recognitionService;

    @FXML
    private void initialize() {
        logger.info("ScannerController initialized");
        
        // Initialize the vision recognition service with configuration
        recognitionService = new VisionRecognitionService(
            com.dooku.vision.VisionConfig.getGridSize(),
            com.dooku.vision.VisionConfig.getFrameInterval(),
            com.dooku.vision.VisionConfig.getConsensusFrames()
        );
        
        // Set callback for recognition results
        recognitionService.setResultCallback(this::handleRecognitionResult);
        
        logger.info("Vision configuration: {}", 
            com.dooku.vision.VisionConfig.getSettingsSummary().replace("\n", " | "));
        
        initCamera();
    }

    private void initCamera() {
        try {
            // Force load native libs (sometimes needed on Windows to avoid first-call overhead)
            Loader.load(opencv_core.class);
            Loader.load(opencv_videoio.class);

            capture = new VideoCapture(0); // device 0
            cameraActive = true;

            // Start the recognition service
            recognitionService.start();

            frameTimer = Executors.newSingleThreadScheduledExecutor();
            frameTimer.scheduleAtFixedRate(this::grabAndShowFrame, 0, 33, TimeUnit.MILLISECONDS); // ~30 FPS
            
            statusMessage.setText("Camera active. Align the puzzle.");
        } catch (Exception e) {
            statusMessage.setText("Camera init failed: " + e.getMessage());
            logger.error("Camera initialization failed", e);
        }
    }

    private void grabAndShowFrame() {
        if (!cameraActive || capture == null) return;

        Mat frame = new Mat();
        if (capture.read(frame) && !frame.empty()) {
            // Display the frame
            Image fxImage = OpenCVUtils.matToImage(frame);
            Platform.runLater(() -> cameraView.setImage(fxImage));
            
            // Process frame with recognition service
            recognitionService.processFrame(frame);
            
            frame.close();
        }
    }
    
    /**
     * Handle recognition results from the vision service.
     * This is called from the recognition service thread.
     */
    private void handleRecognitionResult(RecognitionResult result) {
        Platform.runLater(() -> {
            // Update status message
            statusMessage.setText(result.message());
            
            // Update overlay based on state
            updateOverlay(result);
            
            // Handle completion
            if (result.state() == RecognitionResult.RecognitionState.COMPLETED) {
                handleRecognitionCompleted(result);
            }
        });
    }
    
    /**
     * Update the visual overlay based on recognition state.
     */
    private void updateOverlay(RecognitionResult result) {
        overlayPane.getChildren().clear();
        
        Point[] corners = recognitionService.getCurrentCorners();
        if (corners == null || corners.length != 4) {
            return;
        }
        
        Color overlayColor;
        switch (result.state()) {
            case DETECTED:
                overlayColor = Color.YELLOW;
                break;
            case VERIFYING:
                overlayColor = Color.ORANGE;
                break;
            case CONFIRMED:
            case COMPLETED:
                overlayColor = Color.GREEN;
                break;
            default:
                return; // No overlay for SCANNING or ERROR
        }
        
        drawOverlay(corners, overlayColor);
    }
    
    /**
     * Draw a colored polygon overlay for the detected grid.
     */
    private void drawOverlay(Point[] corners, Color color) {
        if (corners == null || corners.length != 4) return;
        
        // Scale corners to match the ImageView size
        // Assume camera resolution is 640x480
        double scaleX = cameraView.getFitWidth() / 640.0;
        double scaleY = cameraView.getFitHeight() / 480.0;
        
        Polygon polygon = new Polygon();
        for (Point corner : corners) {
            polygon.getPoints().addAll(
                corner.x() * scaleX,
                corner.y() * scaleY
            );
        }
        
        polygon.setFill(Color.TRANSPARENT);
        polygon.setStroke(color);
        polygon.setStrokeWidth(3);
        
        overlayPane.getChildren().add(polygon);
    }
    
    /**
     * Handle completed recognition - navigate to main scene with detected board.
     */
    private void handleRecognitionCompleted(RecognitionResult result) {
        if (result.board() == null) {
            logger.warn("Recognition completed but board is null");
            return;
        }
        
        logger.info("Board recognition completed with confidence: {}", result.confidence());
        printBoard(result.board());
        
        // TODO: Pass the detected board to MainController
        // For now, just show success message
        statusMessage.setText("Board recognized! Confidence: " + 
            String.format("%.1f%%", result.confidence() * 100));
        
        // Stop camera after successful recognition
        stopCamera();
        
        // Navigate to main scene
        // try {
        //     App.setRoot("main");
        // } catch (Exception e) {
        //     logger.error("Failed to navigate to main scene", e);
        // }
    }

    @FXML
    private void takeSnapshot() {
        // Manual snapshot is kept as a fallback option
        // The automatic recognition service is the primary method
        statusMessage.setText("Manual snapshot - use automatic recognition instead");
        logger.info("Manual snapshot button pressed");
        
        // Reset and restart the recognition service
        recognitionService.reset();
    }
    
    private void printBoard(int[][] board) {
        System.out.println("Recognized board:");
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    @FXML
    private void goBack() {
        stopCamera();
        try {
            App.setRoot("menu");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopCamera() {
        cameraActive = false;
        
        // Stop frame timer
        if (frameTimer != null) {
            frameTimer.shutdown();
            try {
                frameTimer.awaitTermination(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Release camera
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        
        // Stop recognition service
        if (recognitionService != null) {
            recognitionService.stop();
        }
        
        logger.info("Camera and recognition service stopped");
    }
}