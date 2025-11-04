package com.dooku.vision.model;

import org.bytedeco.opencv.opencv_core.Point;

/**
 * Result of a grid search operation.
 * @param status The status of the grid search (NO_GRID, UNSTABLE, STABLE)
 * @param corners The four corners of the detected grid (top-left, top-right, bottom-right, bottom-left), or null if no grid
 */
public record GridSearchResult(Status status, Point[] corners) {
    
    public enum Status {
        NO_GRID,    // No grid detected
        UNSTABLE,   // Grid detected but unstable (moving or changing)
        STABLE      // Grid detected and stable
    }
    
    public GridSearchResult {
        if (status == Status.NO_GRID && corners != null) {
            throw new IllegalArgumentException("NO_GRID status must have null corners");
        }
        if ((status == Status.UNSTABLE || status == Status.STABLE) && corners == null) {
            throw new IllegalArgumentException("UNSTABLE and STABLE status must have non-null corners");
        }
        if (corners != null && corners.length != 4) {
            throw new IllegalArgumentException("Corners array must contain exactly 4 points");
        }
    }
}
