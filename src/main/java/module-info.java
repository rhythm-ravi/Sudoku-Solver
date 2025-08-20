module com.dooku {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires javafx.graphics;

    opens com.dooku to javafx.fxml;
    exports com.dooku;
}
