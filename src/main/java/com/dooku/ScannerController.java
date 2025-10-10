package com.dooku;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_videoio;

import org.bytedeco.javacpp.Loader;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScannerController {

    @FXML
    private ImageView cameraView;
    @FXML
    private Label statusMessage;
    @FXML
    private Button backButton, snapButton;

    private VideoCapture capture;
    private boolean cameraActive = false;
    private ScheduledExecutorService frameTimer;

    @FXML
    private void initialize() {
        System.out.println("ScannerController initialized");
        initCamera();
    }

    private void initCamera() {
        try {
            // Force load native libs (sometimes needed on Windows to avoid first-call overhead)
            Loader.load(opencv_core.class);
            Loader.load(opencv_videoio.class);

            capture = new VideoCapture(0); // device 0
            // if (!capture.isOpened()) {
            //     statusMessage.setText("Cannot open camera device 0");
            //     return;
            // }
            cameraActive = true;

            frameTimer = Executors.newSingleThreadScheduledExecutor();
            frameTimer.scheduleAtFixedRate(this::grabAndShowFrame, 0, 33, TimeUnit.MILLISECONDS); // ~30 FPS
            statusMessage.setText("Camera active. Align the puzzle.");
        } catch (Exception e) {
            statusMessage.setText("Camera init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void grabAndShowFrame() {
        if (!cameraActive || capture == null) return;

        Mat frame = new Mat();
        if (capture.read(frame) && !frame.empty()) {
            // (Optional) Flip or preprocess
            // opencv_core.flip(frame, frame, 1);

            Image fxImage = OpenCVUtils.matToImage(frame);
            Platform.runLater(() -> cameraView.setImage(fxImage));
        }
    }

    @FXML
    private void takeSnapshot() {
        if (capture == null || !capture.isOpened()) {
            statusMessage.setText("No camera for snapshot.");
            return;
        }
        Mat shot = new Mat();
        if (capture.read(shot) && !shot.empty()) {
            String filename = "snapshot_raw.png";
            opencv_imgcodecs.imwrite(filename, shot);
            statusMessage.setText("Snapshot saved: " + filename);
            // Later: send path to Python for grid recognition
        } else {
            statusMessage.setText("Failed to capture frame.");
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
        if (frameTimer != null) {
            frameTimer.shutdown();
            try {
                frameTimer.awaitTermination(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {}
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
    }
}