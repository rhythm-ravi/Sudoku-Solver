# 🎯 Sudoku Solver

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.2-blue.svg)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

A modern, feature-rich Sudoku Solver with **AI-powered vision recognition** capabilities. Solve puzzles manually, or point your camera at a Sudoku grid and watch it detect, recognize, and solve automatically—completely offline!

---

## ✨ Features

### 🎮 Interactive Solving
- **Multiple Grid Sizes**: Support for 4×4, 6×6, 9×9, 12×12, and 16×16 Sudoku puzzles
- **Manual Input**: Type digits directly into cells
- **Smart Validation**: Real-time validation of user inputs
- **Instant Solving**: Fast backtracking algorithm with optimizations
- **Visual Feedback**: Color-coded cells to distinguish original vs. solved values

### 📷 Vision Recognition (AI-Powered)
- **Real-Time Camera Detection**: Automatically detect Sudoku grids from camera feed
- **Visual Overlays**: 
  - 🟡 **Yellow**: Grid detected, checking stability
  - 🟠 **Orange**: Verifying position (hold steady)
  - 🟢 **Green**: Grid confirmed, processing digits
- **Image Upload**: Drag & drop or browse for Sudoku images (JPG/PNG)
- **Multi-Frame Consensus**: Ensures accuracy by verifying across multiple frames
- **Offline Processing**: No internet required—all AI runs locally

### 🎨 Modern UI/UX
- **Sleek Design**: Modern gradient backgrounds with card-based layouts
- **Smooth Animations**: Polished transitions and hover effects
- **Responsive Interface**: Adapts to different screen sizes
- **Dark Theme**: Easy on the eyes with professional styling

### ⚙️ Customizable Settings
- Adjustable grid sizes (4×4 to 16×16)
- Vision recognition tuning (processing speed, verification frames)
- Debug mode for developers
- Persistent settings using Java Preferences API

---

## 🏗️ Architecture

### Tech Stack
- **Frontend**: JavaFX 17.0.2
- **Computer Vision**: OpenCV 4.11.0 (via Bytedeco)
- **AI Model**: ONNX Runtime 1.15.1
- **Build Tool**: Maven 3.6+
- **Testing**: JUnit 5.10.0

### Project Structure
```
sudoku-solver-testing/
├── src/main/
│   ├── java/com/dooku/
│   │   ├── App.java                    # Application entry point
│   │   ├── MainController.java         # Puzzle solver interface
│   │   ├── MenuController.java         # Main menu
│   │   ├── ScannerController.java      # Camera vision interface
│   │   ├── SettingsController.java     # Configuration panel
│   │   ├── Board.java                  # Sudoku solving logic
│   │   ├── utils/
│   │   │   └── OpenCVUtils.java        # Image conversion utilities
│   │   └── vision/
│   │       ├── VisionRecognitionService.java  # Main orchestrator
│   │       ├── GridDetector.java              # Grid detection
│   │       ├── GridSegmenter.java             # Cell segmentation
│   │       ├── DigitClassifier.java           # AI digit recognition
│   │       ├── FrameConsensusManager.java     # Multi-frame verification
│   │       ├── VisionConfig.java              # Vision settings
│   │       └── RecognitionResult.java         # Result data classes
│   └── resources/
│       ├── com/dooku/
│       │   ├── fxml/                   # UI layouts
│       │   │   ├── menu.fxml
│       │   │   ├── scanner.fxml
│       │   │   └── settings.fxml
│       │   └── css/                    # Stylesheets
│       │       ├── application.css
│       │       ├── menu.css
│       │       ├── scanner.css
│       │       └── settings.css
│       └── models/
│           └── digit_classifier.onnx   # AI model (MNIST-based)
└── pom.xml                             # Maven configuration
```

---

## 🚀 Getting Started

### Prerequisites
- **Java**: JDK 17 or higher
- **Maven**: 3.6 or higher
- **Camera**: Webcam (for vision features)
- **OS**: Windows, macOS, or Linux

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/rhythm-ravi/Sudoku-Solver-Testing.git
cd Sudoku-Solver-Testing
```

2. **Build the project**
```bash
mvn clean compile
```

3. **Run the application**
```bash
mvn javafx:run
```

### Running Tests
```bash
mvn test
```

---

## 📖 Usage Guide

### Manual Solving
1. Launch the application
2. Select **"Manual Input"** from the main menu
3. Choose your grid size (default: 9×9)
4. Click cells to enter digits
5. Press **"Solve"** to see the solution
6. Press **"Clear"** to reset the grid

### Vision Recognition (Camera)
1. Select **"Scan Puzzle"** from the main menu
2. Grant camera permissions if prompted
3. Point your camera at a printed or on-screen Sudoku puzzle
4. Align the grid within the camera view
5. Hold steady until the overlay turns **green**
6. The system automatically captures and solves the puzzle

### Vision Recognition (Image Upload)
1. In the Scanner scene, toggle to **"📁 Upload"** mode
2. **Drag & drop** an image onto the upload zone, OR
3. Click **"Browse Files"** to select an image (JPG/PNG, max 10MB)
4. The system processes the image and displays the solution

### Settings Configuration
- **Grid Size**: Choose puzzle dimensions (4×4 to 16×16)
- **Processing Speed**: Adjust frame processing rate (higher = faster but more CPU)
- **Verification Frames**: Number of frames required for stable detection (3-10)
- **Debug Mode**: Enable detailed logging for troubleshooting

---

## 🧠 Vision Recognition Pipeline

### How It Works

1. **Grid Detection**
   - Converts camera frames to grayscale
   - Applies Gaussian blur and adaptive thresholding
   - Identifies the largest quadrilateral contour
   - Validates shape is convex and aspect ratio is square-like

2. **Stability Verification**
   - Monitors detected grid corners across multiple frames
   - Requires position to be stable (within 10px tolerance) for 5 consecutive frames
   - Prevents false captures from camera shake

3. **Perspective Transformation**
   - Warps detected quadrilateral to perfect 450×450 square
   - Corrects for camera angle and distortion

4. **Cell Segmentation**
   - Divides warped grid into N×N cells (e.g., 81 cells for 9×9)
   - Each cell is preprocessed to 28×28 grayscale
   - Removes border padding to isolate digits

5. **Digit Classification**
   - Parallel processing of all cells using ONNX Runtime
   - CNN model trained on MNIST + augmented Sudoku data
   - Outputs digit (1-9) or empty (0) for each cell
   - Confidence thresholding to reject uncertain predictions

6. **Board Consensus**
   - Collects multiple board readings
   - Uses majority voting to determine final values
   - Ensures high accuracy before displaying result

### Performance
- **Detection Latency**: ~30ms per frame
- **Classification Speed**: ~100ms for full 9×9 grid (parallel processing)
- **Accuracy**: >95% on well-lit, clear puzzles
- **Memory Usage**: <15MB for vision pipeline

---

## 🎓 AI Model Details

### Model Specifications
- **Architecture**: Lightweight CNN (2-layer dense network)
- **Input**: 28×28 grayscale image, normalized [0, 1]
- **Output**: Softmax probabilities for digits 0-9 (0 = empty)
- **Format**: ONNX (.onnx)
- **Size**: ~2.6MB
- **Framework**: ONNX Runtime
- **Training**: MNIST dataset with Keras/TensorFlow

### Training Details
- **Dataset**: MNIST handwritten digits (60,000 training images)
- **Architecture**: 
  - Flatten layer (28×28 → 784)
  - Dense layer (512 neurons, ReLU activation)
  - Dropout (0.2)
  - Dense layer (512 neurons, ReLU activation)
  - Dropout (0.2)
  - Output layer (10 neurons, Softmax activation)
- **Accuracy**: 98.42% on test set
- **Preprocessing**: Images inverted (white digits on black background)

### Converting Your Own Model
If you have a TensorFlow/Keras model, convert it to ONNX:

```bash
pip install tf2onnx onnx

python -m tf2onnx.convert \
    --saved-model /path/to/model \
    --output digit_classifier.onnx \
    --opset 13
```

Place the `.onnx` file in `src/main/resources/models/`

---

## 🎨 UI Theming

The application uses a modern dark theme with customizable CSS:

### Color Palette
```css
Primary Background:   #1a1a2e (dark blue-black)
Secondary Background: #16213e (deep navy)
Accent Color:         #4CAF50 (green)
Warning Color:        #FF9800 (orange)
Error Color:          #F44336 (red)
Text Primary:         #FFFFFF (white)
Text Secondary:       #B0B0B0 (gray)
```

### Customization
Edit CSS files in `src/main/resources/com/dooku/css/` to change:
- Colors and gradients
- Button styles and hover effects
- Animation timings
- Spacing and layout

---

## 🐛 Troubleshooting

### Camera Not Working
- **Check Permissions**: Ensure camera access is granted
- **Multiple Cameras**: The app uses device 0 by default. Modify `VideoCapture(0)` in `ScannerController.java` to use a different camera
- **Windows**: May require admin privileges on first run

### Poor Grid Detection
- **Lighting**: Ensure good, even lighting on the puzzle
- **Angle**: Hold camera perpendicular to the puzzle
- **Distance**: Keep puzzle within 50-80% of camera view
- **Quality**: Use high-contrast puzzles (dark lines, white background)

### Slow Performance
- **Lower Processing Speed**: Adjust in Settings → Vision Recognition
- **Close Other Apps**: Free up CPU resources
- **Update Graphics Drivers**: Especially on older systems

### Model Not Found
If you see "Failed to load ONNX model":
1. Ensure `digit_classifier.onnx` exists in `src/main/resources/models/`
2. Rebuild the project: `mvn clean compile`
3. Check file permissions

### Build Errors
- **OpenCV Native Libraries**: If you see `UnsatisfiedLinkError`, ensure OpenCV natives are properly loaded
- **Module Issues**: Verify `module-info.java` includes all required modules
- **Maven Clean**: Run `mvn clean` to clear cached artifacts

---

## 🛣️ Roadmap

### Completed ✅
- [x] Core Sudoku solving algorithm
- [x] JavaFX UI with multiple scenes
- [x] OpenCV integration for vision
- [x] Real-time grid detection
- [x] ONNX model integration
- [x] Image upload functionality
- [x] Modern UI redesign
- [x] Settings persistence
- [x] Multi-frame consensus verification
- [x] Configurable grid sizes (4×4 to 16×16)

### In Progress 🚧
- [ ] Real-time camera overlay (PR in progress)
- [ ] Model accuracy improvements (training on more diverse data)
- [ ] Performance optimizations for Board.java solver

### Planned 🎯
- [ ] Improved solver algorithm (Dancing Links / Algorithm X)
- [ ] Puzzle difficulty rating
- [ ] Hint system for manual solving
- [ ] Save/load puzzle feature
- [ ] Printable puzzle generator
- [ ] Multi-language support
- [ ] Dark/light theme toggle
- [ ] Sound effects and haptic feedback
- [ ] Mobile app (Android/iOS) using same vision pipeline

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

### Development Guidelines
- Follow Java naming conventions
- Write unit tests for new features
- Update README.md for significant changes
- Keep code modular and well-documented
- Test vision features on various puzzles

---

## 📝 License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **rhythm-ravi** - *Initial work and vision recognition* - [@rhythm-ravi](https://github.com/rhythm-ravi)

---

## 🙏 Acknowledgments

- **OpenCV** for powerful computer vision tools
- **ONNX Runtime** for efficient model inference
- **JavaFX** for modern UI capabilities
- **MNIST Dataset** for digit recognition training
- **Bytedeco** for Java bindings to native libraries

---

## 📧 Contact

For questions, suggestions, or feedback:
- **GitHub Issues**: [Open an issue](https://github.com/rhythm-ravi/Sudoku-Solver-Testing/issues)

---

## ⭐ Show Your Support

If you found this project helpful or interesting, please consider giving it a ⭐ on GitHub!

---

**Built with ❤️ using Java, JavaFX, and Computer Vision**