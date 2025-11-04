package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Main orchestrator for vision recognition system.
 * Runs on a separate thread pool and coordinates preprocessing, detection, and classification.
 * Implements state machine: SCANNING → DETECTED → VERIFYING → CONFIRMED → COMPLETED
 */
public class VisionRecognitionService {
    
    private static final Logger logger = LoggerFactory.getLogger(VisionRecognitionService.class);
    
    // Configuration
    private final int gridSize;
    private final long frameIntervalMs;
    
    // Components
    private final GridDetector gridDetector;
    private final GridSegmenter gridSegmenter;
    private final DigitClassifier digitClassifier;
    private final FrameConsensusManager consensusManager;
    
    // Threading
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService processingExecutor;
    private volatile boolean running = false;
    private volatile boolean processing = false;
    
    // State
    private volatile RecognitionResult currentResult;
    private volatile Consumer<RecognitionResult> resultCallback;
    private volatile Point[] currentCorners = null;
    
    /**
     * Create a vision recognition service with specified configuration.
     * 
     * @param gridSize The size of the Sudoku grid (4, 6, 9, 12, 16)
     * @param frameIntervalMs Interval between frame processing in milliseconds
     * @param consensusFrames Number of frames required for consensus
     */
    public VisionRecognitionService(int gridSize, long frameIntervalMs, int consensusFrames) {
        if (gridSize < 4 || gridSize > 16) {
            throw new IllegalArgumentException("Grid size must be between 4 and 16");
        }
        
        this.gridSize = gridSize;
        this.frameIntervalMs = frameIntervalMs;
        
        // Initialize components
        this.gridDetector = new GridDetector();
        this.gridSegmenter = new GridSegmenter(gridSize);
        this.digitClassifier = new DigitClassifier();
        this.consensusManager = new FrameConsensusManager(consensusFrames, 10.0);
        
        // Initialize state
        this.currentResult = RecognitionResult.scanning("Ready to scan");
        
        logger.info("VisionRecognitionService created: gridSize={}, frameInterval={}ms, consensusFrames={}",
                   gridSize, frameIntervalMs, consensusFrames);
    }
    
    /**
     * Create a vision recognition service with default configuration (9x9 grid).
     */
    public VisionRecognitionService() {
        this(9, 100, 5); // 9x9 grid, 100ms interval (10 FPS), 5 consensus frames
    }
    
    /**
     * Start the recognition service.
     * This begins processing frames at the configured interval.
     */
    public void start() {
        if (running) {
            logger.warn("Service already running");
            return;
        }
        
        running = true;
        
        // Create thread pools
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VisionRecognition-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        processingExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "VisionRecognition-Processor");
                t.setDaemon(true);
                return t;
            }
        );
        
        logger.info("VisionRecognitionService started");
        updateResult(RecognitionResult.scanning("Scanning for grid..."));
    }
    
    /**
     * Stop the recognition service and clean up resources.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // Shutdown executors
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                scheduledExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (processingExecutor != null) {
            processingExecutor.shutdown();
            try {
                processingExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Cleanup components
        digitClassifier.shutdown();
        
        logger.info("VisionRecognitionService stopped");
    }
    
    /**
     * Process a single frame.
     * This should be called periodically with new camera frames.
     * 
     * @param frame The camera frame to process
     */
    public void processFrame(Mat frame) {
        if (!running || processing || frame == null || frame.empty()) {
            return;
        }
        
        processing = true;
        
        // Clone frame for processing
        Mat frameCopy = frame.clone();
        
        // Submit processing task
        CompletableFuture.supplyAsync(() -> processFrameInternal(frameCopy), processingExecutor)
            .thenAccept(result -> {
                frameCopy.close();
                processing = false;
            })
            .exceptionally(e -> {
                logger.error("Error processing frame", e);
                frameCopy.close();
                processing = false;
                updateResult(RecognitionResult.error("Processing error: " + e.getMessage()));
                return null;
            });
    }
    
    /**
     * Internal frame processing logic.
     */
    private RecognitionResult processFrameInternal(Mat frame) {
        // Step 1: Detect grid
        GridDetectionResult detection = gridDetector.detectGrid(frame);
        
        if (!detection.isDetected()) {
            // No grid detected
            currentCorners = null;
            // processFrame will call  reset and return correct message and status, so here we simply reset corners
        }
        
        // Step 2: Check consensus for grid stability
        Point[] corners = detection.getCorners();
        currentCorners = corners;
        
        FrameConsensusManager.ConsensusState consensusState = consensusManager.processFrame(corners);
        
        switch (consensusState.getStatus()) {
            case UNSTABLE:
                updateResult(RecognitionResult.detected(consensusState.getMessage()));
                return currentResult;
                
            case VERIFYING:
                updateResult(RecognitionResult.verifying(
                    "Verifying... " + consensusState.getFrameCount() + "/" + 
                    consensusManager.getRequiredFrames(),
                    consensusState.getFrameCount()
                ));
                return currentResult;
                
            case READY:
                // Grid is stable and confirmed, extract digits
                return extractDigits(detection.getWarpedGrid());
        }
        
        return currentResult;
    }
    
    /**
     * Extract digits from the confirmed grid.
     */
    private RecognitionResult extractDigits(Mat warpedGrid) {
        updateResult(RecognitionResult.confirmed("Processing grid..."));
        
        try {
            // Step 3: Segment grid into cells
            Mat[] cells = gridSegmenter.segmentGrid(warpedGrid);
            
            if (cells.length == 0) {
                return RecognitionResult.error("Failed to segment grid");
            }
            
            // Step 4: Classify digits in parallel
            DigitClassifier.ClassificationResult[] classifications = 
                digitClassifier.classifyBatch(cells);
            
            // Convert to 2D board array
            int[][] board = new int[gridSize][gridSize];
            for (DigitClassifier.ClassificationResult result : classifications) {
                int row = result.getCellIndex() / gridSize;
                int col = result.getCellIndex() % gridSize;
                board[row][col] = result.getDigit();
            }
            
            // Add to consensus history
            consensusManager.addBoardResult(board);
            
            // Check if we have enough board results for consensus
            FrameConsensusManager.BoardConsensus consensus = consensusManager.getConsensusBoard();
            
            if (consensus != null && consensus.getConfidence() > 0.5) {
                // We have a good consensus
                RecognitionResult completed = RecognitionResult.completed(
                    consensus.getBoard(),
                    consensus.getConfidence()
                );
                updateResult(completed);
                
                // Stop processing after successful recognition
                stop();
                
                return completed;
            }
            
            // Clean up cells
            for (Mat cell : cells) {
                if (cell != null) {
                    cell.close();
                }
            }
            
            // Continue verifying
            return RecognitionResult.verifying(
                "Analyzing digits...",
                consensusManager.getGoodFrameCount()
            );
            
        } catch (Exception e) {
            logger.error("Error extracting digits", e);
            return RecognitionResult.error("Failed to recognize digits: " + e.getMessage());
        }
    }
    
    /**
     * Update the current result and notify callback.
     */
    private void updateResult(RecognitionResult result) {
        this.currentResult = result;
        
        if (resultCallback != null) {
            try {
                resultCallback.accept(result);
            } catch (Exception e) {
                logger.error("Error in result callback", e);
            }
        }
    }
    
    /**
     * Set a callback to be notified of recognition results.
     * 
     * @param callback The callback function
     */
    public void setResultCallback(Consumer<RecognitionResult> callback) {
        this.resultCallback = callback;
    }
    
    /**
     * Get the current recognition result.
     */
    public RecognitionResult getCurrentResult() {
        return currentResult;
    }
    
    /**
     * Get the current detected grid corners (for UI overlay).
     */
    public Point[] getCurrentCorners() {
        return currentCorners;
    }
    
    /**
     * Check if the service is currently running.
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Check if the service is currently processing a frame.
     */
    public boolean isProcessing() {
        return processing;
    }
    
    /**
     * Reset the service state without stopping it.
     */
    public void reset() {
        consensusManager.reset();
        currentCorners = null;
        updateResult(RecognitionResult.scanning("Scanning for grid..."));
        logger.info("VisionRecognitionService reset");
    }
}
