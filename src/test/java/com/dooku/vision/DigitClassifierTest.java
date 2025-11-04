package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DigitClassifierTest {
    
    private DigitClassifier classifier;
    
    @BeforeEach
    void setUp() {
        classifier = new DigitClassifier();
    }
    
    @AfterEach
    void tearDown() {
        if (classifier != null) {
            classifier.shutdown();
        }
    }
    
    @Test
    void testClassifyBatchWithEmptyArray() {
        Mat[] cells = new Mat[0];
        
        DigitClassifier.ClassificationResult[] results = classifier.classifyBatch(cells);
        
        assertNotNull(results);
        assertEquals(0, results.length);
    }
    
    @Test
    void testClassifyBatchWithNull() {
        DigitClassifier.ClassificationResult[] results = classifier.classifyBatch(null);
        
        assertNotNull(results);
        assertEquals(0, results.length);
    }
    
    @Test
    void testClassifyBatchSingleCell() {
        Mat[] cells = new Mat[1];
        cells[0] = new Mat(28, 28, opencv_core.CV_32F);
        cells[0].put(new Scalar(0.5, 0.0, 0.0, 0.0)); // Gray cell
        
        DigitClassifier.ClassificationResult[] results = classifier.classifyBatch(cells);
        
        assertNotNull(results);
        assertEquals(1, results.length);
        assertNotNull(results[0]);
        assertEquals(0, results[0].getCellIndex());
        assertTrue(results[0].getDigit() >= 0 && results[0].getDigit() <= 9);
        assertTrue(results[0].getConfidence() >= 0.0 && results[0].getConfidence() <= 1.0);
        
        cells[0].close();
    }
    
    @Test
    void testClassifyBatchMultipleCells() {
        int numCells = 81;
        Mat[] cells = new Mat[numCells];
        
        for (int i = 0; i < numCells; i++) {
            cells[i] = new Mat(28, 28, opencv_core.CV_32F);
            cells[i].put(new Scalar(0.1, 0.0, 0.0, 0.0)); // Dark cell
        }
        
        DigitClassifier.ClassificationResult[] results = classifier.classifyBatch(cells);
        
        assertNotNull(results);
        assertEquals(numCells, results.length);
        
        for (int i = 0; i < numCells; i++) {
            assertNotNull(results[i]);
            assertEquals(i, results[i].getCellIndex());
            assertTrue(results[i].getDigit() >= 0 && results[i].getDigit() <= 9);
            cells[i].close();
        }
    }
    
    @Test
    void testClassificationResultEmpty() {
        DigitClassifier.ClassificationResult result = DigitClassifier.ClassificationResult.empty(5);
        
        assertNotNull(result);
        assertEquals(5, result.getCellIndex());
        assertEquals(0, result.getDigit());
        assertTrue(result.isEmpty());
        assertEquals(1.0, result.getConfidence(), 0.001);
    }
    
    @Test
    void testClassificationResultNonEmpty() {
        DigitClassifier.ClassificationResult result = 
            new DigitClassifier.ClassificationResult(10, 7, 0.95);
        
        assertNotNull(result);
        assertEquals(10, result.getCellIndex());
        assertEquals(7, result.getDigit());
        assertFalse(result.isEmpty());
        assertEquals(0.95, result.getConfidence(), 0.001);
    }
    
    @Test
    void testPlaceholderReturnsEmpty() {
        // The placeholder implementation should return empty cells (0)
        Mat cell = new Mat(28, 28, opencv_core.CV_32F);
        cell.put(new Scalar(0.5, 0.0, 0.0, 0.0));
        
        Mat[] cells = new Mat[] { cell };
        DigitClassifier.ClassificationResult[] results = classifier.classifyBatch(cells);
        
        // Current placeholder always returns 0 (empty)
        assertEquals(0, results[0].getDigit());
        assertTrue(results[0].isEmpty());
        
        cell.close();
    }
}
