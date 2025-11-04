# Architecture Overview

## Project Structure

```
src/main/java/com/dooku/
├── App.java                          # JavaFX Application entry point
├── Board.java                        # Sudoku board logic
├── MainController.java               # Main screen controller
├── MainScene.java                    # Main scene UI
├── MenuController.java               # Menu screen controller
├── ScannerController.java            # Camera/scanner screen controller (UPDATED)
├── SettingsController.java           # Settings screen controller
├── utils/
│   └── OpenCVUtils.java             # OpenCV/JavaFX utility functions (MOVED)
└── vision/                           # NEW: Vision processing module
    ├── Classifier.java              # Digit recognition (dummy implementation)
    ├── VisionService.java           # Main vision processing service
    └── model/
        └── GridSearchResult.java    # Grid detection result model

src/test/java/com/dooku/vision/
├── ClassifierTest.java              # Unit tests for Classifier
├── VisionServiceTest.java           # Unit tests for VisionService
└── model/
    └── GridSearchResultTest.java   # Unit tests for GridSearchResult
```

## Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        JavaFX UI Layer                          │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              ScannerController                          │    │
│  │  - Camera Management                                    │    │
│  │  - UI Updates (overlays, status messages)              │    │
│  │  - Async coordination with CompletableFuture           │    │
│  └───────────────────┬────────────────────────────────────┘    │
└────────────────────────┼──────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Vision Processing Layer                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              VisionService                              │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │  ExecutorService (Thread Pool)                   │  │    │
│  │  │  - Parallel processing                           │  │    │
│  │  │  - Resource management                           │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  │                                                          │    │
│  │  Methods:                                                │    │
│  │  - findGrid(Mat) → CompletableFuture<GridSearchResult> │    │
│  │  - warpGrid(Mat, Point[]) → Mat                        │    │
│  │  - segmentGrid(Mat) → Mat[]                            │    │
│  │  - recognizeBoard(Mat) → CompletableFuture<int[][]>    │    │
│  └────────────────────┬───────────────────────────────────┘    │
└────────────────────────┼──────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Computer Vision Pipeline                     │
│                                                                  │
│  1. Preprocessing                                               │
│     ├─ Grayscale conversion                                     │
│     ├─ Gaussian blur                                            │
│     └─ Adaptive thresholding                                    │
│                                                                  │
│  2. Grid Detection                                              │
│     ├─ Contour detection                                        │
│     ├─ Find largest quadrilateral                               │
│     └─ Validate convexity                                       │
│                                                                  │
│  3. Stability Tracking                                          │
│     ├─ Compare with previous corners                            │
│     ├─ Count stable frames                                      │
│     └─ Return status (NO_GRID/UNSTABLE/STABLE)                  │
│                                                                  │
│  4. Perspective Warping                                         │
│     ├─ Calculate transform matrix                               │
│     └─ Warp to 450x450 square                                   │
│                                                                  │
│  5. Cell Segmentation                                           │
│     └─ Divide into 9x9 grid (81 cells of 50x50)                │
│                                                                  │
│  6. Digit Recognition (Parallel)                                │
│     ├─ Process 81 cells using parallel streams                  │
│     └─ Classifier.classify(Mat) → int                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

### Real-time Grid Detection Flow

```
Camera Frame
    │
    ├─── (Display to UI) ──────────────────────────────────┐
    │                                                       │
    └─── (Process if not busy) ─────┐                      │
                                     │                      │
                        ┌────────────▼──────────────┐      │
                        │    VisionService          │      │
                        │    findGrid()             │      │
                        └────────────┬──────────────┘      │
                                     │                      │
                              GridSearchResult              │
                                     │                      │
                        ┌────────────▼──────────────┐      │
                        │    ScannerController      │      │
                        │    updateUI()             │◄─────┘
                        └────────────┬──────────────┘
                                     │
                        ┌────────────▼──────────────┐
                        │    UI Elements            │
                        │  - Status Message         │
                        │  - Colored Overlay        │
                        │    (green/yellow/none)    │
                        └───────────────────────────┘
```

### Snapshot Recognition Flow

```
User Presses "Snap"
    │
    ├─── Capture Frame
    │
    └─── VisionService.findGrid(frame)
            │
            ├─── (NO_GRID) ──────────► Error Message
            │
            └─── (UNSTABLE/STABLE) ───┐
                                       │
                    VisionService.warpGrid(frame, corners)
                                       │
                                       ├─── Warped Grid (450x450)
                                       │
                    VisionService.recognizeBoard(warped)
                                       │
                                       ├─── Parallel Processing
                                       │    (81 cells)
                                       │
                                       ├─── Classifier.classify()
                                       │    (for each cell)
                                       │
                                       └─── int[][] board (9x9)
                                              │
                                              └─► Display/Process Result
```

## Threading Model

```
┌─────────────────────────────────────────────────────────────────┐
│                          Main Thread                            │
│                      (JavaFX Application)                       │
│                                                                  │
│  - UI rendering                                                 │
│  - Event handling                                               │
│  - Camera display                                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                               │
                               │ CompletableFuture
                               │ Platform.runLater()
                               │
┌─────────────────────────────▼──────────────────────────────────┐
│                    Vision Processing Pool                       │
│               (Runtime.availableProcessors() threads)           │
│                                                                  │
│  - Grid detection                                               │
│  - Image preprocessing                                          │
│  - Perspective warping                                          │
│  - Cell segmentation                                            │
│                                                                  │
│  ┌──────────────────────────────────────────────────────┐      │
│  │         Parallel Stream Processing                    │      │
│  │  (Digit recognition for 81 cells)                    │      │
│  │                                                       │      │
│  │  Cell 1  Cell 2  Cell 3  ...  Cell 81               │      │
│  │    │       │       │            │                    │      │
│  │    └───────┴───────┴────────────┘                    │      │
│  │              │                                        │      │
│  │        Classifier.classify()                         │      │
│  └──────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
                               │
                               │ Frame Grabber Thread
                               │
┌─────────────────────────────▼──────────────────────────────────┐
│                 ScheduledExecutorService                        │
│                   (Single Thread, 30 FPS)                       │
│                                                                  │
│  - Capture frames from camera                                   │
│  - Schedule grabAndShowFrame() every 33ms                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Key Design Patterns

### 1. Service Layer Pattern
- **VisionService** encapsulates all vision processing logic
- Provides clean interface for controllers
- Manages its own resources (thread pool)

### 2. Asynchronous Pattern
- **CompletableFuture** for non-blocking operations
- Prevents UI freezing during processing
- Easy composition of pipeline stages

### 3. Record Pattern (Java 14+)
- **GridSearchResult** as immutable data container
- Built-in validation in compact constructor
- Clear, type-safe communication

### 4. Strategy Pattern (Future)
- **Classifier** interface ready for different implementations
- Easy to swap dummy implementation with TFLite model
- Testable and mockable

### 5. Resource Management Pattern
- Explicit **close()** calls on Mat objects
- Try-finally blocks for cleanup
- Prevents memory leaks with native resources

## Integration Points

### Current Integration
- **OpenCV**: Grid detection, image processing
- **JavaFX**: UI rendering, event handling
- **Java Concurrency**: Thread pools, parallel streams

### Future Integration (Ready)
- **TensorFlow Lite**: Replace Classifier dummy implementation
- **Model Loading**: Add .tflite model loading in Classifier
- **Preprocessing**: Add normalization in Classifier
- **Confidence Scores**: Return confidence with predictions

## Performance Characteristics

### Grid Detection
- **Frequency**: Every ~3-4 frames (when not processing)
- **Latency**: ~50-100ms per frame
- **CPU**: 1 core during detection

### Cell Recognition
- **Parallel Factor**: Number of CPU cores
- **Per-Cell Time**: ~1ms (dummy), ~5-10ms (expected with model)
- **Total Time**: ~10-15ms (parallel) vs ~81-810ms (sequential)

### Memory
- **Frame Buffer**: ~1-2 MB per frame
- **Warped Grid**: ~200 KB
- **Cell Buffers**: ~12 KB (81 cells × 150 bytes)
- **Thread Overhead**: ~1 MB per thread

## Error Handling Strategy

1. **Null Checks**: All public methods validate inputs
2. **Empty Mat**: Handled gracefully, return empty/null results
3. **Thread Interruption**: Caught and logged
4. **Resource Cleanup**: Finally blocks ensure Mat.close()
5. **CompletableFuture.exceptionally()**: Catches async errors
6. **Platform.runLater**: Safe UI updates from any thread
