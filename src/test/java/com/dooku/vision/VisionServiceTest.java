package com.dooku.vision;

import com.dooku.vision.model.GridSearchResult;
import com.dooku.vision.model.GridSearchResult.Status;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.global.opencv_core;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class VisionServiceTest {
    
    private VisionService visionService;
    
    @BeforeEach
    void setUp() {
        visionService = new VisionService();
    }
    
    @AfterEach
    void tearDown() {
        if (visionService != null) {
            visionService.shutdown();
        }
    }
    
    @Test
    void testFindGridWithNull() throws ExecutionException, InterruptedException {
        CompletableFuture<GridSearchResult> future = visionService.findGrid(null);
        GridSearchResult result = future.get();
        
        assertEquals(Status.NO_GRID, result.status());
        assertNull(result.corners());
    }
    
    @Test
    void testFindGridWithEmptyMat() throws ExecutionException, InterruptedException {
        Mat emptyMat = new Mat();
        CompletableFuture<GridSearchResult> future = visionService.findGrid(emptyMat);
        GridSearchResult result = future.get();
        
        assertEquals(Status.NO_GRID, result.status());
        assertNull(result.corners());
        emptyMat.close();
    }
    
    @Test
    void testSegmentGridReturns81Cells() {
        // Create a 450x450 test image (the expected size after warping)
        Mat testGrid = new Mat(450, 450, opencv_core.CV_8UC1);
        
        Mat[] cells = visionService.segmentGrid(testGrid);
        
        assertEquals(81, cells.length);
        
        // Verify each cell is 50x50 (450/9)
        for (Mat cell : cells) {
            assertNotNull(cell);
            assertEquals(50, cell.rows());
            assertEquals(50, cell.cols());
            cell.close();
        }
        
        testGrid.close();
    }
    
    @Test
    void testSegmentGridWithNull() {
        Mat[] cells = visionService.segmentGrid(null);
        assertEquals(0, cells.length);
    }
    
    @Test
    void testWarpGridWithNull() {
        Mat result = visionService.warpGrid(null, null);
        assertTrue(result.empty());
        result.close();
    }
    
    @Test
    void testWarpGridWithValidInput() {
        Mat frame = new Mat(480, 640, opencv_core.CV_8UC3);
        Point[] corners = new Point[]{
            new Point(100, 100),
            new Point(300, 100),
            new Point(300, 300),
            new Point(100, 300)
        };
        
        Mat warped = visionService.warpGrid(frame, corners);
        
        assertNotNull(warped);
        assertEquals(450, warped.rows());
        assertEquals(450, warped.cols());
        
        warped.close();
        frame.close();
    }
    
    @Test
    void testRecognizeBoardReturns9x9Array() throws ExecutionException, InterruptedException {
        Mat testGrid = new Mat(450, 450, opencv_core.CV_8UC1);
        
        CompletableFuture<int[][]> future = visionService.recognizeBoard(testGrid);
        int[][] board = future.get();
        
        assertNotNull(board);
        assertEquals(9, board.length);
        for (int[] row : board) {
            assertEquals(9, row.length);
            // Since classifier returns 0, all values should be 0
            for (int val : row) {
                assertEquals(0, val);
            }
        }
        
        testGrid.close();
    }
}
