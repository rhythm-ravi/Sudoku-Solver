package com.dooku.vision;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VisionRecognitionServiceTest {
    
    private VisionRecognitionService service;
    
    @BeforeEach
    void setUp() {
        service = new VisionRecognitionService(9, 100, 3); // Quick settings for testing
    }
    
    @AfterEach
    void tearDown() {
        if (service != null && service.isRunning()) {
            service.stop();
        }
    }
    
    @Test
    void testServiceCreation() {
        assertNotNull(service);
        assertFalse(service.isRunning());
        assertFalse(service.isProcessing());
    }
    
    @Test
    void testServiceStartStop() {
        service.start();
        assertTrue(service.isRunning());
        
        service.stop();
        assertFalse(service.isRunning());
    }
    
    @Test
    void testDoubleStart() {
        service.start();
        assertTrue(service.isRunning());
        
        // Second start should be ignored
        service.start();
        assertTrue(service.isRunning());
        
        service.stop();
    }
    
    @Test
    void testStopWithoutStart() {
        assertFalse(service.isRunning());
        
        // Should not throw exception
        service.stop();
        
        assertFalse(service.isRunning());
    }
    
    @Test
    void testProcessFrameWithNull() throws InterruptedException {
        service.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RecognitionResult> resultRef = new AtomicReference<>();
        
        service.setResultCallback(result -> {
            resultRef.set(result);
            latch.countDown();
        });
        
        service.processFrame(null);
        
        // Wait a bit for processing
        latch.await(500, TimeUnit.MILLISECONDS);
        
        // Should not crash, might not trigger callback for null frame
        assertFalse(service.isProcessing());
    }
    
    @Test
    void testProcessFrameWithEmptyMat() throws InterruptedException {
        service.start();
        
        Mat emptyMat = new Mat();
        
        CountDownLatch latch = new CountDownLatch(1);
        service.setResultCallback(result -> latch.countDown());
        
        service.processFrame(emptyMat);
        
        // Wait for processing
        latch.await(500, TimeUnit.MILLISECONDS);
        
        assertFalse(service.isProcessing());
        emptyMat.close();
    }
    
    @Test
    void testProcessFrameWithBlankImage() throws InterruptedException {
        service.start();
        
        Mat blankImage = new Mat(480, 640, opencv_core.CV_8UC3);
        blankImage.put(new Scalar(255.0, 255.0, 255.0, 0.0));
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RecognitionResult> resultRef = new AtomicReference<>();
        
        service.setResultCallback(result -> {
            resultRef.set(result);
            latch.countDown();
        });
        
        service.processFrame(blankImage);
        
        // Wait for processing
        latch.await(1000, TimeUnit.MILLISECONDS);
        
        // Wait a bit longer for processing to complete
        Thread.sleep(200);
        
        // Processing may or may not be complete depending on timing
        // Just verify no exceptions were thrown
        
        // Should have a result indicating no grid
        if (resultRef.get() != null) {
            assertEquals(RecognitionResult.RecognitionState.SCANNING, resultRef.get().state());
        }
        
        blankImage.close();
    }
    
    @Test
    void testResultCallback() throws InterruptedException {
        service.start();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RecognitionResult> resultRef = new AtomicReference<>();
        
        service.setResultCallback(result -> {
            resultRef.set(result);
            latch.countDown();
        });
        
        Mat testFrame = new Mat(480, 640, opencv_core.CV_8UC3);
        testFrame.put(new Scalar(200.0, 200.0, 200.0, 0.0));
        
        service.processFrame(testFrame);
        
        // Wait for callback
        boolean called = latch.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(called, "Callback should be called");
        
        assertNotNull(resultRef.get());
        assertNotNull(resultRef.get().message());
        
        testFrame.close();
    }
    
    @Test
    void testGetCurrentResult() {
        RecognitionResult initial = service.getCurrentResult();
        
        assertNotNull(initial);
        assertNotNull(initial.state());
        assertNotNull(initial.message());
    }
    
    @Test
    void testReset() throws InterruptedException {
        service.start();
        
        // Process a frame
        Mat testFrame = new Mat(480, 640, opencv_core.CV_8UC3);
        service.processFrame(testFrame);
        
        Thread.sleep(200); // Wait for processing
        
        service.reset();
        
        assertNull(service.getCurrentCorners());
        RecognitionResult result = service.getCurrentResult();
        assertEquals(RecognitionResult.RecognitionState.SCANNING, result.state());
        
        testFrame.close();
    }
    
    @Test
    void testCustomGridSize() {
        VisionRecognitionService service6x6 = new VisionRecognitionService(6, 100, 3);
        
        assertNotNull(service6x6);
        assertFalse(service6x6.isRunning());
        
        service6x6.start();
        assertTrue(service6x6.isRunning());
        service6x6.stop();
    }
    
    @Test
    void testInvalidGridSize() {
        assertThrows(IllegalArgumentException.class, 
            () -> new VisionRecognitionService(3, 100, 3));
        assertThrows(IllegalArgumentException.class, 
            () -> new VisionRecognitionService(17, 100, 3));
    }
    
    @Test
    void testDefaultConstructor() {
        VisionRecognitionService defaultService = new VisionRecognitionService();
        
        assertNotNull(defaultService);
        assertFalse(defaultService.isRunning());
        
        defaultService.start();
        assertTrue(defaultService.isRunning());
        defaultService.stop();
    }
}
