package com.dooku;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_imgproc;

public class OpenCVUtils {

    // Converts BGR/GRAY/4-channel Mat to JavaFX Image
    public static Image matToImage(Mat mat) {
        if (mat == null || mat.empty()) return null;

        Mat converted = new Mat();
        int channels = mat.channels();
        switch (channels) {
            case 1 -> opencv_imgproc.cvtColor(mat, converted, opencv_imgproc.COLOR_GRAY2BGRA);
            case 3 -> opencv_imgproc.cvtColor(mat, converted, opencv_imgproc.COLOR_BGR2BGRA);
            case 4 -> converted = mat;
            default -> converted = mat;
        }

        int width = converted.cols();
        int height = converted.rows();
        int bufferSize = (int) (converted.channels() * width * height);
        byte[] buffer = new byte[bufferSize];
        converted.data().get(buffer);

        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        pw.setPixels(0, 0, width, height,
                PixelFormat.getByteBgraPreInstance(),
                buffer, 0, converted.channels() * width);
        return image;
    }
}