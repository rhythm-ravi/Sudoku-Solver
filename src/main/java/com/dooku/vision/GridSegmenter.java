package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Segments a detected Sudoku grid into individual cells.
 * Supports variable grid dimensions (4x4, 6x6, 9x9, 12x12, 16x16).
 */
public class GridSegmenter {
    
    private static final Logger logger = LoggerFactory.getLogger(GridSegmenter.class);
    
    // Configuration
    private static final int CELL_OUTPUT_SIZE = 28; // 28x28 for MNIST compatibility
    private static final double PADDING_RATIO = 0.15; // Remove 15% padding from edges
    
    private final int gridSize;
    
    /**
     * Create a grid segmenter for the specified grid dimensions.
     * 
     * @param gridSize The size of the grid (e.g., 9 for 9x9 Sudoku)
     */
    public GridSegmenter(int gridSize) {
        if (gridSize < 4 || gridSize > 16) {
            throw new IllegalArgumentException("Grid size must be between 4 and 16");
        }
        this.gridSize = gridSize;
    }
    
    /**
     * Segment the warped grid into individual cells.
     * 
     * @param warpedGrid The warped grid image (should be square)
     * @return Array of preprocessed cell images, ready for classification
     */
    public Mat[] segmentGrid(Mat warpedGrid) {
        if (warpedGrid == null || warpedGrid.empty()) {
            logger.warn("Warped grid is null or empty");
            return new Mat[0];
        }
        
        int totalCells = gridSize * gridSize;
        Mat[] cells = new Mat[totalCells];
        
        int gridPixelSize = warpedGrid.rows();
        int cellPixelSize = gridPixelSize / gridSize;
        
        logger.debug("Segmenting {}x{} grid, cell size: {}px", gridSize, gridSize, cellPixelSize);
        
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int idx = row * gridSize + col;
                
                // Extract cell region
                Rect cellRect = new Rect(
                    col * cellPixelSize,
                    row * cellPixelSize,
                    cellPixelSize,
                    cellPixelSize
                );
                
                Mat cellRoi = new Mat(warpedGrid, cellRect);
                
                // Preprocess cell
                Mat processedCell = preprocessCell(cellRoi);
                cells[idx] = processedCell;
            }
        }
        
        return cells;
    }
    
    /**
     * Preprocess a single cell for digit classification.
     * - Convert to grayscale
     * - Remove padding
     * - Center the digit
     * - Resize to 28x28
     * - Normalize
     */
    private Mat preprocessCell(Mat cellRoi) {
        Mat gray = new Mat();
        Mat padded = new Mat();
        Mat resized = new Mat();
        Mat normalized = new Mat();
        
        try {
            // Convert to grayscale if needed
            if (cellRoi.channels() == 3) {
                opencv_imgproc.cvtColor(cellRoi, gray, opencv_imgproc.COLOR_BGR2GRAY);
            } else {
                gray = cellRoi.clone();
            }
            
            // Remove padding from edges
            int padding = (int) (gray.rows() * PADDING_RATIO);
            if (padding > 0 && gray.rows() > 2 * padding && gray.cols() > 2 * padding) {
                Rect innerRect = new Rect(
                    padding, padding,
                    gray.cols() - 2 * padding,
                    gray.rows() - 2 * padding
                );
                Mat inner = new Mat(gray, innerRect);
                padded = inner.clone();
                inner.close();
            } else {
                padded = gray.clone();
            }
            
            // Apply thresholding to isolate digit
            Mat thresh = new Mat();
            opencv_imgproc.threshold(
                padded, thresh, 0, 255,
                opencv_imgproc.THRESH_BINARY_INV | opencv_imgproc.THRESH_OTSU
            );
            
            // Center the digit within the cell
            Mat centered = centerDigit(thresh);
            
            // Resize to 28x28 (MNIST size)
            opencv_imgproc.resize(centered, resized, new Size(CELL_OUTPUT_SIZE, CELL_OUTPUT_SIZE));
            
            // Normalize pixel values to 0-1 range
            resized.convertTo(normalized, opencv_core.CV_32F, 1.0 / 255.0, 0.0);
            
            thresh.close();
            centered.close();
            
            return normalized;
            
        } finally {
            gray.close();
            padded.close();
            resized.close();
        }
    }
    
    /**
     * Center the digit within the image by finding its bounding box.
     */
    private Mat centerDigit(Mat binaryImage) {
        // Find bounding box of non-zero pixels
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        opencv_imgproc.findContours(
            binaryImage, contours, hierarchy,
            opencv_imgproc.RETR_EXTERNAL,
            opencv_imgproc.CHAIN_APPROX_SIMPLE
        );
        
        if (contours.size() == 0) {
            hierarchy.close();
            return binaryImage.clone();
        }
        
        // Find the largest contour (should be the digit)
        double maxArea = 0;
        int maxIdx = -1;
        for (int i = 0; i < contours.size(); i++) {
            double area = opencv_imgproc.contourArea(contours.get(i));
            if (area > maxArea) {
                maxArea = area;
                maxIdx = i;
            }
        }
        
        if (maxIdx == -1 || maxArea < 10) { // Too small, likely empty
            hierarchy.close();
            return binaryImage.clone();
        }
        
        // Get bounding rectangle
        Rect boundingRect = opencv_imgproc.boundingRect(contours.get(maxIdx));
        hierarchy.close();
        
        // Create centered image
        int size = Math.max(boundingRect.width(), boundingRect.height());
        size = (int) (size * 1.2); // Add some margin
        
        // Limit size to image dimensions
        size = Math.min(size, binaryImage.rows());
        size = Math.min(size, binaryImage.cols());
        
        Mat centered = new Mat(size, size, binaryImage.type());
        centered.put(new Scalar(0.0, 0.0, 0.0, 0.0));
        
        // Calculate position to place the digit in center
        int x = (size - boundingRect.width()) / 2;
        int y = (size - boundingRect.height()) / 2;
        
        // Extract digit region
        Mat digitRegion = new Mat(binaryImage, boundingRect);
        
        // Place in center
        Rect targetRect = new Rect(x, y, boundingRect.width(), boundingRect.height());
        Mat targetRoi = new Mat(centered, targetRect);
        digitRegion.copyTo(targetRoi);
        
        digitRegion.close();
        targetRoi.close();
        
        return centered;
    }
    
    /**
     * Get the grid size.
     */
    public int getGridSize() {
        return gridSize;
    }
}
