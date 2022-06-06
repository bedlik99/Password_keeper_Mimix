module PASSWORDS.KEEPER {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.jfoenix;
    requires jbcrypt;
    requires json.simple;
    requires org.controlsfx.controls;

    opens Main;
    opens Controller;
    opens DataModel;
    opens Repository;
    opens Service;
}