module com.dooku {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.prefs;
    
    requires org.bytedeco.opencv;

    opens com.dooku to javafx.fxml;
    exports com.dooku;
}