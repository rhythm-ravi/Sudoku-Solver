package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;

/**
 * Result of grid detection operation.
 * Contains the detected grid corners and the warped grid image if successful.
 */
public class GridDetectionResult {
    
    private final boolean detected;
    private final Point[] corners;
    private final Mat warpedGrid;
    private final double confidence;
    
    private GridDetectionResult(boolean detected, Point[] corners, Mat warpedGrid, double confidence) {
        this.detected = detected;
        this.corners = corners;
        this.warpedGrid = warpedGrid;
        this.confidence = confidence;
    }
    
    /**
     * Create a successful detection result
     */
    public static GridDetectionResult success(Point[] corners, Mat warpedGrid, double confidence) {
        if (corners == null || corners.length != 4) {
            throw new IllegalArgumentException("Corners must be an array of 4 points");
        }
        if (warpedGrid == null || warpedGrid.empty()) {
            throw new IllegalArgumentException("Warped grid must not be null or empty");
        }
        return new GridDetectionResult(true, corners, warpedGrid, confidence);
    }
    
    /**
     * Create a failed detection result
     */
    public static GridDetectionResult failure() {
        return new GridDetectionResult(false, null, null, 0.0);
    }
    
    public boolean isDetected() {
        return detected;
    }
    
    public Point[] getCorners() {
        return corners;
    }
    
    public Mat getWarpedGrid() {
        return warpedGrid;
    }
    
    public double getConfidence() {
        return confidence;
    }
}
