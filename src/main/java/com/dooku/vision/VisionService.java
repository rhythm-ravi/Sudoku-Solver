package com.dooku.vision;

import com.dooku.vision.model.GridSearchResult;
import com.dooku.vision.model.GridSearchResult.Status;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Service responsible for computer vision tasks related to Sudoku grid detection and recognition.
 */
public class VisionService {
    
    private final ExecutorService executorService;
    private final Classifier classifier;
    
    // Constants for grid detection
    private static final double MIN_AREA = 10000;
    private static final double APPROX_EPSILON = 0.02;
    private static final int STABILITY_THRESHOLD = 10; // pixels
    
    // State for stability checking
    private Point[] lastCorners = null;
    private int stableFrameCount = 0;
    private static final int REQUIRED_STABLE_FRAMES = 5;
    
    public VisionService() {
        // Create a fixed thread pool for processing
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        this.classifier = new Classifier();
    }
    
    /**
     * Find a Sudoku grid in the given frame.
     * 
     * @param frame The input frame from the camera
     * @return A CompletableFuture containing the grid search result
     */
    public CompletableFuture<GridSearchResult> findGrid(Mat frame) {
        return CompletableFuture.supplyAsync(() -> {
            if (frame == null || frame.empty()) {
                return new GridSearchResult(Status.NO_GRID, null);
            }
            
            // Preprocess the frame
            Mat gray = new Mat();
            Mat blurred = new Mat();
            Mat edges = new Mat();
            
            try {
                // Convert to grayscale
                opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
                
                // Apply Gaussian blur
                opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
                
                // Apply adaptive threshold
                opencv_imgproc.adaptiveThreshold(
                    blurred, edges, 255,
                    opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    opencv_imgproc.THRESH_BINARY_INV,
                    11, 2
                );
                
                // Find contours
                MatVector contours = new MatVector();
                Mat hierarchy = new Mat();
                opencv_imgproc.findContours(
                    edges, contours, hierarchy,
                    opencv_imgproc.RETR_EXTERNAL,
                    opencv_imgproc.CHAIN_APPROX_SIMPLE
                );
                
                // Find the largest quadrilateral
                Point[] corners = findLargestQuadrilateral(contours, frame);
                
                if (corners == null) {
                    lastCorners = null;
                    stableFrameCount = 0;
                    return new GridSearchResult(Status.NO_GRID, null);
                }
                
                // Check stability
                boolean isStable = checkStability(corners);
                Status status = isStable ? Status.STABLE : Status.UNSTABLE;
                
                return new GridSearchResult(status, corners);
                
            } finally {
                // Clean up
                gray.close();
                blurred.close();
                edges.close();
            }
        }, executorService);
    }
    
    /**
     * Find the largest quadrilateral in the contours.
     */
    private Point[] findLargestQuadrilateral(MatVector contours, Mat frame) {
        double maxArea = MIN_AREA;
        Point[] bestQuad = null;
        
        for (int i = 0; i < contours.size(); i++) {
            Mat contour = contours.get(i);
            double area = opencv_imgproc.contourArea(contour);
            
            if (area > maxArea) {
                // Approximate the contour to a polygon
                Mat approx = new Mat();
                double epsilon = APPROX_EPSILON * opencv_imgproc.arcLength(contour, true);
                opencv_imgproc.approxPolyDP(contour, approx, epsilon, true);
                
                // Check if it's a quadrilateral
                if (approx.rows() == 4) {
                    // Check if it's convex
                    if (opencv_imgproc.isContourConvex(approx)) {
                        maxArea = area;
                        bestQuad = extractPoints(approx);
                    }
                }
                
                approx.close();
            }
        }
        
        return bestQuad;
    }
    
    /**
     * Extract points from a contour Mat.
     */
    private Point[] extractPoints(Mat contour) {
        Point[] points = new Point[4];
        for (int i = 0; i < 4; i++) {
            int x = contour.ptr(i).get(0);
            int y = contour.ptr(i).get(4);
            points[i] = new Point(x, y);
        }
        return orderPoints(points);
    }
    
    /**
     * Order points in clockwise order: top-left, top-right, bottom-right, bottom-left.
     */
    private Point[] orderPoints(Point[] points) {
        // Calculate center
        double centerX = Arrays.stream(points).mapToDouble(p -> p.x()).average().orElse(0);
        double centerY = Arrays.stream(points).mapToDouble(p -> p.y()).average().orElse(0);
        
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
     * Check if the grid is stable by comparing with the last detected corners.
     */
    private boolean checkStability(Point[] corners) {
        if (lastCorners == null) {
            lastCorners = corners;
            stableFrameCount = 0;
            return false;
        }
        
        // Calculate maximum distance between corresponding corners
        double maxDistance = 0;
        for (int i = 0; i < 4; i++) {
            double dx = corners[i].x() - lastCorners[i].x();
            double dy = corners[i].y() - lastCorners[i].y();
            double distance = Math.sqrt(dx * dx + dy * dy);
            maxDistance = Math.max(maxDistance, distance);
        }
        
        if (maxDistance < STABILITY_THRESHOLD) {
            stableFrameCount++;
        } else {
            stableFrameCount = 0;
        }
        
        lastCorners = corners;
        return stableFrameCount >= REQUIRED_STABLE_FRAMES;
    }
    
    /**
     * Perform perspective warp on the detected grid.
     */
    public Mat warpGrid(Mat frame, Point[] corners) {
        if (frame == null || frame.empty() || corners == null || corners.length != 4) {
            return new Mat();
        }
        
        // Define the size of the output grid
        int gridSize = 450; // 450x450 pixels
        
        // Define destination points (a perfect square)
        Mat srcPoints = new Mat(4, 1, opencv_core.CV_32FC2);
        Mat dstPoints = new Mat(4, 1, opencv_core.CV_32FC2);
        
        for (int i = 0; i < 4; i++) {
            srcPoints.ptr(i).putFloat(0, (float) corners[i].x());
            srcPoints.ptr(i).putFloat(1, (float) corners[i].y());
        }
        
        dstPoints.ptr(0).putFloat(0, 0);
        dstPoints.ptr(0).putFloat(1, 0);
        dstPoints.ptr(1).putFloat(0, gridSize);
        dstPoints.ptr(1).putFloat(1, 0);
        dstPoints.ptr(2).putFloat(0, gridSize);
        dstPoints.ptr(2).putFloat(1, gridSize);
        dstPoints.ptr(3).putFloat(0, 0);
        dstPoints.ptr(3).putFloat(1, gridSize);
        
        // Get perspective transform matrix
        Mat transform = opencv_imgproc.getPerspectiveTransform(srcPoints, dstPoints);
        
        // Apply perspective warp
        Mat warped = new Mat();
        opencv_imgproc.warpPerspective(frame, warped, transform, new Size(gridSize, gridSize));
        
        srcPoints.close();
        dstPoints.close();
        transform.close();
        
        return warped;
    }
    
    /**
     * Segment the warped grid into 81 cells.
     */
    public Mat[] segmentGrid(Mat warpedGrid) {
        if (warpedGrid == null || warpedGrid.empty()) {
            return new Mat[0];
        }
        
        Mat[] cells = new Mat[81];
        int gridSize = warpedGrid.rows();
        int cellSize = gridSize / 9;
        
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = row * 9 + col;
                Rect cellRect = new Rect(
                    col * cellSize,
                    row * cellSize,
                    cellSize,
                    cellSize
                );
                cells[idx] = new Mat(warpedGrid, cellRect).clone();
            }
        }
        
        return cells;
    }
    
    /**
     * Recognize the Sudoku board by classifying all cells.
     * 
     * @param warpedGrid The warped grid image
     * @return A CompletableFuture containing the recognized board (9x9 array)
     */
    public CompletableFuture<int[][]> recognizeBoard(Mat warpedGrid) {
        return CompletableFuture.supplyAsync(() -> {
            Mat[] cells = segmentGrid(warpedGrid);
            
            // Process cells in parallel
            int[] digits = IntStream.range(0, 81)
                .parallel()
                .map(i -> classifier.classify(cells[i]))
                .toArray();
            
            // Convert to 2D array
            int[][] board = new int[9][9];
            for (int i = 0; i < 81; i++) {
                board[i / 9][i % 9] = digits[i];
            }
            
            // Clean up
            for (Mat cell : cells) {
                if (cell != null) {
                    cell.close();
                }
            }
            
            return board;
        }, executorService);
    }
    
    /**
     * Shut down the executor service.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
