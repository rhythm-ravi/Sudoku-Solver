# convert.py - CORRECTED VERSION
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

import tensorflow as tf

print(f"TensorFlow version: {tf.__version__}")

# Load model
try:
    model = tf.keras.models.load_model('mnist_model.h5', compile=False)
    print("✅ Model loaded successfully")
except Exception as e:
    print(f"❌ Direct load failed: {e}")
    print("\nTrying with weights only...")
    
    # Recreate the EXACT architecture from the notebook
    model = tf.keras.Sequential([
        tf.keras.layers.Flatten(input_shape=(28, 28)),
        tf.keras.layers.Dense(512, activation=tf.nn.relu),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(512, activation=tf.nn.relu),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(10, activation=tf.nn.softmax)
    ])
    
    # Load weights
    model.load_weights('mnist_model.h5')
    print("✅ Weights loaded successfully")

model.summary()

# Test with random input
import numpy as np
test_input = np.random.randn(1, 28, 28).astype(np.float32)
output = model.predict(test_input, verbose=0)
print(f"\n✅ Model works! Output shape: {output.shape}")
print(f"Test prediction: {np.argmax(output)}")

# Export as SavedModel
model.export('mnist_saved_model')
print("✅ Exported as SavedModel")

# Convert to ONNX
import subprocess
result = subprocess.run([
    'python', '-m', 'tf2onnx.convert',
    '--saved-model', 'mnist_saved_model',
    '--output', 'digit_classifier.onnx',
    '--opset', '13'
], capture_output=True, text=True)

print(result.stdout)
if result.returncode == 0:
    print("✅ SUCCESS! digit_classifier.onnx created")
    
    # Verify ONNX model
    import onnx
    onnx_model = onnx.load('digit_classifier.onnx')
    print(f"\nONNX Model Info:")
    print(f"  Inputs: {[i.name for i in onnx_model.graph.input]}")
    print(f"  Outputs: {[o.name for o in onnx_model.graph.output]}")
    
    import os
    print(f"  File size: {os.path.getsize('digit_classifier.onnx') / 1024:.2f} KB")
else:
    print("❌ Conversion failed:")
    print(result.stderr)

# Cleanup
import shutil
shutil.rmtree('mnist_saved_model', ignore_errors=True)

print("\n✅ Done!")