# Implementation Notes - Vision Recognition Module

## Overview
This document describes the implementation of the Sudoku board vision recognition feature.

## Changes Made

### 1. Package Restructuring

#### Created New Packages
- `com.dooku.utils` - For utility classes
- `com.dooku.vision` - For vision processing logic
- `com.dooku.vision.model` - For data models

#### Moved Files
- `OpenCVUtils.java` moved from `com.dooku` to `com.dooku.utils`

### 2. New Classes

#### VisionService (`com.dooku.vision.VisionService`)
Main service responsible for all computer vision operations.

**Key Features:**
- Thread pool management using `ExecutorService`
- Grid detection using contour analysis
- Stability tracking across frames
- Perspective warping to normalize grid
- Cell segmentation (9x9 grid = 81 cells)
- Board recognition using parallel processing

**Methods:**
- `findGrid(Mat frame)` - Detects Sudoku grid in a frame
- `warpGrid(Mat frame, Point[] corners)` - Applies perspective transformation
- `segmentGrid(Mat warpedGrid)` - Splits grid into 81 cells
- `recognizeBoard(Mat warpedGrid)` - Recognizes all digits in parallel
- `shutdown()` - Cleans up thread pool

**Constants:**
- `MIN_AREA = 10000` - Minimum contour area to be considered a grid
- `APPROX_EPSILON = 0.02` - Epsilon for polygon approximation
- `STABILITY_THRESHOLD = 10` - Max pixel movement for stability
- `REQUIRED_STABLE_FRAMES = 5` - Frames needed to be considered stable

#### Classifier (`com.dooku.vision.Classifier`)
Dummy implementation for digit recognition.

**Current Behavior:**
- Simulates 1ms processing delay
- Always returns 0 (empty cell)
- Ready to be replaced with TensorFlow Lite model

**Future Integration:**
Replace with real model that:
- Preprocesses cell images (resize, normalize)
- Runs inference using TFLite model
- Returns digit 0-9 (0 = empty)

#### GridSearchResult (`com.dooku.vision.model.GridSearchResult`)
Record class to communicate grid detection results.

**Fields:**
- `status` - Enum: NO_GRID, UNSTABLE, STABLE
- `corners` - Array of 4 Points representing grid corners

**Validation:**
- NO_GRID must have null corners
- UNSTABLE/STABLE must have 4 corners
- Enforced in compact constructor

### 3. Updated Classes

#### ScannerController (`com.dooku.ScannerController`)
Refactored to use VisionService and provide UI feedback.

**New Features:**
- Instantiates VisionService in initialize()
- Processes frames asynchronously using CompletableFuture
- Updates UI based on GridSearchResult status
- Draws colored overlays on detected grids
- Implements full recognition pipeline in takeSnapshot()

**Key Changes:**
- Added `overlayPane` FXML field
- Added `visionService` and `isProcessing` fields
- New `processFrame()` method for async processing
- New `updateUI()` method to update status and overlay
- New `drawOverlay()` method to draw grid outline
- Updated `takeSnapshot()` to perform full recognition
- Updated `stopCamera()` to shutdown VisionService

**Threading Model:**
- UI thread: Updates UI components
- Frame grabber thread: Captures frames at 30 FPS
- Vision processing thread pool: Processes frames asynchronously
- Platform.runLater: Ensures UI updates on JavaFX thread

### 4. Module Configuration

Updated `module-info.java` to export new packages:
```java
exports com.dooku.utils;
exports com.dooku.vision;
exports com.dooku.vision.model;
```

### 5. Build Configuration

Updated `pom.xml`:
- Changed Java version from 21 to 17 (for compatibility)
- Changed JavaFX version from 23.0.1 to 17.0.2
- Added JUnit 5.10.0 dependency for testing

### 6. Tests

Created comprehensive test suite:

#### GridSearchResultTest
- Tests validation logic for all status/corners combinations
- Ensures IllegalArgumentException for invalid states

#### ClassifierTest
- Tests dummy classifier behavior
- Tests null and empty Mat handling

#### VisionServiceTest
- Tests grid detection with null/empty frames
- Tests cell segmentation (81 cells, 50x50 each)
- Tests perspective warping
- Tests board recognition (9x9 array)

**Test Results:**
- 16 tests total
- All passing
- Coverage of main functionality

## Design Decisions

### 1. Asynchronous Processing
**Decision:** Use CompletableFuture for all vision operations

**Rationale:**
- Prevents UI from freezing during processing
- Allows camera to continue streaming
- Enables easy composition of pipeline stages
- Natural error handling with exceptionally()

### 2. Parallel Cell Processing
**Decision:** Use Java parallel streams to process 81 cells

**Rationale:**
- Takes advantage of multi-core processors
- Significantly reduces total processing time
- Simple implementation with parallelStream()
- Thread pool managed by VisionService

### 3. Stability Tracking
**Decision:** Require 5 stable frames before marking as STABLE

**Rationale:**
- Prevents false positives from momentary matches
- Ensures user has positioned camera correctly
- Provides smooth transition between states
- Configurable via REQUIRED_STABLE_FRAMES constant

### 4. Dummy Classifier
**Decision:** Implement simple dummy classifier initially

**Rationale:**
- Allows full pipeline testing without ML model
- Makes it easy to test integration
- Clear interface for future TFLite integration
- Demonstrates parallel processing works correctly

### 5. Resource Management
**Decision:** Explicitly close all Mat objects

**Rationale:**
- OpenCV Mat objects use native memory
- Java garbage collector won't clean them up
- Prevents memory leaks during long sessions
- Uses try-finally and .close() calls

### 6. UI Feedback
**Decision:** Use color-coded overlays (green/yellow) and status messages

**Rationale:**
- Clear visual feedback for user
- No text overlay on camera view
- Color meanings are intuitive
- Status message provides additional context

## Known Limitations

1. **Camera Resolution**: Assumes 640x480 for overlay scaling
   - Could be improved by querying actual camera resolution

2. **Classifier**: Returns only 0
   - Needs TFLite model integration for real recognition

3. **Grid Detection**: Basic contour-based approach
   - Could be improved with ML-based detection
   - May struggle with poor lighting or reflections

4. **No Auto-Capture**: User must press Snap button
   - Could auto-capture when STABLE for N seconds

5. **Single Grid Support**: Only processes one grid at a time
   - Could be extended to support multiple grids

## Performance Considerations

- Thread pool size: Uses available processor count
- Frame processing: Only processes when not already processing
- Memory: Mat objects properly closed to prevent leaks
- UI updates: Batched on JavaFX Application Thread

## Future Work

1. **ML Integration**
   - Add TensorFlow Lite dependency
   - Train/obtain digit recognition model
   - Update Classifier to use model
   - Add confidence thresholds

2. **Enhanced Detection**
   - ML-based grid detection
   - Support for rotated grids
   - Better handling of partial grids
   - Shadow and reflection removal

3. **User Experience**
   - Auto-capture when stable
   - Progress indicators during recognition
   - Preview of recognized digits overlaid on cells
   - Edit mode to correct misrecognitions

4. **Performance**
   - Optimize preprocessing pipeline
   - Cache processed frames
   - Reduce memory allocations
   - Profile and optimize hot paths

5. **Configuration**
   - Adjustable stability thresholds
   - Camera selection for multiple cameras
   - Resolution settings
   - Processing quality vs. speed tradeoff
