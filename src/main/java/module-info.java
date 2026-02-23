module org.example.qq_farm {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires javafx.graphics;

    exports org.example.qq_farm.common;
    exports org.example.qq_farm.server;
    exports org.example.qq_farm.client;

    opens org.example.qq_farm.client to javafx.fxml;
    opens org.example.qq_farm.common to javafx.fxml;
}