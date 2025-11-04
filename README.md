# Sudoku-Solver
JavaFX sudoku solver with vision recognition capabilities

## Features

- **Interactive Sudoku Solving**: Play and solve Sudoku puzzles interactively
- **Camera-based Grid Detection**: Use your camera to capture Sudoku puzzles
- **Computer Vision Processing**: Automatically detect and recognize Sudoku grids
- **Real-time Feedback**: Visual overlays showing grid detection status

## Vision Recognition Module

The application now includes a complete vision recognition pipeline for detecting and recognizing Sudoku puzzles from camera input.

### Architecture

#### Package Structure
- `com.dooku.utils` - Utility classes (e.g., OpenCVUtils for image conversion)
- `com.dooku.vision` - Computer vision services and processing
  - `VisionService` - Main service for grid detection and recognition
  - `Classifier` - Digit recognition (currently a dummy implementation)
- `com.dooku.vision.model` - Data models
  - `GridSearchResult` - Result object containing detection status and corners

#### Vision Pipeline

1. **Grid Detection**
   - Converts camera frames to grayscale
   - Applies Gaussian blur and adaptive thresholding
   - Finds contours and identifies the largest quadrilateral
   - Validates that the shape is convex

2. **Stability Tracking**
   - Monitors detected grid corners across frames
   - Requires grid to be stable for 5 consecutive frames
   - Provides three status levels: NO_GRID, UNSTABLE, STABLE

3. **Perspective Warping**
   - Transforms the detected quadrilateral to a perfect square (450x450 pixels)
   - Prepares the grid for cell segmentation

4. **Cell Segmentation**
   - Divides the warped grid into 81 cells (9x9)
   - Each cell is 50x50 pixels

5. **Digit Recognition**
   - Processes all 81 cells in parallel using Java parallel streams
   - Uses the Classifier to recognize digits (currently returns 0)
   - Ready for integration with a TensorFlow Lite model

### UI Features

The Scanner interface provides real-time visual feedback:

- **Green Rectangle**: Grid detected and stable - ready for recognition
- **Yellow Rectangle**: Grid detected but unstable - hold camera steady
- **No Overlay**: No grid detected - adjust camera position
- **Status Message**: Text updates based on detection state

### Usage

1. Navigate to the Scanner screen from the main menu
2. Point your camera at a Sudoku puzzle
3. Align the puzzle within the camera view
4. Hold steady until the overlay turns green
5. Press "Snap" to capture and recognize the puzzle

### Technical Details

- **Threading**: Uses a dedicated thread pool for processing to avoid blocking the UI
- **Asynchronous Processing**: All vision operations return CompletableFutures
- **Resource Management**: Properly closes OpenCV Mat objects to prevent memory leaks
- **JavaFX Integration**: Updates UI on the JavaFX Application Thread via Platform.runLater

### Testing

The module includes comprehensive unit tests:
- `GridSearchResultTest` - Tests the result model validation
- `ClassifierTest` - Tests the digit classifier
- `VisionServiceTest` - Tests grid detection, warping, and segmentation

Run tests with:
```bash
mvn test
```

### Future Enhancements

- Integrate a real TensorFlow Lite model for digit recognition
- Add training data collection mode
- Implement confidence thresholds for recognition
- Add support for different puzzle sizes
- Improve preprocessing for various lighting conditions

## Building and Running

### Requirements
- Java 17 or higher
- Maven 3.6+
- Camera/webcam (for vision features)

### Build
```bash
mvn clean compile
```

### Test
```bash
mvn test
```

### Run
```bash
mvn javafx:run
```

## Dependencies

- JavaFX 17.0.2 - UI framework
- OpenCV 4.11.0 (via Bytedeco) - Computer vision
- JUnit 5.10.0 - Testing framework
