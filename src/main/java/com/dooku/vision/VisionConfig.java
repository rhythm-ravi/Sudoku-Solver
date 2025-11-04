package com.dooku.vision;

import java.util.prefs.Preferences;

/**
 * Configuration settings for the vision recognition system.
 * Uses Java Preferences API to persist settings.
 */
public class VisionConfig {
    
    private static final Preferences prefs = Preferences.userNodeForPackage(VisionConfig.class);
    
    // Configuration keys
    private static final String KEY_GRID_SIZE = "vision.gridSize";
    private static final String KEY_FRAME_INTERVAL = "vision.frameInterval";
    private static final String KEY_CONSENSUS_FRAMES = "vision.consensusFrames";
    private static final String KEY_POSITION_TOLERANCE = "vision.positionTolerance";
    private static final String KEY_DEBUG_MODE = "vision.debugMode";
    
    // Default values
    private static final int DEFAULT_GRID_SIZE = 9;
    private static final long DEFAULT_FRAME_INTERVAL = 100; // ms
    private static final int DEFAULT_CONSENSUS_FRAMES = 5;
    private static final double DEFAULT_POSITION_TOLERANCE = 10.0; // pixels
    private static final boolean DEFAULT_DEBUG_MODE = false;
    
    /**
     * Get the grid size setting (4, 6, 9, 12, 16).
     */
    public static int getGridSize() {
        return prefs.getInt(KEY_GRID_SIZE, DEFAULT_GRID_SIZE);
    }
    
    /**
     * Set the grid size.
     */
    public static void setGridSize(int gridSize) {
        if (gridSize < 4 || gridSize > 16) {
            throw new IllegalArgumentException("Grid size must be between 4 and 16");
        }
        prefs.putInt(KEY_GRID_SIZE, gridSize);
    }
    
    /**
     * Get the frame processing interval in milliseconds.
     * Controls how often frames are processed (lower = more frequent, higher CPU usage).
     */
    public static long getFrameInterval() {
        return prefs.getLong(KEY_FRAME_INTERVAL, DEFAULT_FRAME_INTERVAL);
    }
    
    /**
     * Set the frame processing interval.
     */
    public static void setFrameInterval(long intervalMs) {
        if (intervalMs < 50 || intervalMs > 1000) {
            throw new IllegalArgumentException("Frame interval must be between 50 and 1000 ms");
        }
        prefs.putLong(KEY_FRAME_INTERVAL, intervalMs);
    }
    
    /**
     * Get the number of consecutive frames required for consensus.
     */
    public static int getConsensusFrames() {
        return prefs.getInt(KEY_CONSENSUS_FRAMES, DEFAULT_CONSENSUS_FRAMES);
    }
    
    /**
     * Set the number of consensus frames.
     */
    public static void setConsensusFrames(int frames) {
        if (frames < 1 || frames > 20) {
            throw new IllegalArgumentException("Consensus frames must be between 1 and 20");
        }
        prefs.putInt(KEY_CONSENSUS_FRAMES, frames);
    }
    
    /**
     * Get the position tolerance in pixels.
     * Maximum allowed movement between frames to maintain stability.
     */
    public static double getPositionTolerance() {
        return prefs.getDouble(KEY_POSITION_TOLERANCE, DEFAULT_POSITION_TOLERANCE);
    }
    
    /**
     * Set the position tolerance.
     */
    public static void setPositionTolerance(double tolerancePx) {
        if (tolerancePx < 1.0 || tolerancePx > 100.0) {
            throw new IllegalArgumentException("Position tolerance must be between 1 and 100 pixels");
        }
        prefs.putDouble(KEY_POSITION_TOLERANCE, tolerancePx);
    }
    
    /**
     * Check if debug mode is enabled.
     * When enabled, detailed logging and frame saving may be performed.
     */
    public static boolean isDebugMode() {
        return prefs.getBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE);
    }
    
    /**
     * Set debug mode.
     */
    public static void setDebugMode(boolean enabled) {
        prefs.putBoolean(KEY_DEBUG_MODE, enabled);
    }
    
    /**
     * Reset all settings to defaults.
     */
    public static void resetToDefaults() {
        prefs.putInt(KEY_GRID_SIZE, DEFAULT_GRID_SIZE);
        prefs.putLong(KEY_FRAME_INTERVAL, DEFAULT_FRAME_INTERVAL);
        prefs.putInt(KEY_CONSENSUS_FRAMES, DEFAULT_CONSENSUS_FRAMES);
        prefs.putDouble(KEY_POSITION_TOLERANCE, DEFAULT_POSITION_TOLERANCE);
        prefs.putBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE);
    }
    
    /**
     * Get a summary of current settings.
     */
    public static String getSettingsSummary() {
        return String.format(
            "Vision Settings:\n" +
            "  Grid Size: %dx%d\n" +
            "  Frame Interval: %d ms\n" +
            "  Consensus Frames: %d\n" +
            "  Position Tolerance: %.1f px\n" +
            "  Debug Mode: %s",
            getGridSize(), getGridSize(),
            getFrameInterval(),
            getConsensusFrames(),
            getPositionTolerance(),
            isDebugMode() ? "ON" : "OFF"
        );
    }
}
