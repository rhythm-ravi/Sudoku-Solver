import tensorflow as tf
import subprocess
import os

# Recreate model
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
print("✅ Model loaded")

# Export
model.export('temp_model')

# Convert - using full path to python
python_path = r'C:\Users\rhyth\OneDrive\Documents\GitHub\Sudoku-Solver-Testing\src\main\resources\models\venv\Scripts\python.exe'
result = subprocess.run([
    python_path, '-m', 'tf2onnx.convert',
    '--saved-model', 'temp_model',
    '--output', 'digit_classifier.onnx',
    '--opset', '13'
], capture_output=True, text=True)

print(result.stdout)
if result.returncode == 0:
    print("✅ DONE! digit_classifier.onnx created")
    print(f"Size: {os.path.getsize('digit_classifier.onnx')/1024:.1f} KB")
else:
    print(f"Error: {result.stderr}")

# Cleanup
import shutil
shutil.rmtree('temp_model', ignore_errors=True)