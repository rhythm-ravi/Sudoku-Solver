package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridDetectorTest {
    
    private GridDetector gridDetector;
    
    @BeforeEach
    void setUp() {
        gridDetector = new GridDetector();
    }
    
    @Test
    void testDetectGridWithNull() {
        GridDetectionResult result = gridDetector.detectGrid(null);
        
        assertNotNull(result);
        assertFalse(result.isDetected());
        assertNull(result.getCorners());
        assertNull(result.getWarpedGrid());
    }
    
    @Test
    void testDetectGridWithEmptyMat() {
        Mat emptyMat = new Mat();
        GridDetectionResult result = gridDetector.detectGrid(emptyMat);
        
        assertNotNull(result);
        assertFalse(result.isDetected());
        assertNull(result.getCorners());
        assertNull(result.getWarpedGrid());
        
        emptyMat.close();
    }
    
    @Test
    void testDetectGridWithBlankImage() {
        // Create a blank 640x480 image
        Mat blankImage = new Mat(480, 640, opencv_core.CV_8UC3);
        blankImage.put(new Scalar(255.0, 255.0, 255.0, 0.0)); // White image
        
        GridDetectionResult result = gridDetector.detectGrid(blankImage);
        
        assertNotNull(result);
        // A blank image should not have a grid detected
        assertFalse(result.isDetected());
        
        blankImage.close();
    }
    
    @Test
    void testDetectGridWithSimpleSquare() {
        // Create an image with a large square
        Mat image = new Mat(480, 640, opencv_core.CV_8UC3);
        image.put(new Scalar(255.0, 255.0, 255.0, 0.0)); // White background
        
        // Draw a black square
        Point pt1 = new Point(150, 100);
        Point pt2 = new Point(450, 400);
        opencv_imgproc.rectangle(image, pt1, pt2, new Scalar(0, 0, 0, 0), 5, opencv_imgproc.LINE_8, 0);
        
        GridDetectionResult result = gridDetector.detectGrid(image);
        
        assertNotNull(result);
        // This test might detect or not depending on contour complexity
        // Just verify it doesn't crash and returns valid result
        if (result.isDetected()) {
            assertNotNull(result.getCorners());
            assertEquals(4, result.getCorners().length);
            assertNotNull(result.getWarpedGrid());
            assertFalse(result.getWarpedGrid().empty());
            assertTrue(result.getConfidence() > 0.0);
            result.getWarpedGrid().close();
        }
        
        image.close();
    }
    
    @Test
    void testDetectGridWithTooSmallContour() {
        // Create an image with a very small square
        Mat image = new Mat(480, 640, opencv_core.CV_8UC3);
        image.put(new Scalar(255.0, 255.0, 255.0, 0.0)); // White background
        
        // Draw a tiny black square (should be rejected due to MIN_AREA)
        Point pt1 = new Point(300, 230);
        Point pt2 = new Point(340, 250);
        opencv_imgproc.rectangle(image, pt1, pt2, new Scalar(0, 0, 0, 0), 2, opencv_imgproc.LINE_8, 0);
        
        GridDetectionResult result = gridDetector.detectGrid(image);
        
        assertNotNull(result);
        // Small contour should not be detected
        assertFalse(result.isDetected());
        
        image.close();
    }
}
