package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements multi-frame verification logic to ensure stable grid detection
 * and consensus-based digit recognition across multiple frames.
 */
public class FrameConsensusManager {
    
    private static final Logger logger = LoggerFactory.getLogger(FrameConsensusManager.class);
    
    // Configuration
    private final int requiredFrames;
    private final double positionTolerancePx;
    
    // State tracking
    private int goodFrameCount = 0;
    private Point[] lastDetectedCorners = null;
    private final List<int[][]> boardHistory = new ArrayList<>();
    
    /**
     * Create a frame consensus manager.
     * 
     * @param requiredFrames Number of consecutive stable frames required for confirmation
     * @param positionTolerancePx Maximum allowed movement in pixels between frames
     */
    public FrameConsensusManager(int requiredFrames, double positionTolerancePx) {
        if (requiredFrames < 1) {
            throw new IllegalArgumentException("Required frames must be at least 1");
        }
        this.requiredFrames = requiredFrames;
        this.positionTolerancePx = positionTolerancePx;
    }
    
    /**
     * Create a frame consensus manager with default settings.
     */
    public FrameConsensusManager() {
        this(5, 10.0); // Default: 5 frames, 10 pixels tolerance
    }
    
    /**
     * Process a frame with detected grid.
     * 
     * @param corners The detected grid corners
     * @return Consensus state indicating whether to continue or extract digits
     */
    public ConsensusState processFrame(Point[] corners) {
        if (corners == null || corners.length != 4) {
            reset();
            return new ConsensusState(
                ConsensusState.Status.UNSTABLE,
                0,
                "No grid detected"
            );
        }
        
        // Check if grid position is consistent with previous frame
        if (lastDetectedCorners == null) {
            // First detection
            lastDetectedCorners = corners;
            goodFrameCount = 1;
            logger.debug("First grid detection");
            return new ConsensusState(
                ConsensusState.Status.VERIFYING,
                goodFrameCount,
                "Hold steady..."
            );
        }
        
        // Check position consistency
        double maxMovement = calculateMaxMovement(lastDetectedCorners, corners);
        
        if (maxMovement > positionTolerancePx) {
            // Grid moved too much
            logger.debug("Grid moved {} pixels, resetting", maxMovement);
            lastDetectedCorners = corners;
            goodFrameCount = 1;
            return new ConsensusState(
                ConsensusState.Status.UNSTABLE,
                goodFrameCount,
                "Hold camera steady!"
            );
        }
        
        // Grid is stable
        lastDetectedCorners = corners;
        goodFrameCount++;
        
        logger.debug("Stable frame {}/{}", goodFrameCount, requiredFrames);
        
        if (goodFrameCount >= requiredFrames) {
            return new ConsensusState(
                ConsensusState.Status.READY,
                goodFrameCount,
                "Grid confirmed"
            );
        }
        
        return new ConsensusState(
            ConsensusState.Status.VERIFYING,
            goodFrameCount,
            "Hold steady..."
        );
    }
    
    /**
     * Add a recognized board to the history for consensus.
     * 
     * @param board The recognized board (2D array)
     */
    public void addBoardResult(int[][] board) {
        if (board != null) {
            boardHistory.add(board);
            logger.debug("Added board to history, size: {}", boardHistory.size());
        }
    }
    
    /**
     * Get consensus board by majority voting across all stored results.
     * 
     * @return Consensus board with most probable digits, or null if no history
     */
    public BoardConsensus getConsensusBoard() {
        if (boardHistory.isEmpty()) {
            return null;
        }
        
        int size = boardHistory.get(0).length;
        int[][] consensusBoard = new int[size][size];
        double totalConfidence = 0.0;
        
        // For each cell, find the most common digit
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                Map<Integer, Integer> voteCounts = new HashMap<>();
                
                // Count votes for each digit
                for (int[][] board : boardHistory) {
                    int digit = board[row][col];
                    voteCounts.put(digit, voteCounts.getOrDefault(digit, 0) + 1);
                }
                
                // Find digit with most votes
                int winningDigit = 0;
                int maxVotes = 0;
                for (Map.Entry<Integer, Integer> entry : voteCounts.entrySet()) {
                    if (entry.getValue() > maxVotes) {
                        maxVotes = entry.getValue();
                        winningDigit = entry.getKey();
                    }
                }
                
                consensusBoard[row][col] = winningDigit;
                
                // Calculate confidence for this cell
                double cellConfidence = (double) maxVotes / boardHistory.size();
                totalConfidence += cellConfidence;
            }
        }
        
        // Average confidence across all cells
        double avgConfidence = totalConfidence / (size * size);
        
        logger.info("Consensus board computed with confidence: {}", avgConfidence);
        return new BoardConsensus(consensusBoard, avgConfidence);
    }
    
    /**
     * Calculate maximum movement between two sets of corners.
     */
    private double calculateMaxMovement(Point[] corners1, Point[] corners2) {
        double maxDistance = 0.0;
        
        for (int i = 0; i < 4; i++) {
            double dx = corners1[i].x() - corners2[i].x();
            double dy = corners1[i].y() - corners2[i].y();
            double distance = Math.sqrt(dx * dx + dy * dy);
            maxDistance = Math.max(maxDistance, distance);
        }
        
        return maxDistance;
    }
    
    /**
     * Reset the consensus state.
     */
    public void reset() {
        goodFrameCount = 0;
        lastDetectedCorners = null;
        boardHistory.clear();
        logger.debug("Consensus manager reset");
    }
    
    /**
     * Get the current frame count.
     */
    public int getGoodFrameCount() {
        return goodFrameCount;
    }
    
    /**
     * Get the required frame count.
     */
    public int getRequiredFrames() {
        return requiredFrames;
    }
    
    /**
     * State of the consensus process.
     */
    public static class ConsensusState {
        public enum Status {
            UNSTABLE,   // Grid not stable (moving or no detection)
            VERIFYING,  // Counting stable frames
            READY       // Ready for digit extraction
        }
        
        private final Status status;
        private final int frameCount;
        private final String message;
        
        public ConsensusState(Status status, int frameCount, String message) {
            this.status = status;
            this.frameCount = frameCount;
            this.message = message;
        }
        
        public Status getStatus() {
            return status;
        }
        
        public int getFrameCount() {
            return frameCount;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Result of board consensus with confidence score.
     */
    public static class BoardConsensus {
        private final int[][] board;
        private final double confidence;
        
        public BoardConsensus(int[][] board, double confidence) {
            this.board = board;
            this.confidence = confidence;
        }
        
        public int[][] getBoard() {
            return board;
        }
        
        public double getConfidence() {
            return confidence;
        }
    }
}
