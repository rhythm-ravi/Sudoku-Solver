package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameConsensusManagerTest {
    
    private FrameConsensusManager consensusManager;
    
    @BeforeEach
    void setUp() {
        consensusManager = new FrameConsensusManager(5, 10.0);
    }
    
    @Test
    void testProcessFrameWithNull() {
        FrameConsensusManager.ConsensusState state = consensusManager.processFrame(null);
        
        assertNotNull(state);
        assertEquals(FrameConsensusManager.ConsensusState.Status.UNSTABLE, state.getStatus());
        assertEquals(0, state.getFrameCount());
    }
    
    @Test
    void testFirstFrameDetection() {
        Point[] corners = createSquareCorners(100, 100, 300);
        
        FrameConsensusManager.ConsensusState state = consensusManager.processFrame(corners);
        
        assertNotNull(state);
        assertEquals(FrameConsensusManager.ConsensusState.Status.VERIFYING, state.getStatus());
        assertEquals(1, state.getFrameCount());
    }
    
    @Test
    void testStableFrameProgression() {
        Point[] corners = createSquareCorners(100, 100, 300);
        
        // Process same corners multiple times
        for (int i = 1; i < 5; i++) {
            FrameConsensusManager.ConsensusState state = consensusManager.processFrame(corners);
            assertEquals(FrameConsensusManager.ConsensusState.Status.VERIFYING, state.getStatus());
            assertEquals(i, state.getFrameCount());
        }
        
        // 5th frame should be ready
        FrameConsensusManager.ConsensusState finalState = consensusManager.processFrame(corners);
        assertEquals(FrameConsensusManager.ConsensusState.Status.READY, finalState.getStatus());
        assertEquals(5, finalState.getFrameCount());
    }
    
    @Test
    void testUnstableFrameResetsCount() {
        Point[] corners1 = createSquareCorners(100, 100, 300);
        
        // Process 3 stable frames
        consensusManager.processFrame(corners1);
        consensusManager.processFrame(corners1);
        consensusManager.processFrame(corners1);
        assertEquals(3, consensusManager.getGoodFrameCount());
        
        // Move the grid significantly
        Point[] corners2 = createSquareCorners(150, 150, 300);
        FrameConsensusManager.ConsensusState state = consensusManager.processFrame(corners2);
        
        assertEquals(FrameConsensusManager.ConsensusState.Status.UNSTABLE, state.getStatus());
        // Should reset, but new position starts at 1
        assertEquals(1, consensusManager.getGoodFrameCount());
    }
    
    @Test
    void testSmallMovementMaintainsStability() {
        Point[] corners1 = createSquareCorners(100, 100, 300);
        consensusManager.processFrame(corners1);
        
        // Move slightly (within tolerance)
        Point[] corners2 = createSquareCorners(102, 102, 300);
        FrameConsensusManager.ConsensusState state = consensusManager.processFrame(corners2);
        
        assertEquals(FrameConsensusManager.ConsensusState.Status.VERIFYING, state.getStatus());
        assertEquals(2, state.getFrameCount());
    }
    
    @Test
    void testBoardConsensus() {
        // Add some board results
        int[][] board1 = createTestBoard(9, 1);
        int[][] board2 = createTestBoard(9, 1);
        int[][] board3 = createTestBoard(9, 2);
        
        consensusManager.addBoardResult(board1);
        consensusManager.addBoardResult(board2);
        consensusManager.addBoardResult(board3);
        
        FrameConsensusManager.BoardConsensus consensus = consensusManager.getConsensusBoard();
        
        assertNotNull(consensus);
        assertNotNull(consensus.getBoard());
        assertEquals(9, consensus.getBoard().length);
        assertTrue(consensus.getConfidence() > 0.0);
        assertTrue(consensus.getConfidence() <= 1.0);
    }
    
    @Test
    void testMajorityVotingInConsensus() {
        int[][] board1 = new int[9][9];
        int[][] board2 = new int[9][9];
        int[][] board3 = new int[9][9];
        
        // Set first cell to different values
        board1[0][0] = 5;
        board2[0][0] = 5;
        board3[0][0] = 3;
        
        consensusManager.addBoardResult(board1);
        consensusManager.addBoardResult(board2);
        consensusManager.addBoardResult(board3);
        
        FrameConsensusManager.BoardConsensus consensus = consensusManager.getConsensusBoard();
        
        assertNotNull(consensus);
        // Majority vote should pick 5 (2 votes vs 1 vote)
        assertEquals(5, consensus.getBoard()[0][0]);
    }
    
    @Test
    void testReset() {
        Point[] corners = createSquareCorners(100, 100, 300);
        
        consensusManager.processFrame(corners);
        consensusManager.processFrame(corners);
        assertEquals(2, consensusManager.getGoodFrameCount());
        
        consensusManager.reset();
        
        assertEquals(0, consensusManager.getGoodFrameCount());
        assertNull(consensusManager.getConsensusBoard());
    }
    
    @Test
    void testCustomConfiguration() {
        FrameConsensusManager custom = new FrameConsensusManager(3, 5.0);
        
        assertEquals(3, custom.getRequiredFrames());
        
        Point[] corners = createSquareCorners(100, 100, 300);
        
        // Should be ready after 3 frames
        custom.processFrame(corners);
        custom.processFrame(corners);
        FrameConsensusManager.ConsensusState state = custom.processFrame(corners);
        
        assertEquals(FrameConsensusManager.ConsensusState.Status.READY, state.getStatus());
    }
    
    @Test
    void testInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new FrameConsensusManager(0, 10.0));
    }
    
    private Point[] createSquareCorners(int x, int y, int size) {
        return new Point[] {
            new Point(x, y),
            new Point(x + size, y),
            new Point(x + size, y + size),
            new Point(x, y + size)
        };
    }
    
    private int[][] createTestBoard(int size, int fillValue) {
        int[][] board = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = fillValue;
            }
        }
        return board;
    }
}
