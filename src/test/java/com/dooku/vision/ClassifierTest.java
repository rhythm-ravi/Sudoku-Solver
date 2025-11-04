package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassifierTest {
    
    @Test
    void testClassifyReturnsZero() {
        Classifier classifier = new Classifier();
        Mat testMat = new Mat(28, 28, 0);
        
        int result = classifier.classify(testMat);
        
        assertEquals(0, result);
        testMat.close();
    }
    
    @Test
    void testClassifyWithNull() {
        Classifier classifier = new Classifier();
        
        int result = classifier.classify(null);
        
        assertEquals(0, result);
    }
    
    @Test
    void testClassifyWithEmptyMat() {
        Classifier classifier = new Classifier();
        Mat emptyMat = new Mat();
        
        int result = classifier.classify(emptyMat);
        
        assertEquals(0, result);
        emptyMat.close();
    }
}
