package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.Mat;

/**
 * Dummy classifier for digit recognition.
 * In the future, this will use a TFLite model to classify digits.
 */
public class Classifier {
    
    /**
     * Classify a cell image and return the predicted digit.
     * This is a dummy implementation that simulates a short delay and returns 0.
     * 
     * @param cellImage The preprocessed cell image
     * @return The predicted digit (0-9, where 0 means empty cell)
     */
    public int classify(Mat cellImage) {
        if (cellImage == null || cellImage.empty()) {
            return 0;
        }
        
        // Simulate a short processing delay
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
        
        // Always return 0 (empty cell) for now
        return 0;
    }
}
