module com.rms.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;

    opens com.rms.desktop to javafx.fxml;
    exports com.rms.desktop;
}
