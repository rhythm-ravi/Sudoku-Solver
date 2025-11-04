package com.dooku.vision;

/**
 * Result of the complete recognition process.
 * Contains the detected grid and classification results.
 */
public record RecognitionResult(
    RecognitionState state,
    int[][] board,
    String message,
    double confidence
) {
    
    /**
     * State of the recognition process
     */
    public enum RecognitionState {
        SCANNING,      // Looking for grid
        DETECTED,      // Grid detected but not verified
        VERIFYING,     // Multi-frame verification in progress
        CONFIRMED,     // Grid confirmed with consensus
        COMPLETED,     // Board recognition completed
        ERROR          // Error occurred
    }
    
    /**
     * Create a result for scanning state
     */
    public static RecognitionResult scanning(String message) {
        return new RecognitionResult(RecognitionState.SCANNING, null, message, 0.0);
    }
    
    /**
     * Create a result for detected state
     */
    public static RecognitionResult detected(String message) {
        return new RecognitionResult(RecognitionState.DETECTED, null, message, 0.0);
    }
    
    /**
     * Create a result for verifying state
     */
    public static RecognitionResult verifying(String message, int frameCount) {
        return new RecognitionResult(
            RecognitionState.VERIFYING, 
            null, 
            message + " (" + frameCount + ")", 
            0.0
        );
    }
    
    /**
     * Create a result for confirmed state
     */
    public static RecognitionResult confirmed(String message) {
        return new RecognitionResult(RecognitionState.CONFIRMED, null, message, 0.0);
    }
    
    /**
     * Create a result for completed state
     */
    public static RecognitionResult completed(int[][] board, double confidence) {
        return new RecognitionResult(
            RecognitionState.COMPLETED, 
            board, 
            "Board recognized successfully", 
            confidence
        );
    }
    
    /**
     * Create a result for error state
     */
    public static RecognitionResult error(String message) {
        return new RecognitionResult(RecognitionState.ERROR, null, message, 0.0);
    }
}
