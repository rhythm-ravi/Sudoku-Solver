# rebuild_and_convert.py
import tensorflow as tf
import h5py
import numpy as np
import tf2onnx

# Peek at the h5 structure
print("Inspecting h5 file...")
with h5py.File('mnist_model.h5', 'r') as f:
    def print_structure(name, obj):
        print(name)
    f.visititems(print_structure)

# Create a new model with typical MNIST architecture
model = tf.keras.Sequential([
    tf.keras.layers.InputLayer(input_shape=(28, 28, 1)),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(10, activation='softmax')
])

# Try to load weights
try:
    model.load_weights('mnist_model.h5')
    print("✅ Weights loaded successfully")
except Exception as e:
    print(f"⚠️ Weight loading failed: {e}")
    print("Creating model with random weights (will need retraining)")

model.summary()

# Convert
spec = (tf.TensorSpec((None, 28, 28, 1), tf.float32, name="input"),)
tf2onnx.convert.from_keras(
    model,
    input_signature=spec,
    opset=13,
    output_path='digit_classifier.onnx'
)

print("✅ digit_classifier.onnx created")