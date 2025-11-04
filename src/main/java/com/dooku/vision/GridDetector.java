package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Responsible for detecting Sudoku grids in camera frames.
 * Implements preprocessing pipeline, contour detection, and perspective transformation.
 */
public class GridDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(GridDetector.class);
    
    // Configuration constants
    private static final double MIN_AREA_RATIO = 0.1; // Minimum area relative to frame size
    private static final double MAX_AREA_RATIO = 0.9; // Maximum area relative to frame size
    private static final double APPROX_EPSILON = 0.02;
    private static final double ASPECT_RATIO_TOLERANCE = 0.3; // ±30% from 1.0
    private static final int MORPH_KERNEL_SIZE = 3;
    private static final int GAUSSIAN_KERNEL_SIZE = 5;
    private static final int ADAPTIVE_BLOCK_SIZE = 11;
    private static final int ADAPTIVE_C = 2;
    private static final int GRID_OUTPUT_SIZE = 450;
    
    /**
     * Detect a Sudoku grid in the given frame.
     * 
     * @param frame The input frame from the camera
     * @return GridDetectionResult containing detection status and data
     */
    public GridDetectionResult detectGrid(Mat frame) {
        if (frame == null || frame.empty()) {
            logger.debug("Frame is null or empty");
            return GridDetectionResult.failure();
        }
        
        Mat gray = new Mat();
        Mat blurred = new Mat();
        Mat thresh = new Mat();
        Mat morphed = new Mat();
        
        try {
            // 1. Preprocessing pipeline
            preprocessFrame(frame, gray, blurred, thresh, morphed);
            
            // 2. Find contours
            MatVector contours = new MatVector();
            Mat hierarchy = new Mat();
            opencv_imgproc.findContours(
                morphed, contours, hierarchy,
                opencv_imgproc.RETR_EXTERNAL,
                opencv_imgproc.CHAIN_APPROX_SIMPLE
            );
            
            // 3. Find the largest valid quadrilateral
            Point[] corners = findLargestQuadrilateral(contours, frame);
            
            hierarchy.close();
            
            if (corners == null) {
                logger.debug("No valid quadrilateral found");
                return GridDetectionResult.failure();
            }
            
            // 4. Perform perspective transformation
            Mat warpedGrid = warpPerspective(frame, corners);
            
            if (warpedGrid.empty()) {
                logger.debug("Perspective warp failed");
                return GridDetectionResult.failure();
            }
            
            // Calculate confidence based on contour properties
            double confidence = calculateConfidence(corners, frame);
            
            logger.debug("Grid detected with confidence: {}", confidence);
            return GridDetectionResult.success(corners, warpedGrid, confidence);
            
        } catch (Exception e) {
            logger.error("Error detecting grid", e);
            return GridDetectionResult.failure();
        } finally {
            gray.close();
            blurred.close();
            thresh.close();
            morphed.close();
        }
    }
    
    /**
     * Preprocess the frame for grid detection.
     */
    private void preprocessFrame(Mat frame, Mat gray, Mat blurred, Mat thresh, Mat morphed) {
        // Convert to grayscale
        opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
        
        // Apply Gaussian blur for noise reduction
        opencv_imgproc.GaussianBlur(
            gray, blurred, 
            new Size(GAUSSIAN_KERNEL_SIZE, GAUSSIAN_KERNEL_SIZE), 
            0
        );
        
        // Apply adaptive thresholding
        opencv_imgproc.adaptiveThreshold(
            blurred, thresh, 255,
            opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            opencv_imgproc.THRESH_BINARY_INV,
            ADAPTIVE_BLOCK_SIZE, ADAPTIVE_C
        );
        
        // Morphological operations to close gaps in grid lines
        Mat kernel = opencv_imgproc.getStructuringElement(
            opencv_imgproc.MORPH_RECT,
            new Size(MORPH_KERNEL_SIZE, MORPH_KERNEL_SIZE)
        );
        opencv_imgproc.morphologyEx(
            thresh, morphed,
            opencv_imgproc.MORPH_CLOSE,
            kernel
        );
        kernel.close();
    }
    
    /**
     * Find the largest valid quadrilateral in the contours.
     */
    private Point[] findLargestQuadrilateral(MatVector contours, Mat frame) {
        double frameArea = frame.rows() * frame.cols();
        double minArea = frameArea * MIN_AREA_RATIO;
        double maxArea = frameArea * MAX_AREA_RATIO;
        double maxValidArea = 0;
        Point[] bestQuad = null;
        
        for (int i = 0; i < contours.size(); i++) {
            Mat contour = contours.get(i);
            double area = opencv_imgproc.contourArea(contour);
            
            // Check area bounds
            if (area < minArea || area > maxArea) {
                continue;
            }
            
            // Approximate the contour to a polygon
            Mat approx = new Mat();
            double epsilon = APPROX_EPSILON * opencv_imgproc.arcLength(contour, true);
            opencv_imgproc.approxPolyDP(contour, approx, epsilon, true);
            
            // Check if it's a quadrilateral
            if (approx.rows() == 4) {
                // Check if it's convex
                if (opencv_imgproc.isContourConvex(approx)) {
                    // Validate aspect ratio (should be close to square)
                    Point[] points = extractPoints(approx);
                    if (isValidAspectRatio(points)) {
                        if (area > maxValidArea) {
                            maxValidArea = area;
                            bestQuad = points;
                        }
                    }
                }
            }
            
            approx.close();
        }
        
        return bestQuad;
    }
    
    /**
     * Extract points from a contour Mat.
     */
    private Point[] extractPoints(Mat contour) {
        Point[] points = new Point[4];
        for (int i = 0; i < 4; i++) {
            // For CV_32SC2 or similar contours, x is at byte offset 0, y is at byte offset 4
            // For CV_32FC2 contours, we need to read as floats
            float x = contour.ptr(i).getFloat(0);
            float y = contour.ptr(i).getFloat(1);
            points[i] = new Point((int) x, (int) y);
        }
        return orderPoints(points);
    }
    
    /**
     * Order points in clockwise order: top-left, top-right, bottom-right, bottom-left.
     */
    private Point[] orderPoints(Point[] points) {
        // Calculate center
        double centerX = Arrays.stream(points).mapToDouble(Point::x).average().orElse(0);
        double centerY = Arrays.stream(points).mapToDouble(Point::y).average().orElse(0);
        
        // Sort by angle from center
        Arrays.sort(points, (a, b) -> {
            double angleA = Math.atan2(a.y() - centerY, a.x() - centerX);
            double angleB = Math.atan2(b.y() - centerY, b.x() - centerX);
            return Double.compare(angleA, angleB);
        });
        
        // Rotate so that top-left is first
        int topLeftIdx = 0;
        double minSum = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double sum = points[i].x() + points[i].y();
            if (sum < minSum) {
                minSum = sum;
                topLeftIdx = i;
            }
        }
        
        Point[] ordered = new Point[4];
        for (int i = 0; i < 4; i++) {
            ordered[i] = points[(topLeftIdx + i) % 4];
        }
        
        return ordered;
    }
    
    /**
     * Validate that the quadrilateral has a reasonable aspect ratio (close to square).
     */
    private boolean isValidAspectRatio(Point[] points) {
        // Calculate width and height
        double width1 = distance(points[0], points[1]);
        double width2 = distance(points[2], points[3]);
        double height1 = distance(points[0], points[3]);
        double height2 = distance(points[1], points[2]);
        
        double avgWidth = (width1 + width2) / 2.0;
        double avgHeight = (height1 + height2) / 2.0;
        
        double aspectRatio = avgWidth / avgHeight;
        
        // Check if aspect ratio is close to 1.0 (square)
        return aspectRatio > (1.0 - ASPECT_RATIO_TOLERANCE) && 
               aspectRatio < (1.0 + ASPECT_RATIO_TOLERANCE);
    }
    
    /**
     * Calculate Euclidean distance between two points.
     */
    private double distance(Point p1, Point p2) {
        double dx = p1.x() - p2.x();
        double dy = p1.y() - p2.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Perform perspective warp to get top-down view of the grid.
     */
    private Mat warpPerspective(Mat frame, Point[] corners) {
        if (corners == null || corners.length != 4) {
            logger.warn("Invalid corners array for perspective warp");
            return new Mat();
        }
        
        // Define destination points (a perfect square)
        Mat srcPoints = new Mat(4, 1, opencv_core.CV_32FC2);
        Mat dstPoints = new Mat(4, 1, opencv_core.CV_32FC2);
        
        try {
            for (int i = 0; i < 4; i++) {
                srcPoints.ptr(i).putFloat(0, (float) corners[i].x());
                srcPoints.ptr(i).putFloat(1, (float) corners[i].y());
            }
            
            dstPoints.ptr(0).putFloat(0, 0);
            dstPoints.ptr(0).putFloat(1, 0);
            dstPoints.ptr(1).putFloat(0, GRID_OUTPUT_SIZE);
            dstPoints.ptr(1).putFloat(1, 0);
            dstPoints.ptr(2).putFloat(0, GRID_OUTPUT_SIZE);
            dstPoints.ptr(2).putFloat(1, GRID_OUTPUT_SIZE);
            dstPoints.ptr(3).putFloat(0, 0);
            dstPoints.ptr(3).putFloat(1, GRID_OUTPUT_SIZE);
            
            // Get perspective transform matrix
            Mat transform = opencv_imgproc.getPerspectiveTransform(srcPoints, dstPoints);
            
            // Apply perspective warp
            Mat warped = new Mat();
            opencv_imgproc.warpPerspective(
                frame, warped, transform, 
                new Size(GRID_OUTPUT_SIZE, GRID_OUTPUT_SIZE)
            );
            
            transform.close();
            
            return warped;
            
        } finally {
            srcPoints.close();
            dstPoints.close();
        }
    }
    
    /**
     * Calculate confidence score based on grid properties.
     */
    private double calculateConfidence(Point[] corners, Mat frame) {
        // Simple confidence based on regularity of the quadrilateral
        double width1 = distance(corners[0], corners[1]);
        double width2 = distance(corners[2], corners[3]);
        double height1 = distance(corners[0], corners[3]);
        double height2 = distance(corners[1], corners[2]);
        
        // Check how similar opposite sides are
        double widthSimilarity = Math.min(width1, width2) / Math.max(width1, width2);
        double heightSimilarity = Math.min(height1, height2) / Math.max(height1, height2);
        
        // Average similarity as confidence
        return (widthSimilarity + heightSimilarity) / 2.0;
    }
}
