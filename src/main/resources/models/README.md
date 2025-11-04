# Digit Classification Model

## Overview

This directory should contain the ONNX model for digit classification in Sudoku grids.

## Model Requirements

### Input Specification
- **Format**: 28×28 grayscale image
- **Data Type**: Float32
- **Value Range**: [0.0, 1.0] (normalized pixel values)
- **Shape**: [1, 1, 28, 28] (batch, channels, height, width)

### Output Specification
- **Format**: Probability distribution over 10 classes
- **Data Type**: Float32
- **Classes**: 0-9 (where 0 represents an empty cell)
- **Shape**: [1, 10] (batch, classes)

### Model Characteristics
- **Size**: Target < 1MB for efficient offline use
- **Format**: ONNX (.onnx)
- **Quantization**: Optional (INT8 quantization for smaller size)
- **Framework**: ONNX Runtime

## Training Dataset

The model should be trained on:

1. **MNIST Dataset**: Standard handwritten digits (0-9)
2. **Augmented Sudoku Data**: 
   - Printed digits from various Sudoku puzzles
   - Different fonts and styles
   - Various lighting conditions
   - Camera distortions and perspective effects
   - Partial occlusions

## Data Preprocessing

Before feeding images to the model:

1. Convert to grayscale
2. Resize to 28×28 pixels
3. Center the digit within the cell
4. Normalize pixel values to [0.0, 1.0] range
5. Apply any additional preprocessing used during training

## Model Integration

### File Location
Place the trained model file at:
```
src/main/resources/models/digit_classifier.onnx
```

### Loading in Code

The `DigitClassifier` class will load the model using:

```java
InputStream modelStream = getClass().getResourceAsStream("/models/digit_classifier.onnx");
byte[] modelBytes = modelStream.readAllBytes();

OrtEnvironment ortEnvironment = OrtEnvironment.getEnvironment();
OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
OrtSession ortSession = ortEnvironment.createSession(modelBytes, sessionOptions);
```

## Performance Requirements

- **Inference Time**: < 10ms per cell on typical hardware
- **Accuracy**: > 95% on test set
- **Memory Usage**: < 10MB when loaded

## Training Recommendations

1. **Architecture**: Lightweight CNN (e.g., MobileNet-based or custom small CNN)
2. **Data Augmentation**: Rotation, scaling, translation, noise
3. **Regularization**: Dropout, L2 regularization to prevent overfitting
4. **Validation**: Separate test set with real Sudoku images

## Converting TensorFlow Model to ONNX

If you have a TensorFlow or Keras model, convert it to ONNX format using `tf2onnx`:

### Installation
```bash
pip install tf2onnx onnx
```

### Convert from SavedModel format
```bash
python -m tf2onnx.convert \
    --saved-model /path/to/saved_model \
    --output digit_classifier.onnx \
    --opset 13
```

### Convert from Keras .h5 file
```bash
python -m tf2onnx.convert \
    --keras /path/to/model.h5 \
    --output digit_classifier.onnx \
    --opset 13
```

### Convert from frozen graph
```bash
python -m tf2onnx.convert \
    --input /path/to/frozen_graph.pb \
    --inputs input:0 \
    --outputs output:0 \
    --output digit_classifier.onnx \
    --opset 13
```

### Python Script for Conversion
```python
import tf2onnx
import tensorflow as tf

# Load your Keras model
model = tf.keras.models.load_model('path/to/model.h5')

# Convert to ONNX
model_proto, _ = tf2onnx.convert.from_keras(
    model,
    input_signature=[tf.TensorSpec(shape=[1, 28, 28, 1], dtype=tf.float32, name='input')],
    opset=13
)

# Save ONNX model
with open('digit_classifier.onnx', 'wb') as f:
    f.write(model_proto.SerializeToString())

print("Model converted successfully!")
```

### Verify the converted model
```python
import onnx
import onnxruntime as ort
import numpy as np

# Load and check the ONNX model
model = onnx.load('digit_classifier.onnx')
onnx.checker.check_model(model)

# Test inference
session = ort.InferenceSession('digit_classifier.onnx')
input_name = session.get_inputs()[0].name

# Create dummy input
dummy_input = np.random.randn(1, 1, 28, 28).astype(np.float32)
output = session.run(None, {input_name: dummy_input})
print(f"Output shape: {output[0].shape}")
print(f"Model is ready for use!")
```

## Alternative Approaches

Other options for digit recognition:

1. **DJL (Deep Java Library)**: Java-native deep learning framework
2. **Pre-trained Services**: Use cloud APIs for digit recognition (requires internet)

## Testing

Before deployment, test the model on:

1. Clean, well-lit Sudoku puzzles
2. Poor lighting conditions
3. Angled/skewed perspectives
4. Partially filled grids
5. Different Sudoku sources (newspapers, books, apps)

## Status

**Current Status**: ONNX Runtime integration complete. Place `digit_classifier.onnx` in this directory to enable recognition.

**Next Steps**:
1. Collect/prepare training dataset
2. Train lightweight CNN model
3. Convert to ONNX format using tf2onnx
4. Validate on test images
5. Place model file in src/main/resources/models/
6. Benchmark performance

## References

- [ONNX Runtime Documentation](https://onnxruntime.ai/docs/)
- [MNIST Dataset](http://yann.lecun.com/exdb/mnist/)
- [tf2onnx GitHub](https://github.com/onnx/tensorflow-onnx)
- [ONNX Runtime Java API](https://onnxruntime.ai/docs/get-started/with-java.html)
