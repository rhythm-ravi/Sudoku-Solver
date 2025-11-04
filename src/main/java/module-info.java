module com.dooku {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.prefs;
    
    requires org.bytedeco.opencv;
    requires org.slf4j;
    requires com.microsoft.onnxruntime;

    opens com.dooku to javafx.fxml;
    exports com.dooku;
    exports com.dooku.utils;
    exports com.dooku.vision;
    exports com.dooku.vision.model;
}