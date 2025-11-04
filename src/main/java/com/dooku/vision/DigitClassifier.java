package com.dooku.vision;

import ai.onnxruntime.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles digit recognition using ONNX Runtime model.
 * Supports parallel classification via ExecutorService.
 */
public class DigitClassifier {
    
    private static final Logger logger = LoggerFactory.getLogger(DigitClassifier.class);
    
    // Configuration
    private static final double EMPTY_CELL_THRESHOLD = 0.05; // Low variance/mean indicates empty cell
    private static final int INPUT_SIZE = 28; // 28x28 input for MNIST
    private static final int NUM_CLASSES = 10; // 0-9 digits
    private static final String MODEL_PATH = "/models/digit_classifier.onnx";
    
    // Thread pool for parallel classification
    private final ExecutorService executorService;
    
    // ONNX Runtime components
    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;
    private boolean modelLoaded = false;
    
    /**
     * Create a digit classifier and load the ONNX model.
     * Model loading is optional - if model is not found, classifier will work in placeholder mode.
     */
    public DigitClassifier() {
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        
        // Try to load the ONNX model
        loadModel();
    }
    
    /**
     * Load the ONNX model from resources.
     * If model is not found, the classifier will work in placeholder mode.
     */
    private void loadModel() {
        try {
            logger.debug("Attempting to load ONNX model from: {}", MODEL_PATH);
            
            // Try to get model as a resource
            InputStream modelStream = getClass().getResourceAsStream(MODEL_PATH);
            
            if (modelStream == null) {
                logger.warn("ONNX model file not found at: {}. Running in placeholder mode.", MODEL_PATH);
                logger.warn("To enable digit recognition, place digit_classifier.onnx in src/main/resources/models/");
                return;
            }
            
            // Read model bytes
            byte[] modelBytes = modelStream.readAllBytes();
            modelStream.close();
            
            logger.debug("Model file loaded, size: {} bytes", modelBytes.length);
            
            // Create ONNX Runtime environment
            ortEnvironment = OrtEnvironment.getEnvironment();
            logger.debug("OrtEnvironment created");
            
            // Create session options
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            
            // Load model into session
            ortSession = ortEnvironment.createSession(modelBytes, sessionOptions);
            sessionOptions.close();
            
            modelLoaded = true;
            
            logger.info("ONNX model loaded successfully");
            logger.debug("Model inputs: {}", ortSession.getInputNames());
            logger.debug("Model outputs: {}", ortSession.getOutputNames());
            
        } catch (OrtException e) {
            logger.error("Failed to load ONNX model due to OrtException", e);
            logger.warn("Running in placeholder mode without model");
            modelLoaded = false;
        } catch (IOException e) {
            logger.error("Failed to read ONNX model file", e);
            logger.warn("Running in placeholder mode without model");
            modelLoaded = false;
        } catch (Exception e) {
            logger.error("Unexpected error loading ONNX model", e);
            logger.warn("Running in placeholder mode without model");
            modelLoaded = false;
        }
    }
    
    /**
     * Classify multiple cells in parallel.
     * 
     * @param cells Array of preprocessed cell images (28x28, normalized)
     * @return Array of classification results
     */
    public ClassificationResult[] classifyBatch(Mat[] cells) {
        if (cells == null || cells.length == 0) {
            return new ClassificationResult[0];
        }
        
        logger.debug("Classifying batch of {} cells", cells.length);
        
        // Create tasks for parallel processing
        List<Future<ClassificationResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < cells.length; i++) {
            final int index = i;
            Future<ClassificationResult> future = executorService.submit(() -> 
                classifyCell(cells[index], index)
            );
            futures.add(future);
        }
        
        // Collect results
        ClassificationResult[] results = new ClassificationResult[cells.length];
        try {
            for (int i = 0; i < futures.size(); i++) {
                results[i] = futures.get(i).get();
            }
        } catch (Exception e) {
            logger.error("Error during batch classification", e);
            // Fill with empty results
            for (int i = 0; i < results.length; i++) {
                if (results[i] == null) {
                    results[i] = ClassificationResult.empty(i);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Classify a single cell.
     * 
     * @param cellImage Preprocessed cell image (28x28, normalized)
     * @param index Cell index for tracking
     * @return Classification result with digit and confidence
     */
    private ClassificationResult classifyCell(Mat cellImage, int index) {
        if (cellImage == null || cellImage.empty()) {
            return ClassificationResult.empty(index);
        }
        
        // Check if cell is empty (low variance/mean)
        if (isEmpty(cellImage)) {
            return ClassificationResult.empty(index);
        }
        
        // Use ONNX model if loaded, otherwise use placeholder
        if (modelLoaded && ortSession != null) {
            return classifyWithONNX(cellImage, index);
        } else {
            return classifyWithPlaceholder(cellImage, index);
        }
    }
    
    /**
     * Check if a cell is empty based on pixel statistics.
     */
    private boolean isEmpty(Mat cellImage) {
        Mat mean = new Mat();
        Mat stddev = new Mat();
        opencv_core.meanStdDev(cellImage, mean, stddev);
        
        double meanValue = mean.ptr(0).getDouble();
        double stddevValue = stddev.ptr(0).getDouble();
        
        mean.close();
        stddev.close();
        
        // If mean and variance are very low, cell is likely empty
        return meanValue < EMPTY_CELL_THRESHOLD && stddevValue < EMPTY_CELL_THRESHOLD;
    }
    
    /**
     * Classify using ONNX Runtime model.
     */
    private ClassificationResult classifyWithONNX(Mat cellImage, int index) {
        OnnxTensor inputTensor = null;
        try {
            // Convert Mat to float array for ONNX input
            float[] inputData = matToFloatArray(cellImage);
            
            // Create input tensor with shape [1, 1, 28, 28] (batch, channels, height, width)
            long[] shape = {1, 1, INPUT_SIZE, INPUT_SIZE};
            inputTensor = OnnxTensor.createTensor(ortEnvironment, FloatBuffer.wrap(inputData), shape);
            
            // Get input name from model
            String inputName = ortSession.getInputNames().iterator().next();
            
            // Run inference
            try (OrtSession.Result result = ortSession.run(Map.of(inputName, inputTensor))) {
                // Get output tensor
                Object outputValue = result.get(0).getValue();
                
                // Validate output type and structure
                if (!(outputValue instanceof float[][])) {
                    logger.error("Unexpected output type from ONNX model: {}", outputValue.getClass().getName());
                    return ClassificationResult.empty(index);
                }
                
                float[][] output = (float[][]) outputValue;
                
                // Validate output structure
                if (output.length == 0 || output[0].length == 0) {
                    logger.error("Empty output from ONNX model");
                    return ClassificationResult.empty(index);
                }
                
                // Find class with highest probability
                int predictedDigit = 0;
                float maxConfidence = output[0][0];
                
                int numClasses = Math.min(output[0].length, NUM_CLASSES);
                for (int i = 1; i < numClasses; i++) {
                    if (output[0][i] > maxConfidence) {
                        maxConfidence = output[0][i];
                        predictedDigit = i;
                    }
                }
                
                logger.debug("Cell {}: predicted={}, confidence={:.3f}", index, predictedDigit, maxConfidence);
                
                // If digit is 0 or confidence is too low, treat as empty
                if (predictedDigit == 0 || maxConfidence < 0.5) {
                    return ClassificationResult.empty(index);
                }
                
                return new ClassificationResult(index, predictedDigit, maxConfidence);
            }
            
        } catch (OrtException e) {
            logger.error("ONNX inference error for cell {}", index, e);
            return ClassificationResult.empty(index);
        } catch (Exception e) {
            logger.error("Unexpected error during classification for cell {}", index, e);
            return ClassificationResult.empty(index);
        } finally {
            // Clean up tensor
            if (inputTensor != null) {
                try {
                    inputTensor.close();
                } catch (Exception e) {
                    logger.debug("Error closing input tensor", e);
                }
            }
        }
    }
    
    /**
     * Placeholder classification implementation.
     * Used when model is not available.
     */
    private ClassificationResult classifyWithPlaceholder(Mat cellImage, int index) {
        // Placeholder: always return empty cell
        return ClassificationResult.empty(index);
    }
    
    /**
     * Convert Mat to float array for ONNX input.
     * Assumes input is already normalized 28x28 single-channel image.
     */
    private float[] matToFloatArray(Mat mat) {
        if (mat.type() != opencv_core.CV_32F && mat.type() != opencv_core.CV_32FC1) {
            throw new IllegalArgumentException("Mat must be of type CV_32F or CV_32FC1");
        }
        
        int height = mat.rows();
        int width = mat.cols();
        
        if (height != INPUT_SIZE || width != INPUT_SIZE) {
            throw new IllegalArgumentException(
                String.format("Mat must be %dx%d, got %dx%d", INPUT_SIZE, INPUT_SIZE, height, width)
            );
        }
        
        float[] array = new float[height * width];
        
        // Read pixel values - for CV_32F, each pixel is a 4-byte float
        int idx = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                array[idx++] = mat.ptr(i, j).getFloat(0);
            }
        }
        
        return array;
    }
    
    /**
     * Shutdown the executor service and close ONNX resources.
     */
    public void shutdown() {
        executorService.shutdown();
        
        // Close ONNX Runtime resources
        if (ortSession != null) {
            try {
                ortSession.close();
                logger.debug("OrtSession closed");
            } catch (Exception e) {
                logger.debug("Error closing OrtSession", e);
            }
            ortSession = null;
        }
        
        // Note: OrtEnvironment is a singleton and should not be closed manually
        // as other parts of the application may still be using it
        ortEnvironment = null;
        
        modelLoaded = false;
    }
    
    /**
     * Result of digit classification for a single cell.
     */
    public static class ClassificationResult {
        private final int cellIndex;
        private final int digit;
        private final double confidence;
        
        public ClassificationResult(int cellIndex, int digit, double confidence) {
            this.cellIndex = cellIndex;
            this.digit = digit;
            this.confidence = confidence;
        }
        
        public static ClassificationResult empty(int cellIndex) {
            return new ClassificationResult(cellIndex, 0, 1.0);
        }
        
        public int getCellIndex() {
            return cellIndex;
        }
        
        public int getDigit() {
            return digit;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public boolean isEmpty() {
            return digit == 0;
        }
    }
}
