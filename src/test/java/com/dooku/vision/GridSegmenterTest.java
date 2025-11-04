package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridSegmenterTest {
    
    @Test
    void testSegmentGrid9x9() {
        GridSegmenter segmenter = new GridSegmenter(9);
        
        // Create a 450x450 test image
        Mat testGrid = new Mat(450, 450, opencv_core.CV_8UC1);
        testGrid.put(new Scalar(128.0, 0.0, 0.0, 0.0)); // Gray image
        
        Mat[] cells = segmenter.segmentGrid(testGrid);
        
        assertNotNull(cells);
        assertEquals(81, cells.length); // 9x9 = 81 cells
        
        // Verify each cell is 28x28 (preprocessed size)
        for (Mat cell : cells) {
            assertNotNull(cell);
            assertEquals(28, cell.rows());
            assertEquals(28, cell.cols());
            cell.close();
        }
        
        testGrid.close();
    }
    
    @Test
    void testSegmentGrid4x4() {
        GridSegmenter segmenter = new GridSegmenter(4);
        
        Mat testGrid = new Mat(400, 400, opencv_core.CV_8UC1);
        testGrid.put(new Scalar(128.0, 0.0, 0.0, 0.0));
        
        Mat[] cells = segmenter.segmentGrid(testGrid);
        
        assertNotNull(cells);
        assertEquals(16, cells.length); // 4x4 = 16 cells
        
        for (Mat cell : cells) {
            assertNotNull(cell);
            assertEquals(28, cell.rows());
            assertEquals(28, cell.cols());
            cell.close();
        }
        
        testGrid.close();
    }
    
    @Test
    void testSegmentGrid6x6() {
        GridSegmenter segmenter = new GridSegmenter(6);
        
        Mat testGrid = new Mat(450, 450, opencv_core.CV_8UC1);
        testGrid.put(new Scalar(128.0, 0.0, 0.0, 0.0));
        
        Mat[] cells = segmenter.segmentGrid(testGrid);
        
        assertNotNull(cells);
        assertEquals(36, cells.length); // 6x6 = 36 cells
        
        for (Mat cell : cells) {
            assertNotNull(cell);
            cell.close();
        }
        
        testGrid.close();
    }
    
    @Test
    void testSegmentGridWithNull() {
        GridSegmenter segmenter = new GridSegmenter(9);
        
        Mat[] cells = segmenter.segmentGrid(null);
        
        assertNotNull(cells);
        assertEquals(0, cells.length);
    }
    
    @Test
    void testSegmentGridWithEmptyMat() {
        GridSegmenter segmenter = new GridSegmenter(9);
        Mat emptyMat = new Mat();
        
        Mat[] cells = segmenter.segmentGrid(emptyMat);
        
        assertNotNull(cells);
        assertEquals(0, cells.length);
        
        emptyMat.close();
    }
    
    @Test
    void testInvalidGridSize() {
        assertThrows(IllegalArgumentException.class, () -> new GridSegmenter(3));
        assertThrows(IllegalArgumentException.class, () -> new GridSegmenter(17));
        assertThrows(IllegalArgumentException.class, () -> new GridSegmenter(0));
    }
    
    @Test
    void testGetGridSize() {
        GridSegmenter segmenter9 = new GridSegmenter(9);
        assertEquals(9, segmenter9.getGridSize());
        
        GridSegmenter segmenter12 = new GridSegmenter(12);
        assertEquals(12, segmenter12.getGridSize());
    }
}
