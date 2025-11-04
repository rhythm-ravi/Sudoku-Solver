package com.dooku.vision.model;

import com.dooku.vision.model.GridSearchResult.Status;
import org.bytedeco.opencv.opencv_core.Point;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GridSearchResultTest {
    
    @Test
    void testNoGridWithNullCorners() {
        GridSearchResult result = new GridSearchResult(Status.NO_GRID, null);
        assertEquals(Status.NO_GRID, result.status());
        assertNull(result.corners());
    }
    
    @Test
    void testStableWithCorners() {
        Point[] corners = new Point[]{
            new Point(0, 0),
            new Point(100, 0),
            new Point(100, 100),
            new Point(0, 100)
        };
        GridSearchResult result = new GridSearchResult(Status.STABLE, corners);
        assertEquals(Status.STABLE, result.status());
        assertNotNull(result.corners());
        assertEquals(4, result.corners().length);
    }
    
    @Test
    void testUnstableWithCorners() {
        Point[] corners = new Point[]{
            new Point(10, 10),
            new Point(110, 10),
            new Point(110, 110),
            new Point(10, 110)
        };
        GridSearchResult result = new GridSearchResult(Status.UNSTABLE, corners);
        assertEquals(Status.UNSTABLE, result.status());
        assertNotNull(result.corners());
    }
    
    @Test
    void testNoGridWithCornersThrowsException() {
        Point[] corners = new Point[]{
            new Point(0, 0),
            new Point(100, 0),
            new Point(100, 100),
            new Point(0, 100)
        };
        assertThrows(IllegalArgumentException.class, () -> {
            new GridSearchResult(Status.NO_GRID, corners);
        });
    }
    
    @Test
    void testStableWithNullCornersThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GridSearchResult(Status.STABLE, null);
        });
    }
    
    @Test
    void testInvalidCornersLengthThrowsException() {
        Point[] corners = new Point[]{
            new Point(0, 0),
            new Point(100, 0),
            new Point(100, 100)
        };
        assertThrows(IllegalArgumentException.class, () -> {
            new GridSearchResult(Status.STABLE, corners);
        });
    }
}
