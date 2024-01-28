module com.dooku {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;

    opens com.dooku to javafx.fxml;
    exports com.dooku;
}
