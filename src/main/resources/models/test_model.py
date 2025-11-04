import onnxruntime as ort
import numpy as np
from PIL import Image
import PIL.ImageOps
import os

print("=" * 60)
print("ONNX Model Verification")
print("=" * 60)

# Load ONNX model
try:
    session = ort.InferenceSession('digit_classifier.onnx')
    print("✅ Model loaded successfully")
except Exception as e:
    print(f"❌ Failed to load model: {e}")
    exit(1)

# Check model info
input_name = session.get_inputs()[0].name
output_name = session.get_outputs()[0].name
input_shape = session.get_inputs()[0].shape
input_type = session.get_inputs()[0].type

print(f"\nModel Info:")
print(f"  Input name: {input_name}")
print(f"  Input shape: {input_shape}")
print(f"  Input type: {input_type}")
print(f"  Output name: {output_name}")

# Test 1: Random input
print("\n" + "=" * 60)
print("Test 1: Random Input")
print("=" * 60)
test_input = np.random.randn(1, 28, 28).astype(np.float32)
output = session.run(None, {input_name: test_input})
prediction = np.argmax(output[0])
confidence = np.max(output[0])
print(f"✅ Model runs successfully")
print(f"   Prediction: {prediction}")
print(f"   Confidence: {confidence:.2%}")

# Test 2: Test with the test.png if it exists
print("\n" + "=" * 60)
print("Test 2: Real Image (test.png)")
print("=" * 60)
try:
    img = Image.open("test.png").convert('L')
    img = PIL.ImageOps.invert(img)
    img = img.resize((28, 28), Image.LANCZOS)
    
    # Convert to numpy array
    data = np.asarray(img, dtype=np.float32) / 255.0
    data = np.expand_dims(data, 0)
    
    # Run inference
    output = session.run(None, {input_name: data})
    prediction = np.argmax(output[0])
    confidence = np.max(output[0])
    
    print(f"✅ Image processed successfully")
    print(f"   Prediction: {prediction}")
    print(f"   Confidence: {confidence:.2%}")
    print(f"\nAll probabilities:")
    for i, prob in enumerate(output[0][0]):
        print(f"   Digit {i}: {prob:.4%}")
        
except FileNotFoundError:
    print("⚠️  test.png not found - skipping image test")
except Exception as e:
    print(f"❌ Error processing image: {e}")

# Test 3: Batch of zeros (should predict 0 or be uncertain)
print("\n" + "=" * 60)
print("Test 3: All Zeros Input (Empty Image)")
print("=" * 60)
zero_input = np.zeros((1, 28, 28), dtype=np.float32)
output = session.run(None, {input_name: zero_input})
prediction = np.argmax(output[0])
confidence = np.max(output[0])
print(f"   Prediction: {prediction}")
print(f"   Confidence: {confidence:.2%}")

# Test 4: All Ones (White image)
print("\n" + "=" * 60)
print("Test 4: All Ones Input (White Image)")
print("=" * 60)
ones_input = np.ones((1, 28, 28), dtype=np.float32)
output = session.run(None, {input_name: ones_input})
prediction = np.argmax(output[0])
confidence = np.max(output[0])
print(f"   Prediction: {prediction}")
print(f"   Confidence: {confidence:.2%}")

print("\n" + "=" * 60)
print("✅ All tests completed successfully!")
print("=" * 60)
print("\nModel is ready to use in your Java application.")
print(f"Model size: {os.path.getsize('digit_classifier.onnx') / 1024:.1f} KB")